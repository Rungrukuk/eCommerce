package ecommerce.api_gateway.util;

public class AudienceDestination {
    private final String audience;
    private final String destination;

    public AudienceDestination(String audience, String destination) {
        this.audience = audience;
        this.destination = destination;
    }

    public String getAudience() {
        return audience;
    }

    public String getDestination() {
        return destination;
    }
}
