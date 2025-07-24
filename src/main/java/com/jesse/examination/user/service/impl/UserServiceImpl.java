package com.jesse.examination.user.service.impl;

import com.jesse.examination.core.email.dto.EmailContent;
import com.jesse.examination.core.email.exception.EmailException;
import com.jesse.examination.core.email.service.EmailSenderInterface;
import com.jesse.examination.core.email.utils.EmailFormatVerifier;
import com.jesse.examination.core.exception.ResourceNotFoundException;
import com.jesse.examination.core.properties.ProjectProperties;
import com.jesse.examination.core.redis.exception.ProjectRedisOperatorException;
import com.jesse.examination.core.respponse.ResponseBuilder;
import com.jesse.examination.score.repository.ScoreRecordRepository;
import com.jesse.examination.user.dto.UserLoginDTO;
import com.jesse.examination.user.dto.UserDeleteDTO;
import com.jesse.examination.user.dto.UserModifyDTO;
import com.jesse.examination.user.dto.UserRegistrationDTO;
import com.jesse.examination.user.entity.UserEntity;
import com.jesse.examination.user.exception.DuplicateUserException;
import com.jesse.examination.user.exception.EmptyRequestDataException;
import com.jesse.examination.user.exception.UserArchiveOperatorFailedException;
import com.jesse.examination.user.redis.UserRedisService;
import com.jesse.examination.user.repository.RolesRepository;
import com.jesse.examination.user.repository.UserRepository;
import com.jesse.examination.user.service.UserService;
import com.jesse.examination.user.utils.AuthService;
import com.jesse.examination.user.utils.LoginAuthService;
import com.jesse.examination.user.utils.UserArchiveManager;
import com.jesse.examination.user.utils.dto.AvatarImageData;
import com.jesse.examination.user.utils.exception.UserLoginFailedException;
import io.netty.handler.timeout.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

import static com.jesse.examination.core.email.utils.VerifyCodeGenerator.generateVerifyCode;
import static com.jesse.examination.core.respponse.URLParamPrase.praseRequestParam;
import static com.jesse.examination.user.entity.RoleID.ROLE_USER;
import static java.lang.String.format;

/** 用户模块服务实现类。*/
@Slf4j
@Component
public class UserServiceImpl implements UserService
{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RolesRepository rolesRepository;

    @Autowired
    private ScoreRecordRepository scoreRecordRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private LoginAuthService loginAuthService;

    @Autowired
    private UserArchiveManager userArchiveManager;

    @Autowired
    private UserRedisService userRedisService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Qualifier(value = "createEmailSender")
    private EmailSenderInterface emailSender;

    @Autowired
    private ResponseBuilder responseBuilder;

    @Autowired
    private TransactionalOperator transactionalOperator;

    @Autowired
    private ProjectProperties projectProperties;

    /**
     * <p>在正式将数据写入之前，要对名字进行是否已经存在的校验。</p>
     *
     * <code><pre>
     * Usage:
     *      nameDuplicateCheck("Jesse", userRepository.existByName(name));
     * </pre></code>
     *
     * @param name 用户名或全名
     * @throws DuplicateUserException 当用户名冲突的时候所抛的异常
     */
    private static @NotNull Mono<Void>
    nameDuplicateCheck(String name, @NotNull Mono<Boolean> checker)
    {
        // log.info("Check name: {}", name);
        return checker.flatMap((isDuplicate) ->
        {
            if (isDuplicate)
            {
                throw new DuplicateUserException(
                    format("Name: %s already exist!", name)
                );
            }
            else { return Mono.empty(); }
        });
    }

