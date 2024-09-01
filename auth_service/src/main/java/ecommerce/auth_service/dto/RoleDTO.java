package ecommerce.auth_service.dto;

import java.util.List;

import lombok.Data;

@Data
public class RoleDTO {
    private Long id;
    private String name;
    private List<PermissionDTO> permissions;
}
