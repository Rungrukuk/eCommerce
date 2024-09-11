package ecommerce.auth_service.util;

public enum CustomResponseStatus {
    /**
     * When access token or session id is wrong but refresh token is correct,
     * created new guest user with new session and token
     */
    SESSION_EXPIRED_CREATED_NEW_SESSION,
    /**
     * When access token or session id is wrong and there are no refresh token,
     * created new guest user with new session and token
     */
    UNAUTHORIZED_USER,
    /**
     * When access token and session id is correct
     */
    AUTHORIZED_USER,
    /**
     * When unexpected error occurs
     */
    UNEXPECTED_ERROR,
    /**
     * When user created successfully
     */
    USER_CREATED_SUCCESSFULLY
}