    /**
     * 用户服务的通用错误处理。
     */
    private @NotNull Mono<ServerResponse>
    genericErrorHandle(@NotNull Throwable exception)
    {
        return switch (exception)
        {
            case IllegalArgumentException illegalArgumentException ->
                this.responseBuilder
                    .BAD_REQUEST(
                        illegalArgumentException.getMessage(),
                        illegalArgumentException
                    );

            case EmptyRequestDataException emptyRequestDataException ->
                this.responseBuilder
                    .BAD_REQUEST(
                        emptyRequestDataException.getMessage(),
                        emptyRequestDataException
                    );

            case ResourceNotFoundException resourceNotFoundException ->
                this.responseBuilder
                    .NOT_FOUND(
                        resourceNotFoundException.getMessage(),
                        resourceNotFoundException
                    );

            case UserLoginFailedException userLoginFailedException ->
                this.responseBuilder
                    .BAD_REQUEST(
                        userLoginFailedException.getMessage(),
                        userLoginFailedException
                    );

            case UserArchiveOperatorFailedException userArchiveOperatorFailedException ->
                this.responseBuilder.BAD_REQUEST(
                    userArchiveOperatorFailedException.getMessage(),
                    userArchiveOperatorFailedException
                );

            case ProjectRedisOperatorException projectRedisOperatorException ->
                this.responseBuilder
                    .INTERNAL_SERVER_ERROR(
                        projectRedisOperatorException.getMessage(),
                        projectRedisOperatorException
                    );

            case DataAccessResourceFailureException dataAccessResourceFailureException ->
                this.responseBuilder
                    .INTERNAL_SERVER_ERROR(
                        dataAccessResourceFailureException.getMessage(),
                        dataAccessResourceFailureException
                    );

            case TimeoutException timeoutException ->
                this.responseBuilder
                    .INTERNAL_SERVER_ERROR(
                        timeoutException.getMessage(),
                        timeoutException
                    );

            case Exception unknowException ->
                this.responseBuilder
                    .INTERNAL_SERVER_ERROR(
                        unknowException.getMessage(),
                        unknowException
                    );

            default ->
                this.responseBuilder
                    .INTERNAL_SERVER_ERROR(
                        "UNKNOW ERROR!",
                        null
                    );
        };
    }

    /**
     * <p>用户注册服务的实现，注册共分为以下几个操作：</p>
     *
     * <ol>
     *     <li>校验从请求体中的用户名和全名是否重复。</li>
     *     <li>创建新用户实体和对应角色并存入数据库。</li>
     *     <li>创建新用户存档。</li>
     *     <li>根据执行中不同的情况返回不同的响应体。</li>
     * </ol>
     *
     * @param request 从前端传来的注册请求
     *
     * @return 返回响应体
     *
     * @throws EmptyRequestDataException          用户的请求体中没有携带任何数据时抛出
     * @throws DuplicateUserException             用户名或全名的验证中出现重复时抛出
     * @throws TimeoutException                   网络通信超时（访问数据库或者其他网络服务）所抛出的异常
     * @throws DataAccessResourceFailureException 获取不到数据库连接时抛出
     * @throws UserArchiveOperatorFailedException 读写用户存档出现问题时抛出的异常
     */
    @Override
    public Mono<ServerResponse>
    userRegister(@NotNull ServerRequest request)
    {
        return request.bodyToMono(UserRegistrationDTO.class)
            .switchIfEmpty(
                Mono.error(new EmptyRequestDataException("Register data not be empty!")))
            .flatMap((registerInfo) ->
            {
                final String userName = registerInfo.getUserName();
                final String fullName = registerInfo.getFullName();

                /* 1. 校验用户名、全名以及邮箱格式是否正确。*/
                Mono<Void> registerInfoChecks
                    = nameDuplicateCheck(
                        userName,
                        this.userRepository
                            .existsByUserName(userName)
                            .map(isExist -> isExist != 0))
                    .and(
                        nameDuplicateCheck(
                            fullName,
                            this.userRepository
                                .existsByFullName(fullName)
                                .map(isExist -> isExist != 0)
                            )
                    )
                    .and(
                        EmailFormatVerifier
                            .isValidEmail(registerInfo.getEmail())
                    );

                /* 2. 创建新用户实体和对应角色并存入数据库。*/
                Mono<Void> createNewUser
                    = Mono.fromCallable(() ->
                        this.passwordEncoder.encode(registerInfo.getPassword()))
                    .subscribeOn(Schedulers.boundedElastic()) // 加密过程非常耗时，丢给线程池去调度
                    .flatMap(
                        (encodedPassword) -> {
                            registerInfo.setPassword(encodedPassword); // 设置完成加密的密码

                            // 正式写入用户和角色的数据
                            return this.userRepository
                                .save(UserEntity.fromUserRegistrationDTO(registerInfo))
                                .flatMap((newUser) ->
                                    this.rolesRepository
                                        .addNewRole(newUser.getUserId(), ROLE_USER.getRoleId())
                                );
                        }).then();

                /* 创建新用户存档。*/
                Mono<Void> createNewUserArchive
                    = this.userArchiveManager
                          .createNewArchiveForNewUser(registerInfo.getUserName());

                /* 将创建新用户的整个动作，包装在一个事务内。*/
                return this.transactionalOperator.transactional(
                    registerInfoChecks
                        .then(createNewUser)
                        .then(createNewUserArchive)
                        .timeout(Duration.ofSeconds(5L))
                        .then(
                            this.responseBuilder.OK(
                                registerInfo,
                                format(
                                    "Register successful! Welcome my new user: %s",
                                    registerInfo.getUserName()
                                ), null, null
                            ))
                        .timeout(Duration.ofSeconds(10L))
                        .onErrorResume(
                            // 注意异常处理的优先级（先处理业务特定异常，再处理通用异常）
                            (exception) -> {
                                if (exception instanceof DuplicateUserException)
                                {
                                    return this.responseBuilder
                                               .BAD_REQUEST(exception.getMessage(), exception);
                                }
                                else if (exception instanceof EmailException)
                                {
                                    return this.responseBuilder
                                               .BAD_REQUEST(exception.getMessage(), exception);
                                }
                                else {
                                    return this.genericErrorHandle(exception);
                                }
                            }
                        )
                );
            }).onErrorResume(this::genericErrorHandle);
    }

