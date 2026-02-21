package ecommerce.auth_service.service.implementation;

import ecommerce.auth_service.dto.BaseResponse;
import ecommerce.auth_service.repository.GuestUserRepository;
import ecommerce.auth_service.repository.SessionRepository;
import ecommerce.auth_service.security.JwtTokenProvider;
import ecommerce.auth_service.service.GuestUserService;
import ecommerce.auth_service.util.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class GuestUserServiceImpl implements GuestUserService {

    private final JwtTokenProvider tokenProvider;

    private final GuestUserRepository guestUserRepository;

    private final SessionRepository sessionRepository;

    @Override
    public Mono<BaseResponse> createGuestUser() {
        BaseResponse userResponse = new BaseResponse();
        return guestUserRepository.saveGuestUser(Roles.GUEST_USER.name())
                .flatMap(guestUserDTO -> {
                    String accessToken = tokenProvider.createAccessToken(guestUserDTO.getUserId(),
                            Roles.GUEST_USER.name());
                    return sessionRepository.saveSession(accessToken)
                            .flatMap(savedSession -> {
                                userResponse.setAccessToken(accessToken);
                                userResponse.setSessionId(savedSession.getSessionId());
                                return Mono.just(userResponse);
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error creating guest user", e);
                    return Mono.error(
                            new RuntimeException("Error creating guest user and session", e));
                });
    }

    public Mono<Boolean> validateGuestUser(String userId) {
        return guestUserRepository.validateGuestUser(userId, Roles.GUEST_USER.name());
    }

}
