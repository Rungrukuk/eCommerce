package ecommerce.api_gateway.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

public class ServiceDestinationMapper {
        private static final Map<String, ServiceDestination> MAPPING = new HashMap<>();

        static {
                MAPPING.put("/", new ServiceDestination(
                                Arrays.asList("NONE"),
                                Arrays.asList("NONE")));
                MAPPING.put("/register", new ServiceDestination(
                                Arrays.asList(Services.AUTH_SERVICE.name()),
                                Arrays.asList("REGISTER")));
                MAPPING.put("/login", new ServiceDestination(
                                Arrays.asList(Services.AUTH_SERVICE.name()),
                                Arrays.asList("LOGIN")));
                MAPPING.put("/user-details", new ServiceDestination(
                                Arrays.asList("USER_SERVICE"),
                                Arrays.asList("CREATE_USER_DETAILS")));
        }

        public static ServiceDestination getMapping(String path) {
                return MAPPING.getOrDefault(path, new ServiceDestination(
                                Arrays.asList("NONE"),
                                Arrays.asList("NONE")));
        }
}