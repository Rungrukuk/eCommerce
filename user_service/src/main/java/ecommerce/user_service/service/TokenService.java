package ecommerce.user_service.service;

public interface TokenService {
    public String validateTokenAndGetUserId(String token, Destination destination);

    public enum Audience {
        USER_SERVICE
    }

    public enum Destination {
        CREATE_USER_DETAILS
    }
}
