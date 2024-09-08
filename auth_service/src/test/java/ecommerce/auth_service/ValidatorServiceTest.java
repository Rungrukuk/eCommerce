package ecommerce.auth_service;

import ecommerce.auth_service.dto.UserCreateDTO;
import ecommerce.auth_service.service.ValidatorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorServiceTest {

    private ValidatorService validatorService;

    @BeforeEach
    void setUp() {
        validatorService = new ValidatorService();
    }

    @Test
    void validateData_WithValidEmailAndStrongPassword_ShouldReturnNoErrors() {
        UserCreateDTO userCreateDTO = new UserCreateDTO();
        userCreateDTO.setEmail("testuser@example.com");
        userCreateDTO.setPassword("StrongP@ssw0rd");
        userCreateDTO.setRePassword("StrongP@ssw0rd");

        List<String> errors = validatorService.validateData(userCreateDTO);

        assertTrue(errors.isEmpty(), "There should be no validation errors");
    }

    @Test
    void validateData_WithInvalidEmail_ShouldReturnEmailError() {
        UserCreateDTO userCreateDTO = new UserCreateDTO();
        userCreateDTO.setEmail("invalid-email");
        userCreateDTO.setPassword("StrongP@ssw0rd");
        userCreateDTO.setRePassword("StrongP@ssw0rd");

        List<String> errors = validatorService.validateData(userCreateDTO);

        assertEquals(1, errors.size());
        assertTrue(errors.contains("Email is not valid!"), "Should return email validation error");
    }

    @Test
    void validateData_WithWeakPassword_ShouldReturnPasswordError() {
        UserCreateDTO userCreateDTO = new UserCreateDTO();
        userCreateDTO.setEmail("testuser@example.com");
        userCreateDTO.setPassword("weakpass");
        userCreateDTO.setRePassword("weakpass");

        List<String> errors = validatorService.validateData(userCreateDTO);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Password is not strong enough"), "Should return password strength error");
    }

    @Test
    void validateData_WithMismatchedPasswords_ShouldReturnPasswordsMismatchError() {
        UserCreateDTO userCreateDTO = new UserCreateDTO();
        userCreateDTO.setEmail("testuser@example.com");
        userCreateDTO.setPassword("StrongP@ssw0rd");
        userCreateDTO.setRePassword("DifferentP@ssw0rd");

        List<String> errors = validatorService.validateData(userCreateDTO);

        assertEquals(1, errors.size());
        assertTrue(errors.contains("Passwords do not match."), "Should return password mismatch error");
    }

    @Test
    void validateData_WithMultipleErrors_ShouldReturnAllErrors() {
        UserCreateDTO userCreateDTO = new UserCreateDTO();
        userCreateDTO.setEmail("invalid-email");
        userCreateDTO.setPassword("weakpass");
        userCreateDTO.setRePassword("differentpass");

        List<String> errors = validatorService.validateData(userCreateDTO);

        assertEquals(3, errors.size(), "Should return three validation errors");
        assertTrue(errors.contains("Email is not valid!"), "Should return email validation error");
        assertTrue(errors.stream().anyMatch(error -> error.contains("Password is not strong enough")),
                "Should return password strength error");
        assertTrue(errors.contains("Passwords do not match."), "Should return password mismatch error");
    }
}
