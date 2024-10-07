package ecommerce.auth_service.service.implementation;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ecommerce.auth_service.repository.RolePermissionRepository;
import ecommerce.auth_service.service.RoleService;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Service
public class RoleServiceImpl implements RoleService {
    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Override
    public Mono<Boolean> hasAccess(String roleName, List<String> services, List<String> destinations) {
        if (services.size() != destinations.size()) {
            return Mono.just(false);
        }

        List<Tuple2<String, String>> serviceDestinations = IntStream.range(0, services.size())
                .mapToObj(i -> Tuples.of(services.get(i), destinations.get(i)))
                .collect(Collectors.toList());

        return rolePermissionRepository.findPermissionsByRoleName(roleName)
                .map(permission -> Tuples.of(permission.getService(), permission.getDestination()))
                .collectList()
                .map(permissions -> serviceDestinations.stream().allMatch(permissions::contains));
    }

}
