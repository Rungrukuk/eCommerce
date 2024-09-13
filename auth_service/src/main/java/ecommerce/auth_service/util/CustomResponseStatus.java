package ecommerce.auth_service.util;

// TODO consider sending these responses to api gateway
public enum CustomResponseStatus {
    /**
     * When user is authenticated and has permissions for the given resources
     */
    AUTHORIZED_USER,
    /**
     * When user is unauthenticated but has permissions for the given resources
     */
    AUTHORIZED_GUEST_USER,
    /**
     * When user has no access to the given resources, but authenticated
     */
    UNAUTHORIZED_USER,
    /**
     * When user has no access to the given resources, and unauthenticated with
     * valid session and access token
     */
    UNAUTHORIZED_GUEST_USER,
    /**
     * When user has no access to the given resources, and unauthenticated with
     * invalid session and access token. Created new session and access token
     */
    UNAUTHENTICATED_GUEST_USER,
    /**
     * When unexpected error occurs
     */
    UNEXPECTED_ERROR,
    /**
     * When user created successfully
     */
    USER_CREATED_SUCCESSFULLY
}
