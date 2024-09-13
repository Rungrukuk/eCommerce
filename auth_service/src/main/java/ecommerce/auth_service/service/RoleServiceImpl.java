package ecommerce.auth_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ecommerce.auth_service.repository.PermissionRepository;
import ecommerce.auth_service.repository.RolePermissionRepository;
import reactor.core.publisher.Mono;

@Service
public class RoleServiceImpl implements RoleService {
    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public Mono<Boolean> hasAccess(String roleName, String audience, String destination) {
        return rolePermissionRepository.findByRoleName(roleName)
                .flatMap(rolePermission -> permissionRepository.findById(rolePermission.getPermissionId()))
                .filter(permission -> permission.getAudience().equals(audience)
                        && permission.getDestination().equals(destination))
                .hasElements();
    }
}
