package ecommerce.api_gateway.util;

import java.util.HashMap;
import java.util.Map;

public class AudienceDestinationMapper {
    private static final Map<String, AudienceDestination> MAPPING = new HashMap<>();

    static {
        MAPPING.put("/register", new AudienceDestination("AUTH_SERVICE", "REGISTER"));
    }

    public static AudienceDestination getMapping(String path) {
        return MAPPING.getOrDefault(path, new AudienceDestination("DEFAULT_AUDIENCE", "DEFAULT_DESTINATION"));
    }
}