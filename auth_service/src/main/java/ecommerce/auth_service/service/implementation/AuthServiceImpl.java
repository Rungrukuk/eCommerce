package ecommerce.auth_service.service.implementation;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import ecommerce.auth_service.ProtoAuthRequest;
import ecommerce.auth_service.dto.AuthResponse;
import ecommerce.auth_service.repository.SessionRepository;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.service.AuthService;
import ecommerce.auth_service.service.RefreshTokenService;
import ecommerce.auth_service.service.RoleService;
import ecommerce.auth_service.util.CustomResponseStatus;
import ecommerce.auth_service.util.Roles;
import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class AuthServiceImpl implements AuthService {

        // Dependency injections with brief descriptions
        @Autowired
        private SessionRepository sessionRepository; // Handles session persistence
        @Autowired
        private JwtTokenProvider jwtTokenProvider; // JWT token operations
        @Autowired
        private GuestUserServiceImpl guestUserService; // Guest user management
        @Autowired
        private TransactionalOperator transactionalOperator; // Transaction control
        @Autowired
        private RoleService roleService; // Role-based access control
        @Autowired
        private BCryptPasswordEncoder passwordEncoder; // Token encryption
        @Autowired
        private RefreshTokenService refreshTokenService; // Refresh token management

        /**
         * Main authentication/authorization entry point.
         * 
         * @param authRequest Contains tokens, session ID, and target service
         *                    information
         * @return Mono<AuthResponse> Authentication response with tokens and status
         */
        @Override
        public Mono<AuthResponse> validate(ProtoAuthRequest authRequest) {
                return Mono.defer(() -> {
                        // Extract request parameters
                        String accessToken = authRequest.getAccessToken();
                        String refreshToken = authRequest.getRefreshToken();
                        String sessionId = authRequest.getSessionId();
                        List<String> services = authRequest.getServicesList();
                        List<String> destinations = authRequest.getDestinationsList();
                        String userAgent = authRequest.getUserAgent();
                        String clientCity = authRequest.getClientCity();

                        // Handle completely unauthenticated requests
                        if ((accessToken.isBlank() || sessionId.isBlank()) && refreshToken.isBlank()) {
                                return unauthenticatedAccessResponse(services, destinations);
                        }

                        // Validate existing credentials and handle accordingly
                        return validateAccessTokenAndSession(accessToken, sessionId)
                                        .flatMap(isValid -> isValid
                                                        ? handleValidAccessToken(accessToken, sessionId, services,
                                                                        destinations)
                                                        : handleInvalidAccessToken(refreshToken, services, destinations,
                                                                        userAgent, clientCity));
                })
                                .as(transactionalOperator::transactional)
                                .onErrorResume(e -> createUnexpectedErrorResponse());
        }

        /**
         * Validates access token and session ID combination.
         * 
         * @param accessToken User's access token
         * @param sessionId   Current session identifier
         * @return Mono<Boolean> True if both token and session are valid
         */
        private Mono<Boolean> validateAccessTokenAndSession(String accessToken, String sessionId) {
                // Handle missing token/session scenarios
                if (accessToken.isBlank() && sessionId.isBlank()) {
                        return Mono.just(false);
                }

                // Cleanup invalid session/token combinations
                if (accessToken.isBlank()) {
                        return handleBackgroundErrors(List.of(sessionRepository.deleteBySessionId(sessionId)))
                                        .thenReturn(false);
                }

                if (sessionId.isBlank()) {
                        return handleBackgroundErrors(List.of(sessionRepository.deleteByAccessToken(accessToken)))
                                        .thenReturn(false);
                }

                // Full validation flow
                return Mono.just(accessToken)
                                .filter(jwtTokenProvider::validateAccessToken)
                                .flatMap(validToken -> sessionRepository.validateSession(accessToken, sessionId)
                                                .flatMap(isValidSession -> {
                                                        if (!isValidSession) {
                                                                // Cleanup invalid session-token pair
                                                                return handleBackgroundErrors(List.of(
                                                                                sessionRepository.deleteByAccessToken(
                                                                                                accessToken),
                                                                                sessionRepository.deleteBySessionId(
                                                                                                sessionId)))
                                                                                .thenReturn(false);
                                                        }
                                                        return Mono.just(true);
                                                }))
                                .switchIfEmpty(
                                                handleBackgroundErrors(
                                                                List.of(sessionRepository.deleteBySessionId(sessionId)))
                                                                .thenReturn(false));
        }

        /**
         * Handles background cleanup operations for invalid tokens/sessions.
         * 
         * @param operations List of cleanup operations to perform
         * @return Mono<Void> Completion signal
         */
        private Mono<Void> handleBackgroundErrors(List<Mono<Boolean>> operations) {
                return Mono.when(operations)
                                .doOnError(e -> System.err.println(
                                                "Error during background cleanup: " + e.getMessage()))
                                .then();
        }

        /**
         * Processes requests with valid access tokens.
         * 
         * @param accessToken  Valid access token
         * @param sessionId    Valid session ID
         * @param services     Requested services
         * @param destinations Target endpoints
         * @return Mono<AuthResponse> Authorization response
         */
        private Mono<AuthResponse> handleValidAccessToken(String accessToken, String sessionId,
                        List<String> services, List<String> destinations) {
                Claims claims = jwtTokenProvider.getAccessTokenClaims(accessToken);
                String roleName = claims.get("role", String.class);

                return roleService.hasAccess(roleName, services, destinations).flatMap(hasAccess -> {
                        if (hasAccess) {
                                // Generate service-specific token for downstream services
                                String serviceToken = jwtTokenProvider.createServiceToken(
                                                claims.getSubject(),
                                                roleName,
                                                services,
                                                destinations);

                                return Mono.just(createAuthResponse(
                                                null, // Access token remains same
                                                null, // Session ID remains same
                                                serviceToken,
                                                null,
                                                roleName.equals(Roles.USER.name())
                                                                ? CustomResponseStatus.AUTHORIZED_USER
                                                                : CustomResponseStatus.AUTHORIZED_GUEST_USER,
                                                200));
                        }
                        return Mono.just(unauthorizedAccessResponse(
                                        null, null, null,
                                        roleName.equals(Roles.USER.name())
                                                        ? CustomResponseStatus.UNAUTHORIZED_USER
                                                        : CustomResponseStatus.UNAUTHORIZED_GUEST_USER));
                });
        }

        /**
         * Handles invalid access token scenario (refresh token flow).
         * 
         * @param refreshToken User's refresh token
         * @param services     Requested services
         * @param destinations Target endpoints
         * @param userAgent    Client device information
         * @param clientCity   Geographic context
         * @return Mono<AuthResponse> Renewed tokens or guest access
         */
        private Mono<AuthResponse> handleInvalidAccessToken(String refreshToken, List<String> services,
                        List<String> destinations, String userAgent, String clientCity) {
                return refreshTokenService.validateRefreshToken(refreshToken, userAgent, clientCity)
                                .flatMap(isValid -> {
                                        if (!isValid) {
                                                // Invalid refresh token - fallback to guest access
                                                return Mono.fromCallable(() -> passwordEncoder.encode(refreshToken))
                                                                .subscribeOn(Schedulers.boundedElastic())
                                                                .flatMap(encodedToken -> handleBackgroundErrors(List.of(
                                                                                refreshTokenService
                                                                                                .deleteByRefreshToken(
                                                                                                                encodedToken))))
                                                                .then(unauthenticatedAccessResponse(services,
                                                                                destinations));
                                        }

                                        // Refresh token is valid - issue new tokens
                                        Claims claims = jwtTokenProvider.getRefreshTokenClaims(refreshToken);
                                        String roleName = claims.get("role", String.class);

                                        return roleService.hasAccess(roleName, services, destinations)
                                                        .flatMap(hasAccess -> {
                                                                String userId = claims.getSubject();
                                                                String newRefreshToken = jwtTokenProvider
                                                                                .createRefreshToken(userId, roleName);

                                                                return Mono.fromCallable(() -> passwordEncoder
                                                                                .encode(newRefreshToken))
                                                                                .subscribeOn(Schedulers
                                                                                                .boundedElastic())
                                                                                .flatMap(encodedRefreshToken -> {
                                                                                        String newAccessToken = jwtTokenProvider
                                                                                                        .createAccessToken(
                                                                                                                        userId,
                                                                                                                        roleName);

                                                                                        return refreshTokenService
                                                                                                        .updateRefreshToken(
                                                                                                                        userId,
                                                                                                                        encodedRefreshToken,
                                                                                                                        userAgent,
                                                                                                                        clientCity)
                                                                                                        .flatMap(savedRefreshTokenEntity -> sessionRepository
                                                                                                                        .saveSession(newAccessToken)
                                                                                                                        .map(savedSession -> {
                                                                                                                                if (hasAccess) {
                                                                                                                                        String serviceToken = jwtTokenProvider
                                                                                                                                                        .createServiceToken(
                                                                                                                                                                        userId,
                                                                                                                                                                        roleName,
                                                                                                                                                                        services,
                                                                                                                                                                        destinations);
                                                                                                                                        return createAuthResponse(
                                                                                                                                                        newAccessToken,
                                                                                                                                                        savedSession.getSessionId(),
                                                                                                                                                        serviceToken,
                                                                                                                                                        newRefreshToken,
                                                                                                                                                        CustomResponseStatus.AUTHORIZED_USER,
                                                                                                                                                        200);
                                                                                                                                }
                                                                                                                                return unauthorizedAccessResponse(
                                                                                                                                                newAccessToken,
                                                                                                                                                savedSession.getSessionId(),
                                                                                                                                                newRefreshToken,
                                                                                                                                                CustomResponseStatus.UNAUTHORIZED_USER);
                                                                                                                        }))
                                                                                                        .doOnError(e -> {
                                                                                                                // Cleanup
                                                                                                                // failed
                                                                                                                // session
                                                                                                                sessionRepository
                                                                                                                                .deleteByAccessToken(
                                                                                                                                                newAccessToken)
                                                                                                                                .subscribe();
                                                                                                        });
                                                                                });
                                                        });
                                });
        }

        /**
         * Handles guest user creation and permission check.
         * 
         * @param services     Requested services
         * @param destinations Target endpoints
         * @return Mono<AuthResponse> Guest access response
         */
        private Mono<AuthResponse> unauthenticatedAccessResponse(List<String> services, List<String> destinations) {
                return guestUserService.createGuestUser().flatMap(guestUserResponse -> {
                        String guestAccessToken = guestUserResponse.getAccessToken();
                        String sessionId = guestUserResponse.getSessionId();

                        Claims claims = jwtTokenProvider.getAccessTokenClaims(guestAccessToken);
                        String roleName = claims.get("role", String.class);

                        return roleService.hasAccess(roleName, services, destinations)
                                        .flatMap(hasAccess -> {
                                                if (hasAccess) {
                                                        // Guest has required permissions
                                                        String serviceToken = jwtTokenProvider.createServiceToken(
                                                                        claims.getSubject(),
                                                                        roleName,
                                                                        services,
                                                                        destinations);
                                                        return Mono.just(createAuthResponse(
                                                                        guestAccessToken,
                                                                        sessionId,
                                                                        serviceToken,
                                                                        null,
                                                                        CustomResponseStatus.AUTHORIZED_GUEST_USER,
                                                                        200));
                                                } else {
                                                        // Guest lacks required permissions
                                                        return Mono.just(createAuthResponse(
                                                                        guestAccessToken,
                                                                        sessionId,
                                                                        null,
                                                                        null,
                                                                        CustomResponseStatus.UNAUTHORIZED_GUEST_USER,
                                                                        403));
                                                }
                                        });
                });
        }

        /**
         * Creates standardized unauthorized response.
         */
        private AuthResponse unauthorizedAccessResponse(String accessToken, String sessionId, String refreshToken,
                        CustomResponseStatus responseStatus) {
                return createAuthResponse(accessToken, sessionId, null, refreshToken, responseStatus, 403);
        }

        /**
         * Standardized response builder.
         * 
         * @param accessToken  Session access token (null for existing valid sessions)
         * @param sessionId    Session identifier (null for existing valid sessions)
         * @param serviceToken Downstream service access token
         * @param refreshToken New refresh token (if rotated)
         * @param status       Response status code
         * @param statusCode   HTTP status code
         */
        private AuthResponse createAuthResponse(String accessToken, String sessionId, String serviceToken,
                        String refreshToken, CustomResponseStatus status, int statusCode) {
                AuthResponse response = new AuthResponse();
                response.setAccessToken(accessToken != null ? accessToken : "");
                response.setSessionId(sessionId != null ? sessionId : "");
                response.setServiceToken(serviceToken != null ? serviceToken : "");
                response.setRefreshToken(refreshToken != null ? refreshToken : "");
                response.setResponseStatus(status != null ? status : CustomResponseStatus.UNEXPECTED_ERROR);
                response.setStatusCode(statusCode != 0 ? statusCode : 500);
                return response;
        }

        /**
         * Handles unexpected system errors.
         */
        private Mono<AuthResponse> createUnexpectedErrorResponse() {
                AuthResponse response = new AuthResponse();
                response.setResponseStatus(CustomResponseStatus.UNEXPECTED_ERROR);
                response.setStatusCode(500);
                return Mono.just(response);
        }
}