    /**
     * <p>用户登录服务的实现，登录主要分为以下几个操作：</p>
     *
     * <ol>
     *     <li>从请求中读取用户登录表单中的数据。</li>
     *     <li>调用登录验证器 {@link LoginAuthService} 逐一比对用户登录数据。</li>
     *     <li>加载对应用户存档。</li>
     *     <li>根据执行中不同的情况返回不同的响应体。</li>
     * </ol>
     *
     * @param request 从前端传来的登录请求
     *
     * @return 返回响应体
     *
     * @throws EmptyRequestDataException          用户的请求体中没有携带任何数据时抛出
     * @throws UserLoginFailedException           登录检查失败时抛出
     * @throws TimeoutException                   网络通信超时（访问数据库或者其他网络服务）所抛出的异常
     * @throws DataAccessResourceFailureException 获取不到数据库连接时抛出
     * @throws UserArchiveOperatorFailedException 读写用户存档出现问题时抛出的异常
     */
    @Override
    public Mono<ServerResponse>
    userLogin(@NotNull ServerRequest request)
    {
        return request.bodyToMono(UserLoginDTO.class)
            .switchIfEmpty(
                Mono.error(new EmptyRequestDataException("Login data not be empty!")))
            .flatMap((loginInfo) ->
                this.loginAuthService.userLoginVerifier(loginInfo)
                    .flatMap((jwt) -> {
                        // 加载用户存档
                        Mono<Void> readArchive
                            = this.userArchiveManager
                            .readUserArchive(loginInfo.getUserName());

                        return this.transactionalOperator.transactional(
                            readArchive.then(
                                this.responseBuilder.OK(
                                    jwt,
                                    format("User: %s login success!", loginInfo.getUserName()),
                                    null, null
                                )
                            )
                        );
                    }))
            .timeout(Duration.ofSeconds(10L))
            .onErrorResume(this::genericErrorHandle);
    }

    /**
     * 用户登出服务。
     * 要做的也很简单，从请求中获取用户名参数，
     * 检查该用户名是否存在后，再进行登出操作（将 Redis 中的用户存档存回文件系统）。
     */
    @Override
    public Mono<ServerResponse>
    userLogout(ServerRequest request)
    {
        return praseRequestParam(request, "name")
                .flatMap((userName) ->
                    this.userRepository.existsByUserName(userName)
                        .map(res -> res != 0)
                        .flatMap((isExist) ->
                            (isExist)
                                ? this.userArchiveManager
                                      .saveUserArchive(userName)
                                      .then(
                                          this.responseBuilder
                                              .OK(null,
                                                  format("User %s log out!", userName),
                                                  null, null
                                              )
                                      )
                                : Mono.error(
                                    new IllegalArgumentException(
                                        format(
                                            "Log out failed! Cause: user %s not exist!",
                                            userName)
                                        )
                                    )
                            ))
            .timeout(Duration.ofSeconds(10L))
            .onErrorResume(this::genericErrorHandle);
    }

