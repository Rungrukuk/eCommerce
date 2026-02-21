package ecommerce.user_service.service.implementation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import ecommerce.user_service.repository.location.CityRepository;
import ecommerce.user_service.repository.location.CountryRepository;
import ecommerce.user_service.repository.location.PostalCodeRepository;
import ecommerce.user_service.repository.location.StateRepository;
import ecommerce.user_service.service.InputValidatorService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
// TODO need to implement max char size validation in both user and auth service
@RequiredArgsConstructor
public class InputValidatorServiceImpl implements InputValidatorService {

    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    private final CountryRepository countryRepository;

    private final StateRepository stateRepository;

    private final CityRepository cityRepository;

    private final PostalCodeRepository postalCodeRepository;

    @Override
    public Mono<List<String>> validateInput(Map<String, String> input) {
        List<String> errors = new ArrayList<>();

        String userId = input.get("userId");
        String email = input.get("email");
        String name = input.get("name");
        String surname = input.get("surname");
        String phoneNumber = input.get("phoneNumber");
        String country = input.get("country");
        String state = input.get("state");
        String city = input.get("city");
        String postalCode = input.get("postalCode");
        String addressLine1 = input.get("addressLine1");

        if (userId == null || userId.isEmpty()) {
            errors.add("User ID cannot be null or empty");
        }
        if (email == null || email.isEmpty() || !isValidEmail(email)) {
            errors.add("Invalid or empty email");
        }

        if (!isValidName(name)) {
            errors.add("Invalid or empty name");
        }
        if (!isValidName(surname)) {
            errors.add("Invalid or empty surname");
        }

        if (!isValidPhoneNumber(phoneNumber, country)) {
            errors.add("Invalid or empty phone number");
        }

        if (addressLine1 == null || addressLine1.isEmpty()) {
            errors.add("Address Line 1 cannot be empty");
        }

        return validateLocation(country, state, city, postalCode)
                .map(locationErrors -> {
                    errors.addAll(locationErrors);
                    return errors;
                });
    }

    private Mono<List<String>> validateLocation(String country, String state, String city,
            String postalCode) {
        List<String> locationErrors = new ArrayList<>();

        return countryRepository.findByIso3(country)
                .flatMap(foundCountry -> {
                    if (state == null || state.isEmpty()) {
                        return handleStateEmpty(foundCountry.getId(), city, postalCode,
                                locationErrors);
                    }
                    return handleStatePresent(foundCountry.getId(), state, city, postalCode,
                            locationErrors);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    locationErrors.add("Country not found: " + city);
                    if (state == null || state.isEmpty()) {
                        return handleStateEmpty(null, city, postalCode, locationErrors);
                    }
                    return handleStatePresent(null, state, city, postalCode, locationErrors);
                }));
    }

    private Mono<List<String>> handleStateEmpty(Integer countryId, String city, String postalCode,
            List<String> locationErrors) {
        return stateRepository.findByNameAndCountryId(city, countryId)
                .flatMap(
                        foundState -> processCityAndPostalCode(city, foundState.getId(), postalCode,
                                locationErrors))
                .switchIfEmpty(Mono.defer(() -> cityRepository.findByName(city)
                        .flatMap(foundCity -> processPostalCodeByCity(postalCode, foundCity.getId(),
                                locationErrors))
                        .switchIfEmpty(handleCityNotFound(city, postalCode, locationErrors))));
    }

    private Mono<List<String>> handleStatePresent(Integer countryId, String state, String city,
            String postalCode,
            List<String> locationErrors) {
        return stateRepository.findByNameAndCountryId(state, countryId)
                .flatMap(
                        foundState -> processCityAndPostalCode(city, foundState.getId(), postalCode,
                                locationErrors))
                .switchIfEmpty(Mono.defer(
                        () -> handleStateNotFound(state, city, postalCode, locationErrors)));
    }

    private Mono<List<String>> processCityAndPostalCode(String city, Integer stateId,
            String postalCode,
            List<String> locationErrors) {
        return cityRepository.findByNameAndStateId(city, stateId)
                .flatMap(foundCity -> postalCodeRepository.findByPostalCodeAndStateId(postalCode,
                                stateId)
                        .flatMap(foundPostalCode -> Mono.just(locationErrors))
                        .switchIfEmpty(Mono.defer(
                                () -> handlePostalCodeNotFound(postalCode, locationErrors))))
                .switchIfEmpty(
                        Mono.defer(() -> handleCityNotFound(city, postalCode, locationErrors)));
    }

    private Mono<List<String>> processPostalCodeByCity(String postalCode, Integer cityId,
            List<String> locationErrors) {
        return postalCodeRepository.findByPostalCodeAndCityId(postalCode, cityId)
                .flatMap(foundPostalCode -> Mono.just(locationErrors))
                .switchIfEmpty(
                        Mono.defer(() -> handlePostalCodeNotFound(postalCode, locationErrors)));
    }

    private Mono<List<String>> handleCityNotFound(String city, String postalCode,
            List<String> locationErrors) {
        locationErrors.add("City not found: " + city);
        return postalCodeRepository.findByPostalCode(postalCode)
                .flatMap(foundPostalCode -> Mono.just(locationErrors))
                .switchIfEmpty(
                        Mono.defer(() -> handlePostalCodeNotFound(postalCode, locationErrors)));
    }

    private Mono<List<String>> handlePostalCodeNotFound(String postalCode,
            List<String> locationErrors) {
        locationErrors.add("Postal Code not found: " + postalCode);
        return Mono.just(locationErrors);
    }

    private Mono<List<String>> handleStateNotFound(String state, String city, String postalCode,
            List<String> locationErrors) {
        locationErrors.add("State not found: " + state);
        return cityRepository.findByName(city)
                .flatMap(foundCity -> processPostalCodeByCity(postalCode, foundCity.getId(),
                        locationErrors))
                .switchIfEmpty(
                        Mono.defer(() -> handleCityNotFound(city, postalCode, locationErrors)));
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    private boolean isValidName(String name) {
        if (name == null) {
            return false;
        }
        String nameRegex = "^[a-zA-Z ]+$";
        return name.matches(nameRegex);
    }

    private boolean isValidPhoneNumber(String phoneNumber, String countryCode) {
        try {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                return false;
            }
            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(phoneNumber, countryCode);
            return phoneNumberUtil.isValidNumber(parsedNumber);
        } catch (NumberParseException e) {
            return false;
        }
    }
}
