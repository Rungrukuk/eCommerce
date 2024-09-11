package ecommerce.auth_service.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import ecommerce.auth_service.dto.UserCreateDTO;

@Component
public class InputValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?\\.[a-zA-Z]{2,63}$");

    private static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^a-zA-Z0-9]");

    private static boolean isStrongPassword(String password) {
        if (password == null) {
            return false;
        }
        return password.length() >= 8
                && UPPERCASE_PATTERN.matcher(password).find()
                && LOWERCASE_PATTERN.matcher(password).find()
                && DIGIT_PATTERN.matcher(password).find()
                && SPECIAL_CHAR_PATTERN.matcher(password).find();
    }

    public List<String> validateData(UserCreateDTO userCreateDTO) {
        List<String> errors = new ArrayList<>();
        if (!isValidEmail(userCreateDTO.getEmail())) {
            errors.add("Email is not valid!");
        }

        if (!isStrongPassword(userCreateDTO.getPassword())) {
            errors.add("""
                        Password is not strong enough.
                        Ensure it has at least 1 uppercase letter, 1 lowercase letter,
                        1 special character, and 1 numerical character.
                    """);
        }

        if (!userCreateDTO.getPassword().equals(userCreateDTO.getRePassword())) {
            errors.add("Passwords do not match.");
        }
        return errors;
    }
}