    private @NotNull Mono<Void>
    doUserModify(
        @NotNull UserModifyDTO modifyInfo,
        @NotNull UserEntity    oldUserInfo
    )
    {
        String newUserName = modifyInfo.getNewUserName();
        String newFullName = modifyInfo.getNewFullName();

        /* 检查用户名是否重复。*/
        Mono<Void> checkUserName
            = Mono.fromCallable(() -> {
                if (!oldUserInfo.getUserName().equals(newUserName))
                {
                    return nameDuplicateCheck(
                        newUserName,
                        this.userRepository
                            .existsByUserName(newUserName)
                            .map((res) -> res != 0)
                    );
                }

                return Mono.empty();
            }).then();

        /* 检查全名是否重复。*/
        Mono<Void> checkFullName
            = Mono.fromCallable(() -> {
            if (!oldUserInfo.getFullName().equals(newFullName))
            {
                return nameDuplicateCheck(
                    newFullName,
                    this.userRepository
                        .existsByUserName(newFullName)
                        .map((res) -> res != 0)
                );
            }

            return Mono.empty();
        }).then();

        /* 检查密码是否正确。*/
        Mono<Void> checkPassword
            = this.authService.passwordVerifier(
                modifyInfo.getOldPassword(),
                oldUserInfo.getPassword()
            );

        /* 往数据库写入新的用户信息。*/
        Mono<Void> saveUserData
            = this.userRepository
                  .save(
                      UserEntity.fromUserModifier(
                          oldUserInfo, modifyInfo,
                          this.passwordEncoder
                      )
                  ).then();

        /* 存档用户数据。*/
        Mono<Void> saveArchive
            = this.userArchiveManager
            .saveUserArchive(modifyInfo.getOldUserName());

        /* 修改用户存档名。*/
        Mono<Void> renameUserArchive
            = this.userArchiveManager
                  .renameUserArchiveDir(
                      oldUserInfo.getUserName(),
                      modifyInfo.getNewUserName()
                  );

        return checkUserName
                .then(checkFullName)
                .then(checkPassword)
                .then(saveUserData)
                .then(saveArchive)
                .then(renameUserArchive);
    }

    @Override
    public Mono<ServerResponse>
    modifyUserInfo(@NotNull ServerRequest request)
    {
        return request.bodyToMono(UserModifyDTO.class)
            .switchIfEmpty(
                Mono.error(new EmptyRequestDataException("Modify data not be empty!")))
            .flatMap((modifyInfo) ->
                this.userRepository
                    .findUserByUserName(modifyInfo.getOldUserName())
                    .switchIfEmpty(
                        Mono.error(
                            new ResourceNotFoundException(
                                format("User: %s not exist!", modifyInfo.getOldUserName())
                            )
                        )
                    )
                    .flatMap((oldUserInfo) ->
                        this.doUserModify(modifyInfo, oldUserInfo)
                            .then(this.responseBuilder.OK(
                                null,
                                format(
                                    "User modify complete, "      +
                                    "your new user name is: %s, " +
                                    "please login again!", modifyInfo.getNewUserName()
                                ), null, null
                            ))
                    ))
            .timeout(Duration.ofSeconds(10L))
            .onErrorResume(this::genericErrorHandle);
    }

