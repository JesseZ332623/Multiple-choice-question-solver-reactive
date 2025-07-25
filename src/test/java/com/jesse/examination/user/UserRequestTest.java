package com.jesse.examination.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jesse.examination.core.properties.ProjectProperties;
import com.jesse.examination.core.respponse.ResponseBuilder;
import com.jesse.examination.question.dto.FullQuestionInfoDTO;
import com.jesse.examination.question.repository.QuestionRepository;
import com.jesse.examination.score.dto.ScoreRecordQueryDTO;
import com.jesse.examination.score.entity.ScoreRecord;
import com.jesse.examination.score.repository.ScoreRecordRepository;
import com.jesse.examination.user.dto.UserDeleteDTO;
import com.jesse.examination.user.dto.UserLoginDTO;
import com.jesse.examination.user.dto.UserRegistrationDTO;
import com.jesse.examination.user.redis.UserRedisService;
import com.jesse.examination.user.repository.UserRepository;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static com.jesse.examination.core.email.utils.VerifyCodeGenerator.generateVerifyCode;
import static com.jesse.examination.question.route.QuestionServiceURL.*;
import static com.jesse.examination.score.RandomTimeGenerator.randomBetween;
import static com.jesse.examination.score.route.ScoreServiceURL.INSERT_NEW_SCORE_URI;
import static com.jesse.examination.score.route.ScoreServiceURL.PAGINATED_SCORE_QUERY_URI;

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
    private QuestionRepository questionRepository;

    @Autowired
    private ScoreRecordRepository scoreRecordRepository;

    @Autowired
    private UserRedisService userRedisService;

    @Autowired
    private ProjectProperties projectProperties;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    private WebTestClient webTestClient;

    /** 项目统一的验证码长度（运行时从配置文件获取）。*/
    private int VERIFYCODE_LENGTH;

    private int PROCESSOR_AMOUNT;

    private int currentTestIndex = 0;
    private static final int TEST_AMOUNT = 5;

    /**
     * 在最后一条测试用例执行完毕后，
     * 重设用户表的 AUTO_INCREMENT 为 1。
     */
    @AfterEach
    public void resumUserIdAutoIncrement()
    {
        ++currentTestIndex;

        if (currentTestIndex == TEST_AMOUNT)
        {
            log.info("User test totally complete! Resume AUTO_INCREMENT = 1");

            this.userRepository
                .resumeAutoIncrement().block();

            this.scoreRecordRepository
                .truncateScoreRecordTable().block();
        }
    }

    /** 获取本设备 CPU 的物理核心数。*/
    @PostConstruct
    public void getProcessorAmountOfThisDevice()
    {
        PROCESSOR_AMOUNT = Runtime.getRuntime().availableProcessors();

        log.info(
            "Processor amount = {}", PROCESSOR_AMOUNT
        );
    }

    /** 预热连接池。*/
    @PostConstruct
    public void warmUpConnectionPool()
    {
        this.userRepository.count().block();
        this.questionRepository.count().block();
        this.scoreRecordRepository.count().block();
    }

    @PostConstruct
    public void setVerifyCodeLen()
    {
        VERIFYCODE_LENGTH = Integer.parseInt(
            this.projectProperties.getVarifyCodeLength()
        );
    }

    /** 将项目的上下文整体绑定给 WebTestClient。*/
    @PostConstruct
    public void bindRouterFunction()
    {
        this.webTestClient
            = WebTestClient.bindToApplicationContext(
                this.applicationContext
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

    private @NotNull List<Long>
    getAllUserIds()
    {
        return Objects.requireNonNull(
            this.userRepository
                .findAllIds()
                .collectList().block()
        );
    }

    private String generateUserData()
    {
        Map<String, String> userData = new LinkedHashMap<>();

        String uuid = UUID.randomUUID().toString();

        userData.put("userName", "Test_" + uuid);
        userData.put("password", "1234567890");
        userData.put("fullName", "Test_User_" + uuid);
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

    private <T> List<T>
    joinResponseFuture(
        @NotNull
        List<CompletableFuture<T>> responseFuture
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

        final int CREATE_AMOUNT = 500;

        List<CompletableFuture<String>> responseFuture =
        IntStream.range(0, CREATE_AMOUNT)
            .mapToObj(
                (index) ->
                    CompletableFuture.supplyAsync(
                        () -> {
                            log.info(
                                "[TestUserRegister()] Current thread: {}",
                                Thread.currentThread().getName()
                            );
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

        final List<String> allUserNames = this.getAllUserName();

        List<CompletableFuture<String>> responseFuture =
        allUserNames.stream()
            .map((userName) ->
                CompletableFuture.supplyAsync(
                    () -> {
                        try
                        {
                            log.info(
                                "[TestUserLogin()] Current thread: {}",
                                Thread.currentThread().getName()
                            );

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

    private @NotNull ScoreRecord
    produceScoreRecord(long specifiedUserId)
    {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        return new ScoreRecord(
            specifiedUserId,
            randomBetween(
                LocalDateTime.of(
                    2015, 1, 1,
                    0, 0, 0),
                LocalDateTime.now()
            ),
            random.nextInt(1, 30),
            random.nextInt(1, 30),
            random.nextInt(1, 30)
        );
    }

    /** 从列表中随机取 limit 个元素，并返回一个不重复的集合。*/
    private <T> List<T>
    getRandomLimit(@NotNull List<T> list, long limit)
    {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Param list not be empty!");
        }

        if (limit < 0) {
            throw new IllegalArgumentException("Param limit not less then 0!");
        }

        if (limit == 0) {
            return Collections.emptyList();
        }

        if (list.size() == limit || list.size() < limit) {
            return new ArrayList<>(list);
        }

        return ThreadLocalRandom
                .current()
                .ints(0, list.size())
                .distinct()
                .limit(limit)
                .mapToObj(list::get)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** 在操作之间随机等待 baseMs ~ 100 毫秒。*/
    void smartWait(int baseMs)
    {
        long waitTime
            = baseMs + ThreadLocalRandom.current().nextInt(100);

        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException ignored) {}
    }

    /** 对于所已经登录的用户，都执行一些成绩、问题的查询和答题操作。*/
    @Order(3)
    @Test
    void doSomethingByLoginUser()
    {
        /* 分页查询问题完整数据的响应体类型。*/
        final ParameterizedTypeReference<ResponseBuilder.APIResponse<List<FullQuestionInfoDTO>>>
            userQuestionQueryResponseType = new ParameterizedTypeReference<>() {};

        /* 用户答对问题时答对次数 + 1 的响应体类型。*/
        final ParameterizedTypeReference<ResponseBuilder.APIResponse<Integer>>
            userAnswerCorrectResponseType = new ParameterizedTypeReference<>() {};

        /* 用户提交新成绩时的响应体类型。*/
        final ParameterizedTypeReference<ResponseBuilder.APIResponse<ScoreRecord>>
            userSubmitNreScoreResponseType = new ParameterizedTypeReference<>() {};

        /* 用户分页查询成绩时的响应体类型。*/
        final ParameterizedTypeReference<ResponseBuilder.APIResponse<List<ScoreRecordQueryDTO>>>
            userScoreQueryResponseType = new ParameterizedTypeReference<>() {};

        /*
         * 限制并发数，只在所有用户中随机挑 PROCESSOR_AMOUNT 个用户模拟操作即可
         * （不要把行为测试玩成极限压力测试！）
         */
        final long CONCURRENT_LIMIT = PROCESSOR_AMOUNT;

        final List<String> testUserNames
            = this.getRandomLimit(this.getAllUserName(), CONCURRENT_LIMIT);

        final List<Long> testUserIds
            = this.getRandomLimit(this.getAllUserIds(), CONCURRENT_LIMIT);

        final Map<Long, String> userInfo
            = IntStream.range(0, testUserNames.size())
                       .boxed()
                       .collect(Collectors.toMap(testUserIds::get, testUserNames::get));

        final long questionAmount
            = Objects.requireNonNull(
                this.questionRepository.count().block()
            );

        final long ANSWER_QUESTION  = questionAmount / 4L;
        final long GENERIC_SCORE    = 25L;
        final long ONE_PAGE_AMOUNT  = 15L;

        final long QUESTION_PAGE_MAX
            = Math.max(1, (long) Math.ceil((double) questionAmount / ONE_PAGE_AMOUNT));

        final long SCORE_PAGE_MAX
            = Math.max(1, (long) Math.ceil((double) GENERIC_SCORE / ONE_PAGE_AMOUNT));

        final Path responseSavaPath
            = Path.of(this.projectProperties.getTestResultPath())
                  .resolve("User-Request-Test")
                  .normalize();

        // 启用 Jackson 的优质打印功能
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        log.info("Question amount = {}", questionAmount);
        log.info("Test user id = {}", testUserIds);
        log.info("QUESTION_PAGE_MAX = {}, SCORE_PAGE_MAX = {}", QUESTION_PAGE_MAX, SCORE_PAGE_MAX);

        List<CompletableFuture<Void>> responsesFuture =
        userInfo.entrySet().stream().map((user) ->
            CompletableFuture.runAsync(() ->
            {
                ThreadLocalRandom random = ThreadLocalRandom.current();

                log.info(
                    "[doSomethingByLoginUser()] Current thread: {}",
                    Thread.currentThread().getName()
                );

                try
                {
                    var questionQueryResponse =
                        this.webTestClient
                            .get()
                            .uri(
                                QUESTION_PAGINATION_QUERY_URI +
                                    "?page=" + random.nextLong(1L, QUESTION_PAGE_MAX) +
                                    "&amount=" + ONE_PAGE_AMOUNT
                            )
                            .accept(MediaType.APPLICATION_JSON)
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody(userQuestionQueryResponseType)
                            .returnResult().getResponseBody();

                    Files.writeString(
                        responseSavaPath
                            .resolve("question-response" + user.getValue() + ".json")
                            .normalize(),
                        this.objectMapper.writeValueAsString(questionQueryResponse),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.CREATE
                    );

                    this.smartWait(random.nextInt(50, 75));

                    var userAnswerCorrectResponse =
                        LongStream.range(0L, ANSWER_QUESTION)
                            .mapToObj((index) ->
                                this.webTestClient
                                    .put()
                                    .uri(
                                        INCREMENT_USER_QUESTION_CORRECT_TIME_URI +
                                            "?name=" + user.getValue() +
                                            "&ques_id=" + random.nextLong(1, questionAmount))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .exchange()
                                    .expectStatus().isOk()
                                    .expectBody(userAnswerCorrectResponseType)
                                    .returnResult().getResponseBody()
                            ).toList();

                    Files.writeString(
                        responseSavaPath
                            .resolve("answer-correct-" + user.getValue() + ".json")
                            .normalize(),
                        this.objectMapper.writeValueAsString(userAnswerCorrectResponse),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.CREATE
                    );

                    this.smartWait(random.nextInt(50, 75));

                    var submitNewScoreResponse =
                        LongStream.range(0L, GENERIC_SCORE)
                            .mapToObj((index) ->
                                this.webTestClient.post()
                                    .uri(INSERT_NEW_SCORE_URI)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .bodyValue(produceScoreRecord(user.getKey()))
                                    .exchange()
                                    .expectStatus().isCreated()
                                    .expectBody(userSubmitNreScoreResponseType)
                                    .returnResult().getResponseBody()
                            ).toList();

                    Files.writeString(
                        responseSavaPath
                            .resolve("submit-new-score" + user.getValue() + ".json")
                            .normalize(),
                        this.objectMapper.writeValueAsString(submitNewScoreResponse),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.CREATE
                    );

                    this.smartWait(random.nextInt(50, 75));

                    var userScoreQueryResponse =
                        this.webTestClient
                            .get()
                            .uri(
                                PAGINATED_SCORE_QUERY_URI +
                                    "?name=" + user.getValue() +
                                    "&page=" + random.nextLong(1, SCORE_PAGE_MAX + 1) +
                                    "&amount=" + ONE_PAGE_AMOUNT)
                            .accept(MediaType.APPLICATION_JSON)
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody(userScoreQueryResponseType)
                            .returnResult().getResponseBody();

                    Files.writeString(
                        responseSavaPath
                            .resolve("score-query-response" + user.getValue() + ".json")
                            .normalize(),
                        this.objectMapper.writeValueAsString(userScoreQueryResponse),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.CREATE
                    );
                }
                catch (JsonProcessingException exception)
                {
                    log.error(
                        "[TestUserLogin()] Json processing failed! Cause: {}.",
                        exception.getMessage(), exception
                    );

                    throw new TestAbortedException("Test abort!", exception);
                }
                catch (IOException exception)
                {
                    log.error(
                        "Save response JSON file failed! Cause: {}.",
                        exception.getMessage(), exception
                    );

                    throw new TestAbortedException("Test abort!", exception);
                }
                catch (Exception exception)
                {
                    log.error(
                        "[TestUserLogin()] User: {} do user operator failed! Cause: {}.",
                        user.getValue(), exception.getMessage(), exception
                    );

                    throw new TestAbortedException("Test abort!", exception);
                }

            }, this.userOperatorExecutor)
        ).toList();

        this.joinResponseFuture(responsesFuture);
    }

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
                        log.info(
                            "[TestUserLogout()] Current thread: {}",
                            Thread.currentThread().getName()
                        );
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
                        log.info(
                            "[TestUserDelete()] Current thread: {}",
                            Thread.currentThread().getName()
                        );
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
