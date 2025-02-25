package ecommerce.user_service.service.Implementation;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import ecommerce.user_service.domain.user.Address;
import ecommerce.user_service.domain.user.User;
import ecommerce.user_service.dto.UserResponse;
import ecommerce.user_service.repository.user.AddressRepository;
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
    @Qualifier("userR2dbcEntityTemplate")
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Override
    public Mono<UserResponse> createUserDetails(Map<String, String> data) {
        return inputValidatorService.validateInput(data)
                .flatMap(
                        errors -> {
                            if (errors.isEmpty()) {
                                return r2dbcEntityTemplate.insert(User.class).using(createUserFromData(data))
                                        .flatMap(
                                                savedUser -> {
                                                    return addressRepository.save(creatAddressFromData(data)).map(
                                                            savedAddress -> {
                                                                return buildSuccessResponse();
                                                            });
                                                })
                                        .as(transactionalOperator::transactional)
                                        .onErrorResume(this::handleError);
                            }
                            return createBadRequestResponse(errors.toString());

                        })
                .onErrorResume(this::handleError);
    }

    @Override
    public Mono<UserResponse> getUserDetails(String userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUser'");
    }

    @Override
    public Mono<UserResponse> deleteUserDetails(String userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteUser'");
    }

    @Override
    public Mono<UserResponse> updateUserDetails(UserResponse user) {
        // TODO Auto-generated method stub
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
        newAddress.setUserId(data.get("userId"));
        newAddress.setCountry(data.get("country"));
        newAddress.setState(data.get("state"));
        newAddress.setCity(data.get("city"));
        newAddress.setPostalCode(data.get("postalCode"));
        newAddress.setAddressLine_1(data.get("addressLine1"));
        newAddress.setAddressLine_2(data.get("addressLine2"));
        return newAddress;
    }

    private Mono<UserResponse> handleError(Throwable e) {
        // TODO Handle Error Gracefully
        System.out.println(e.getMessage());
        e.printStackTrace();
        return Mono.just(buildErrorResponse("Unexpected error", CustomResponseStatus.UNEXPECTED_ERROR, 500));
    }

    private Mono<UserResponse> createBadRequestResponse(String message) {
        return Mono.just(buildErrorResponse(message, CustomResponseStatus.BAD_REQUEST, 400));
    }

    private UserResponse buildErrorResponse(String message, CustomResponseStatus status, int statusCode) {
        UserResponse response = new UserResponse();
        response.setMessage(message);
        response.setResponseStatus(status);
        response.setStatusCode(statusCode);
        return response;
    }

    private UserResponse buildSuccessResponse() {
        UserResponse response = new UserResponse();
        response.setMessage("User details created successfully");
        response.setResponseStatus(CustomResponseStatus.OK);
        response.setStatusCode(200);
        return response;
    }
}