    /**
     * deleteUser() 辅助函数，
     * 在检查到数据库确实存在用户数据时，
     * 正式执行用户的删除操作，共分为以下几个操作：
     *
     * <ol>
     *     <li>检查用户输入的密码是否正确</li>
     *     <li>检查用户输入的验证码是否正确</li>
     *     <li>删除用户的存档（文件系统 + Redis 数据库的数据）</li>
     *     <li>删除数据库中用户的所有成绩、角色和用户数据本身（三个操作放在一个事务内执行）</li>
     * </ol>
     */
    private @NotNull Mono<ServerResponse>
    doUserDelete(@NotNull UserDeleteDTO deleteInfo)
    {
        Mono<Void> checkPassword
            = this.userRepository
            .findUserByUserName(deleteInfo.getUserName())
            .map((user) ->
                this.passwordEncoder
                    .matches(deleteInfo.getPassword(), user.getPassword())
            )
            .flatMap((isMatch) -> {
                if (!isMatch)
                {
                    return Mono.error(
                        new IllegalArgumentException(
                            "Password code mismatch! Please try again!"
                        )
                    );
                }

                return Mono.empty();
            });

        Mono<Void> checkVarifyCode
            = this.userRedisService
                  .getUserVarifyCode(deleteInfo.getUserName())
                  .map((varifyCodeFromRedis) ->
                      varifyCodeFromRedis.equals(deleteInfo.getVarifyCode()))
                  .flatMap((isMatch) -> {
                      if (!isMatch) {
                          return Mono.error(
                              new IllegalArgumentException("Varify code mismatch! Please try again!")
                          );
                      }

                      return Mono.empty();
                  });

        Mono<Void> deleteUserArchive
            = this.userArchiveManager
                  .deleteUserArchive(deleteInfo.getUserName());

        Mono<Void> deleteUserFromDataBase
            = this.userRepository
                  .findIdByUserName(deleteInfo.getUserName())
                  .flatMap((userId) -> {
                      Mono<Void> deleteAllScoreForUser
                          = this.scoreRecordRepository
                                .deleteAllScoreRecordByUserName(userId)
                                .then();

                      Mono<Void> deleteRolesForUser
                          = this.rolesRepository
                                .deleteRolesByUserId(userId)
                                .then();

                      Mono<Void> deleteUser
                          = this.userRepository.deleteById(userId);

                      return this.transactionalOperator
                          .transactional(
                              deleteAllScoreForUser
                                  .then(deleteRolesForUser)
                                  .then(deleteUser)
                          );
                  });

        return checkPassword.then(checkVarifyCode)
            .then(deleteUserArchive)
            .then(deleteUserFromDataBase)
            .then(
                this.responseBuilder.OK(
                    null,
                    format(
                        "Delete user: %s success! Bye!",
                        deleteInfo.getUserName()
                    ), null, null
                )
            )
            .timeout(Duration.ofSeconds(10L))
            .onErrorResume(this::genericErrorHandle);
    }

    @Override
    public Mono<ServerResponse>
    deleteUser(@NotNull ServerRequest request)
    {
        return request.bodyToMono(UserDeleteDTO.class)
                .switchIfEmpty(
                    Mono.error(new EmptyRequestDataException("User delete data not be empty!")))
               .flatMap((deleteInfo) ->
                   this.userRepository
                       .existsByUserName(deleteInfo.getUserName())
                       .map((res) -> res != 0)
                       .flatMap((isExist) ->
                           (isExist)
                                ? doUserDelete(deleteInfo)
                                : this.genericErrorHandle(
                                    new IllegalArgumentException(
                                        format(
                                            "Delete user failed! Cause: user %s not exist!",
                                            deleteInfo.getUserName()
                                       )
                                   )
                           )
                       )
               );
    }

    /**
     * sendVarifyCodeEmail() 的辅助方法，
     * 查询器用户名对应的邮箱。
     *
     * @param userName 解析前端传来的请求体参数获得的用户名
     *
     * @throws IllegalArgumentException  在请求体参数解析失败时抛出
     * @throws ResourceNotFoundException 解析的用户名在数据库中查询不到邮箱时抛出
     *
     * @return 承载了对应邮箱字符串的 Mono
     */
    private @NotNull Mono<String>
    findUserEmail(String userName)
    {
        return this.userRepository
                   .findEmailByUserName(userName)
                   .switchIfEmpty(
                       Mono.error(
                           new ResourceNotFoundException(
                               format("User name: %s not exist!", userName)
                           )
                       )
                   );
    }

    /**
     * sendVarifyCodeEmail() 的辅助方法，
     * 在 findUserEmail() 之后，向指定用户发送验证码邮件。
     *
     * @param userName  用户名
     * @param userEmail 用户邮箱
     *
     * @throws EmailException 在发送邮件操作中出现的任何错误都会抛该异常
     *
     * @return 返回验证码字符串，下游的 Redis 操作需要进行存储。
     */
    private @NotNull Mono<String>
    sendVarifyCodeEmail(String userName, String userEmail)
    {
        return generateVerifyCode(
            Integer.parseInt(
                this.projectProperties.getVarifyCodeLength()))
        .flatMap((varifyCode) ->
        {
            Duration expiration
                = Duration.ofMinutes(
                    Long.parseLong(this.projectProperties.getVarifyCodeExpiration()) / 60
                );

            return this.emailSender
                       .sendEmail(EmailContent.fromVarify(
                           userName, userEmail,
                           varifyCode, expiration))
                       .then(Mono.just(varifyCode));
        });
    }

