package ecommerce.auth_service.utils;

import ecommerce.auth_service.domain.GuestUser;
import ecommerce.auth_service.dto.GuestUserDTO;

public class GuestUserMapper {

    public static GuestUserDTO toGuestUserDTO(GuestUser guestUser) {
        if (guestUser == null) {
            return null;
        }

        GuestUserDTO guestUserDTO = new GuestUserDTO();
        guestUserDTO.setUserId(guestUser.getUserId());
        guestUserDTO.setRole(RoleMapper.toRoleDTO(guestUser.getRole()));

        return guestUserDTO;
    }

    public static GuestUser toGuestUserEntity(GuestUserDTO guestUserDTO) {
        if (guestUserDTO == null) {
            return null;
        }

        GuestUser guestUser = new GuestUser();
        guestUser.setUserId(guestUserDTO.getUserId());
        guestUser.setRole(RoleMapper.toRoleEntity(guestUserDTO.getRole()));
        return guestUser;
    }
}
