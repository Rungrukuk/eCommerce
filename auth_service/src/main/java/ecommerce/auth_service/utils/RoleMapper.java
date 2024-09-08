package ecommerce.auth_service.utils;

import ecommerce.auth_service.domain.Role;
import ecommerce.auth_service.dto.RoleDTO;
import java.util.Collections;

import java.util.stream.Collectors;

public class RoleMapper {

    public static RoleDTO toRoleDTO(Role role) {
        if (role == null) {
            return null;
        }

        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setId(role.getId());

        if (role.getName() != null) {
            roleDTO.setName(role.getName());
        }

        if (role.getPermissions() != null) {
            roleDTO.setPermissions(role.getPermissions().stream()
                    .map(PermissionMapper::toPermissionDTO)
                    .collect(Collectors.toList()));
        } else {
            roleDTO.setPermissions(Collections.emptyList());
        }

        return roleDTO;
    }

    public static Role toRoleEntity(RoleDTO roleDTO) {
        if (roleDTO == null) {
            return null;
        }

        Role role = new Role();
        role.setId(roleDTO.getId());

        if (roleDTO.getName() != null) {
            role.setName(roleDTO.getName());
        }

        if (roleDTO.getPermissions() != null) {
            role.setPermissions(roleDTO.getPermissions().stream()
                    .map(PermissionMapper::toPermissionEntity)
                    .collect(Collectors.toList()));
        } else {
            role.setPermissions(Collections.emptyList());
        }

        return role;
    }

}
