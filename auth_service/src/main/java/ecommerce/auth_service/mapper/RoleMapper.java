package ecommerce.auth_service.mapper;

import ecommerce.auth_service.domain.Role;
import ecommerce.auth_service.dto.RoleDTO;

import java.util.stream.Collectors;

public class RoleMapper {

    public static RoleDTO toRoleDTO(Role role) {
        if (role == null) {
            return null;
        }

        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setId(role.getId());
        roleDTO.setName(role.getName());
        roleDTO.setPermissions(role.getPermissions().stream()
                .map(PermissionMapper::toPermissionDTO)
                .collect(Collectors.toList()));

        return roleDTO;
    }

    public static Role toRoleEntity(RoleDTO roleDTO) {
        if (roleDTO == null) {
            return null;
        }

        Role role = new Role();
        role.setId(roleDTO.getId());
        role.setName(roleDTO.getName());
        role.setPermissions(roleDTO.getPermissions().stream()
                .map(PermissionMapper::toPermissionEntity)
                .collect(Collectors.toList()));

        return role;
    }
}
