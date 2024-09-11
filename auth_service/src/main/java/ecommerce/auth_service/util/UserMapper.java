package ecommerce.auth_service.util;

import ecommerce.auth_service.domain.User;
import ecommerce.auth_service.dto.UserDTO;

// TODO Need to fix role issue when it's necessary
public class UserMapper {

    public static UserDTO toUserDTO(User user) {
        if (user == null) {
            return null;
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(user.getUserId());
        userDTO.setEmail(user.getEmail());
        // userDTO.setRole(user.getRole());

        return userDTO;
    }

    public static User toUserEntity(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }

        User user = new User();
        user.setUserId(userDTO.getUserId());
        user.setEmail(userDTO.getEmail());
        // user.setRole(userDTO.getRole());

        return user;
    }
}
