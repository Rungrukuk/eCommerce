package ecommerce.user_service.service.Implementation;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import ecommerce.user_service.domain.user.Address;
import ecommerce.user_service.domain.user.User;
import ecommerce.user_service.domain.user.UserAddress;
import ecommerce.user_service.dto.UserResponse;
import ecommerce.user_service.repository.user.AddressRepository;
import ecommerce.user_service.repository.user.UserAddressRepository;
import ecommerce.user_service.service.InputValidatorService;
import ecommerce.user_service.service.UserService;
import ecommerce.user_service.util.CustomResponseStatus;
import reactor.core.publisher.Mono;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private InputValidatorService inputValidatorService;

    @Autowired
    @Qualifier("userTransactionalOperator")
    private TransactionalOperator transactionalOperator;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    @Qualifier("userR2dbcEntityTemplate")
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Override
    public Mono<UserResponse> createUserDetails(Map<String, String> data) {
        return inputValidatorService.validateInput(data)
                .flatMap(errors -> {
                    if (errors.isEmpty()) {
                        User user = createUserFromData(data);
                        Address address = creatAddressFromData(data);

                        return r2dbcEntityTemplate.insert(User.class).using(user)
                                .flatMap(savedUser -> addressRepository.save(address)
                                        .flatMap(savedAddress -> createUserAddressEntry(savedUser.getId(),
                                                savedAddress.getId()))
                                        .thenReturn(buildSuccessResponse()))
                                .as(transactionalOperator::transactional)
                                .onErrorResume(this::handleError);
                    }
                    return createBadRequestResponse(errors.toString());
                })
                .onErrorResume(this::handleError);
    }

    @Override
    public Mono<UserResponse> getUserDetails(String userId) {
        throw new UnsupportedOperationException("Unimplemented method 'getUser'");
    }

    @Override
    public Mono<UserResponse> deleteUserDetails(String userId) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteUser'");
    }

    @Override
    public Mono<UserResponse> updateUserDetails(UserResponse user) {
        throw new UnsupportedOperationException("Unimplemented method 'updateUser'");
    }

    private User createUserFromData(Map<String, String> data) {
        User newUser = new User();
        newUser.setId(data.get("userId"));
        newUser.setName(data.get("name"));
        newUser.setSurname(data.get("surname"));
        newUser.setPhoneNumber(data.get("phoneNumber"));
        return newUser;
    }

    private Address creatAddressFromData(Map<String, String> data) {
        Address newAddress = new Address();
        newAddress.setCountry(data.get("country"));
        newAddress.setState(data.get("state"));
        newAddress.setCity(data.get("city"));
        newAddress.setPostalCode(data.get("postalCode"));
        newAddress.setAddressLine_1(data.get("addressLine1"));
        newAddress.setAddressLine_2(data.get("addressLine2"));
        return newAddress;
    }

    private Mono<UserAddress> createUserAddressEntry(String userId, Long addressId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        userAddress.setAddressId(addressId);
        userAddress.setDefault(true);
        return userAddressRepository.save(userAddress);
    }

    private Mono<UserResponse> handleError(Throwable e) {
        System.out.println(e.getMessage());
        e.printStackTrace();
        return Mono.just(buildErrorResponse(CustomResponseStatus.UNEXPECTED_ERROR, "Unexpected error", 500));
    }

    private Mono<UserResponse> createBadRequestResponse(String message) {
        return Mono.just(buildErrorResponse(CustomResponseStatus.BAD_REQUEST, message, 400));
    }

    private UserResponse buildErrorResponse(CustomResponseStatus status, String message, int statusCode) {
        return new UserResponse(status, message, statusCode);
    }

    private UserResponse buildSuccessResponse() {
        return new UserResponse(CustomResponseStatus.OK, "User details created successfully", 200);
    }
}