    /**
     * sendVarifyCodeEmail() 的辅助方法，
     * 在 sendVarifyCodeEmail() 之后，将验证码存入 redis。
     *
     * @param userName   用户名
     * @param varifyCode 上游生成的验证码
     *
     * @throws ProjectRedisOperatorException Redis 操作出错时抛出的异常
     *
     * @return Redis 操作是否成功？作为下游返回何种响应的依据。
     */
    private @NotNull Mono<Boolean>
    saveVarifyCodeToRedis(String userName, String varifyCode)
    {
        return this.userRedisService
                   .saveUserVerifyCode(userName, varifyCode);
    }

    /**
     * 从前端请求中解析用户名，
     * 向该用户发出一封验证码邮件。
     */
    @Override
    public Mono<ServerResponse>
    sendVarifyCodeEmail(ServerRequest request)
    {
        return praseRequestParam(request, "name")
            .flatMap((userName) ->
                this.findUserEmail(userName)
                    .flatMap((userEmail) ->
                        this.sendVarifyCodeEmail(userName, userEmail)
                            .flatMap((varifyCode) ->
                                this.saveVarifyCodeToRedis(userName, varifyCode)
                                    .flatMap((isSuccess) ->
                                        (isSuccess)
                                            ? this.responseBuilder.OK(
                                                null,
                                                format("Send varify code to %s complete!", userEmail),
                                                null, null)
                                            : this.responseBuilder.BAD_REQUEST(
                                                format("Send varify code to %s failed!", userEmail),
                                                null
                                            )
                                    ))
                    )
            ).onErrorResume((exception) -> {
                if (exception instanceof EmailException)
                {
                    return this.responseBuilder
                               .INTERNAL_SERVER_ERROR(exception.getMessage(), exception);
                }
                else {
                    return this.genericErrorHandle(exception);
                }
            });
    }

    /** 从前端请求中解析用户名，获取该用户的头像数据。*/
    @Override
    public Mono<ServerResponse>
    getUserAvatarImage(ServerRequest request)
    {
        return praseRequestParam(request, "name")
            .flatMap((userName) ->
                this.userArchiveManager
                    .getAvatarImageByUserName(userName)
                    .flatMap((avatar) ->
                        ServerResponse.status(HttpStatus.OK)
                                      .headers((headers) ->
                                          headers.setContentType(MediaType.IMAGE_PNG))
                                      .bodyValue(avatar.getAvatarBytes())
                    )
            )
            .onErrorResume(this::genericErrorHandle);
    }

    /**
     * setUserAvatarImage() 的辅助方法，
     * 读取前端传来的的二进制数据，将其映射成 AvatarImageData 后返回。
     *
     * @param buffer 前端传来的二进制数据
     *
     * @return 承载了 AvatarImageData 实例的 Mono
     */
    private @NotNull Mono<AvatarImageData>
    readAvatarFromBinary(@NotNull DataBuffer buffer)
    {
        byte[] bytes = new byte[buffer.readableByteCount()];

        buffer.read(bytes);
        DataBufferUtils.release(buffer);

        return Mono.just(
            AvatarImageData.fromBytes(bytes)
        );
    }

    /**
     * 从前端请求中解析用户名和新头像字节数据，
     * 设置该用户的头像数据。
     */
    @Override
    public Mono<ServerResponse>
    setUserAvatarImage(@NotNull ServerRequest request)
    {
        return request.bodyToMono(DataBuffer.class)
            .switchIfEmpty(
                Mono.error(new EmptyRequestDataException("Avatar image data not be empty!")))
            .flatMap((buffer) ->
                this.readAvatarFromBinary(buffer)
                    .flatMap((avatar) ->
                        praseRequestParam(request, "name")
                            .flatMap((userName) ->
                                this.userRepository.existsByUserName(userName)
                                    .map((res) -> res != 0)
                                    .flatMap((isExist) ->
                                        (isExist)
                                            ? this.userArchiveManager
                                                  .setUserAvatarImage(userName, avatar)
                                                  .then(
                                                    this.responseBuilder.OK(
                                                        null,
                                                        format("Set new avatar for user: %s success!", userName),
                                                        null, null
                                                    )
                                                  )
                                            : Mono.error(
                                                new ResourceNotFoundException(
                                                    format(
                                                        "Set avatar image for user %s failed! Cause: User %s not exist!",
                                                        userName, userName
                                                    )
                                                )
                                            )
                                        )
                                    )
                            )
                        )
            .onErrorResume(this::genericErrorHandle);
    }
}