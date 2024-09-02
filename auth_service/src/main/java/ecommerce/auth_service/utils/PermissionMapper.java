package ecommerce.auth_service.utils;

import ecommerce.auth_service.domain.Permission;
import ecommerce.auth_service.dto.PermissionDTO;

public class PermissionMapper {

    public static PermissionDTO toPermissionDTO(Permission permission) {
        if (permission == null) {
            return null;
        }

        PermissionDTO permissionDTO = new PermissionDTO();
        permissionDTO.setId(permission.getId());
        permissionDTO.setName(permission.getName());

        return permissionDTO;
    }

    public static Permission toPermissionEntity(PermissionDTO permissionDTO) {
        if (permissionDTO == null) {
            return null;
        }

        Permission permission = new Permission();
        permission.setId(permissionDTO.getId());
        permission.setName(permissionDTO.getName());

        return permission;
    }
}
