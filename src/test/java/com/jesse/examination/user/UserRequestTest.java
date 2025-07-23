package com.jesse.examination.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.examination.core.properties.ProjectProperties;
import com.jesse.examination.core.respponse.ResponseBuilder;
import com.jesse.examination.user.dto.UserDeleteDTO;
import com.jesse.examination.user.dto.UserLoginDTO;
import com.jesse.examination.user.dto.UserRegistrationDTO;
import com.jesse.examination.user.redis.UserRedisService;
import com.jesse.examination.user.repository.UserRepository;
import com.jesse.examination.user.route.UserServiceRouteConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.opentest4j.TestAbortedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static com.jesse.examination.core.email.utils.VerifyCodeGenerator.generateVerifyCode;

/** 用户服务请求测试类。*/
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserRequestTest
{
    @Autowired
    private Executor userOperatorExecutor;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRedisService userRedisService;

    @Autowired
    private ProjectProperties projectProperties;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    private WebTestClient webTestClient;

    private AtomicLong userId;

    private int VERIFYCODE_LENGTH;

    private int currentTestIndex = 0;
    private static final int TEST_AMOUNT = 5;

    @AfterEach
    public void resumUserIdAutoIncrement()
    {
        ++currentTestIndex;

        if (currentTestIndex == TEST_AMOUNT)
        {
            log.info("User test totally complete! Resume AUTO_INCREMENT = 1");

            this.userRepository.resumeAutoIncrement();
        }
    }

    @PostConstruct
    public void warmUpConnectionPool() {
        this.userRepository.count().block();
    }

    @PostConstruct
    public void setVerifyCodeLen()
    {
        VERIFYCODE_LENGTH = Integer.parseInt(
            this.projectProperties.getVarifyCodeLength()
        );
    }

    @PostConstruct
    public void setBeginUserId()
    {
        long getMaxUserId
            = Objects.requireNonNull(this.userRepository
                     .findMaxUserId()
                     .switchIfEmpty(Mono.just(0L))
                     .block()
            ) + 1;

        userId = new AtomicLong(getMaxUserId);

        log.info("Latest user_id = {}.", getMaxUserId);
    }

    @PostConstruct
    public void bindRouterFunction()
    {
        var userServiceRouteConfig
            = this.applicationContext
                  .getBean(UserServiceRouteConfig.class);

        this.webTestClient
            = WebTestClient.bindToRouterFunction(
                userServiceRouteConfig.userRouterFunction()
            ).build();
    }

    private @NotNull String
    generatePhoneNumber()
    {
        return Objects.requireNonNull(
                generateVerifyCode(11).block()
            ).replaceFirst("^.", "1");
    }

    private @NotNull List<String>
    getAllUserName()
    {
        return Objects.requireNonNull(
            this.userRepository
                .findAllUserName()
                .switchIfEmpty(Flux.empty())
                .collectList().block()
        );
    }

    /**
     * {
     *     "userName" : "Test_3",
     *     "password" : "1234567890",
     *     "fullName" : "Test_User_3",
     *     "telephoneNumber" : "13677898776",
     *     "email" : "zhj3191955858@gmail.com"
     * }
     */
    private String generateUserData()
    {
        Map<String, String> userData = new LinkedHashMap<>();

        long userId = this.userId.getAndIncrement();

        userData.put("userName", "Test_" + userId);
        userData.put("password", "1234567890");
        userData.put("fullName", "Test_User_" + userId);
        userData.put("telephoneNumber", this.generatePhoneNumber());
        userData.put("email", "zhj3191955858@gmail.com");

        String userJson;

        try
        {
             userJson
                = this.objectMapper.writeValueAsString(userData);
        }
        catch (JsonProcessingException exception)
        {
            log.error(
                "[generateUserData()] Json processing failed! Cause: {}",
                exception.getMessage()
            );

            throw new TestAbortedException("Test abort!", exception);
        }

        return userJson;
    }

    private List<String>
    joinResponseFuture(
        @NotNull
        List<CompletableFuture<String>> responseFuture
    )
    {
        return responseFuture.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    @Order(1)
    @Test
    public void TestUserRegister()
    {
        final ParameterizedTypeReference<ResponseBuilder.APIResponse<UserRegistrationDTO>>
            userRegisterResponse = new ParameterizedTypeReference<>() {};

        final int CREATE_AMOUNT = 1000;

        List<CompletableFuture<String>> responseFuture =
        IntStream.range(0, CREATE_AMOUNT)
            .mapToObj(
                (index) ->
                    CompletableFuture.supplyAsync(
                        () -> {
                            try
                            {
                                ResponseBuilder.APIResponse<UserRegistrationDTO> response =
                                this.webTestClient
                                    .post()
                                    .uri("/api/user/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .bodyValue(generateUserData())
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(userRegisterResponse)
                                    .returnResult().getResponseBody();

                                assert response != null && response.getStatus().equals(HttpStatus.OK);

                                return this.objectMapper.writeValueAsString(response);
                            }
                            catch (JsonProcessingException exception)
                            {
                                log.error(
                                    "[TestUserRegister()] Json processing failed! Cause: {}",
                                    exception.getMessage(), exception
                                );

                                throw new TestAbortedException("Test abort!", exception);
                            }
                            catch (Exception exception)
                            {
                                log.error(
                                    "[TestUserRegister()] Create new user (index = {}), Cause: {}",
                                    index, exception.getMessage(), exception
                                );

                                throw new TestAbortedException("Test abort!", exception);
                            }
                        }, this.userOperatorExecutor
                    )
            ).toList();

        log.info("Create {} user data complete, show responses: ", CREATE_AMOUNT);
        this.joinResponseFuture(responseFuture)
            .forEach(System.out::println);
    }

    @Order(2)
    @Test
    public void TestUserLogin()
    {
        final ParameterizedTypeReference<ResponseBuilder.APIResponse<Object>>
            userLoginResponseType = new ParameterizedTypeReference<>() {};

        List<String> allUserNames  = this.getAllUserName();

        List<CompletableFuture<String>> responseFuture =
        allUserNames.stream()
            .map((userName) ->
                CompletableFuture.supplyAsync(
                    () -> {
                        try
                        {
                            String verifyCode = generateVerifyCode(VERIFYCODE_LENGTH).block();

                            this.userRedisService
                                .saveUserVerifyCode(userName, verifyCode)
                                .block();

                            ResponseBuilder.APIResponse<Object> response =
                                this.webTestClient
                                    .post()
                                    .uri("/api/user/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .bodyValue(UserLoginDTO.of(userName, "1234567890", verifyCode))
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(userLoginResponseType)
                                    .returnResult().getResponseBody();

                            return this.objectMapper.writeValueAsString(response);
                        }
                        catch (JsonProcessingException exception)
                        {
                            log.error(
                                "[TestUserLogin()] Json processing failed! Cause: {}",
                                exception.getMessage(), exception
                            );

                            throw new TestAbortedException("Test abort!", exception);
                        }
                        catch (Exception exception)
                        {
                            log.error(
                                "[TestUserLogin()] User: {} logout failed! Cause: {}.",
                                userName, exception.getMessage(), exception
                            );

                            throw new TestAbortedException("Test abort!", exception);
                        }
                    }, this.userOperatorExecutor)
            ).toList();

        log.info("All user login complete! show responses: ");
        this.joinResponseFuture(responseFuture)
            .forEach(System.out::println);
    }

//    @Order(3)
//    @Test
//    public void TestUserModify()
//    {
//        final ParameterizedTypeReference<ResponseBuilder.APIResponse<Object>>
//            userModifyResponseType = new ParameterizedTypeReference<>() {};
//
//        // ThreadLocalRandom random = ThreadLocalRandom.current();
//
//        List<String> allUserNames = this.getAllUserName();
//
//        List<CompletableFuture<String>> responseFuture
//            = allUserNames.stream()
//            .map((userName) ->
//                CompletableFuture.supplyAsync(
//                    () -> {
//                        try
//                        {
//                            ResponseBuilder.APIResponse<Object> response =
//                                this.webTestClient.put()
//                                    .uri("/api/user/modify")
//                                    .contentType(MediaType.APPLICATION_JSON)
//                                    .accept(MediaType.APPLICATION_JSON)
//                                    .bodyValue(
//                                        new UserModifyDTO()
//                                            .setOldUserName(userName)
//                                            .setNewUserName(userName + "_233")
//                                            .setOldPassword("1234567890")
//                                            .setNewPassword("1234567890")
//                                            .setNewTelephoneNumber(this.generatePhoneNumber())
//                                            .setNewFullName(userName + "_6666")
//                                            .setNewEmail("zhj3191955858@gamil.com"))
//                                    .exchange()
//                                    .expectStatus().isOk()
//                                    .expectBody(userModifyResponseType)
//                                    .returnResult().getResponseBody();
//
//                            return this.objectMapper
//                                .writeValueAsString(response);
//                        }
//                        catch (JsonProcessingException exception)
//                        {
//                            log.error(
//                                "[TestUserLogout()] Json processing failed! Cause: {}",
//                                exception.getMessage(), exception
//                            );
//
//                            throw new TestAbortedException("Test abort!", exception);
//                        }
//                        catch (Exception exception)
//                        {
//                            log.error(
//                                "[TestUserLogout()] User: {} logout failed! Cause: {}.",
//                                userName, exception.getMessage(), exception
//                            );
//
//                            throw new TestAbortedException("Test abort!", exception);
//                        }
//                    }, this.userOperatorExecutor)
//            ).toList();
//
//        log.info("Modify all user complete, show responses: ");
//        this.joinResponseFuture(responseFuture)
//            .forEach(System.out::println);
//    }

    @Order(4)
    @Test
    public void TestUserLogout()
    {
        final ParameterizedTypeReference<ResponseBuilder.APIResponse<Object>>
            userLoginResponseType = new ParameterizedTypeReference<>() {};

        List<String> allUserNames  = this.getAllUserName();

        List<CompletableFuture<String>> responseFuture
            = allUserNames.stream()
                .map((userName) ->
                    CompletableFuture.supplyAsync(
                    () -> {
                        try
                        {
                            ResponseBuilder.APIResponse<Object> response =
                                this.webTestClient
                                    .post()
                                    .uri("/api/user/logout?name=" + userName)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(userLoginResponseType)
                                    .returnResult().getResponseBody();

                            assert response != null && response.getStatus().equals(HttpStatus.OK);

                            return this.objectMapper
                                .writeValueAsString(response);
                        }
                        catch (JsonProcessingException exception)
                        {
                            log.error(
                                "[TestUserLogout()] Json processing failed! Cause: {}",
                                exception.getMessage(), exception
                            );

                            throw new TestAbortedException("Test abort!", exception);
                        }
                        catch (Exception exception)
                        {
                            log.error(
                                "[TestUserLogout()] User: {} logout failed! Cause: {}.",
                                userName, exception.getMessage(), exception
                            );

                            throw new TestAbortedException("Test abort!", exception);
                        }
                    }, this.userOperatorExecutor)
                ).toList();

        log.info("Logout all user complete, show responses: ");
        this.joinResponseFuture(responseFuture)
            .forEach(System.out::println);
    }

    @Order(5)
    @Test
    public void TestUserDelete()
    {
        final ParameterizedTypeReference<ResponseBuilder.APIResponse<Object>>
            userDeleteResponse = new ParameterizedTypeReference<>() {};

        List<String> allUserNames  = this.getAllUserName();

        List<CompletableFuture<String>> responseFuture
            = allUserNames.stream()
            .map((userName) ->
                CompletableFuture.supplyAsync(
                    () -> {
                        try
                        {
                            String verifyCode
                                = generateVerifyCode(VERIFYCODE_LENGTH).block();

                            this.userRedisService
                                .saveUserVerifyCode(userName, verifyCode)
                                .block();

                            ResponseBuilder.APIResponse<Object> response =
                            this.webTestClient
                                .mutate().build()
                                .method(HttpMethod.DELETE)
                                .uri("/api/user/delete")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .bodyValue(UserDeleteDTO.of(userName, "1234567890", verifyCode))
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody(userDeleteResponse)
                                .returnResult().getResponseBody();

                            assert response != null && response.getStatus().equals(HttpStatus.OK);

                            return this.objectMapper
                                .writeValueAsString(response);
                        }
                        catch (JsonProcessingException exception)
                        {
                            log.error(
                                "[TestUserLogout()] Json processing failed! Cause: {}",
                                exception.getMessage(), exception
                            );

                            throw new TestAbortedException("Test abort!", exception);
                        }
                        catch (Exception exception)
                        {
                            log.error(
                                "[TestUserLogout()] User: {} logout failed! Cause: {}.",
                                userName, exception.getMessage(), exception
                            );

                            throw new TestAbortedException("Test abort!", exception);
                        }
                    }, this.userOperatorExecutor)
            ).toList();

        log.info("Delete all user data complete, show responses: ");

        this.joinResponseFuture(responseFuture)
            .forEach(System.out::println);
    }
}
