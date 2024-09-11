package ecommerce.auth_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ecommerce.auth_service.dto.GuestUserResponse;
import ecommerce.auth_service.repository.GuestUserRepository;
import ecommerce.auth_service.repository.SessionRepository;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.util.CustomResponseStatus;
import ecommerce.auth_service.util.Roles;
import reactor.core.publisher.Mono;

@Service
public class GuestUserServiceImpl {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private GuestUserRepository guestUserRepository;

    @Autowired
    private SessionRepository sessionRepository;

    // TODO Research Reactive Redis transactional operations
    public Mono<GuestUserResponse> createGuestUser() {
        GuestUserResponse userResponse = new GuestUserResponse();
        return guestUserRepository.saveGuestUser(Roles.GUEST_USER.name())
                .flatMap(guestUserDTO -> {
                    String accessToken = tokenProvider.createAccessToken(guestUserDTO.getUserId(),
                            Roles.GUEST_USER.name());
                    return sessionRepository.saveSession(accessToken)
                            .flatMap(savedSession -> {
                                userResponse.setAccessToken(accessToken);
                                userResponse.setSessionId(savedSession.getSessionId());
                                userResponse.setResponseStatus(CustomResponseStatus.USER_CREATED_SUCCESSFULLY);
                                return Mono.just(userResponse);
                            });
                })
                .onErrorResume(e -> {
                    return Mono.error(new RuntimeException("Error creating guest user and session", e));
                });
    }

    public Mono<Boolean> validateGuestUser(String userId) {
        return guestUserRepository.validateGuestUser(userId, Roles.GUEST_USER.name());
    }

}
