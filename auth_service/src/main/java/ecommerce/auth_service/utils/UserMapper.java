package ecommerce.auth_service.utils;

import ecommerce.auth_service.domain.GuestUser;
import ecommerce.auth_service.domain.User;
import ecommerce.auth_service.dto.UserDTO;

public class UserMapper {

    public static UserDTO toUserDTO(User user) {
        if (user == null) {
            return null;
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(user.getUserId());
        userDTO.setEmail(user.getEmail());
        userDTO.setRole(RoleMapper.toRoleDTO(user.getRole()));

        return userDTO;
    }

    public static UserDTO toUserDTO(GuestUser user) {
        if (user == null) {
            return null;
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(user.getUserId());
        userDTO.setRole(RoleMapper.toRoleDTO(user.getRole()));

        return userDTO;
    }

    public static User toUserEntity(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }

        User user = new User();
        user.setUserId(userDTO.getUserId());
        user.setEmail(userDTO.getEmail());
        user.setRole(RoleMapper.toRoleEntity(userDTO.getRole()));

        return user;
    }
}
