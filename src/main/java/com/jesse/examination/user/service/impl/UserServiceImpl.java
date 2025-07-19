package com.jesse.examination.user.service.impl;

import com.jesse.examination.core.email.dto.EmailContent;
import com.jesse.examination.core.email.service.EmailSenderInterface;
import com.jesse.examination.core.properties.ProjectProperties;
import com.jesse.examination.core.respponse.ResponseBuilder;
import com.jesse.examination.user.dto.UserLoginDTO;
import com.jesse.examination.user.dto.UserRegistrationDTO;
import com.jesse.examination.user.entity.UserEntity;
import com.jesse.examination.user.exception.DuplicateUserException;
import com.jesse.examination.user.exception.EmptyRequestDataException;
import com.jesse.examination.user.redis.UserRedisService;
import com.jesse.examination.user.repository.RolesRepository;
import com.jesse.examination.user.repository.UserRepository;
import com.jesse.examination.user.service.UserService;
import com.jesse.examination.user.utils.LoginAuthService;
import com.jesse.examination.user.utils.UserArchiveManager;
import com.jesse.examination.user.utils.exception.UserLoginFailedException;
import io.netty.handler.timeout.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

import static com.jesse.examination.core.email.utils.VarifyCodeGenerator.generateVarifyCode;
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
     * Usage 1. nameDuplicateCheck("Jesse", userRepository::existsByUsername);
     * Usage 2. nameDuplicateCheck("Peter-Griffin", userRepository::existsByFullName);
     * </pre></code>
     *
     * @param name 用户名或全名
     *
     * @throws DuplicateUserException
     *         当用户名冲突的时候所抛的异常
     */
    private static @NotNull Mono<Void>
    nameDuplicateCheck(String name, @NotNull Mono<Boolean> checker)
    {
        log.info("Ckeck name: {}", name);

        return checker.flatMap((isDuplicate) -> {
            if (isDuplicate) {
                throw new DuplicateUserException(
                    format("Name: %s aleardy exist!", name)
                );
            }
            else { return Mono.empty(); }
        });
    }

    /** 用户服务的通用错误处理。*/
    private @NotNull Mono<ServerResponse>
    genericErrorHandle(@NotNull Mono<ServerResponse> mono)
    {
        return mono.onErrorResume(
            EmptyRequestDataException.class,
                (exception) ->
                this.responseBuilder.BAD_REQUEST(exception.getMessage(), exception))
            .onErrorResume(
                TimeoutException.class,
                (exception) ->
                    this.responseBuilder.INTERNAL_SERVER_ERROR(exception.getMessage(), exception))
            .onErrorResume(DataAccessResourceFailureException.class,
                (exception) ->
                    this.responseBuilder.INTERNAL_SERVER_ERROR(exception.getMessage(), exception))
            .onErrorResume(
                Exception.class,
                (exception) ->
                    this.responseBuilder.INTERNAL_SERVER_ERROR(exception.getMessage(), exception)
            );
    }

    @Override
    public Mono<ServerResponse>
    userRegister(@NotNull ServerRequest request)
    {
        return this.genericErrorHandle(
            request.bodyToMono(UserRegistrationDTO.class)
                   .switchIfEmpty(
                       Mono.error(new EmptyRequestDataException("Register data not be null!")))
                   .flatMap((registInfo) ->
                   {
                       final String userName = registInfo.getUserName();
                       final String fullName = registInfo.getFullName();

                       /* 1. 校验用户名与全名。*/
                       Mono<Void> nameChecks
                           = nameDuplicateCheck(
                               userName,
                               this.userRepository
                                   .existsByUserName(userName)
                                   .map(isExist -> isExist != 0))
                               .and(nameDuplicateCheck(
                                   fullName,
                                   this.userRepository.existsByFullName(fullName)
                                       .map(isExist -> isExist != 0)
                                   )
                               );

                       /* 2. 创建新用户实体并存入数据库。*/
                       Mono<Void> createNewUser
                           = Mono.fromCallable(() ->
                               this.passwordEncoder.encode(registInfo.getPassword()))
                                   .subscribeOn(Schedulers.boundedElastic()) // 加密过程非常耗时，丢给别的线程去干
                                   .flatMap(
                                       (encodedPassword) -> {
                                           registInfo.setPassword(encodedPassword);

                                           return this.userRepository.save(UserEntity.fromUserRegistrationDTO(registInfo))
                                                       .flatMap((newUser) ->
                                                           this.rolesRepository
                                                               .addNewRole(newUser.getUserId(), ROLE_USER.getRoleId())
                                                       );
                                               }).then();

                       /* 创建新用户存档。*/
                       Mono<Void> createNewUserArchive
                           = this.userArchiveManager
                                 .createNewArchiveForNewUser(registInfo.getUserName());

                       /* 将创建新用户的整个动作，包装在一个事务内。*/
                       return this.transactionalOperator.transactional(
                           nameChecks.then(createNewUser)
                                     .then(createNewUserArchive)
                                     .timeout(Duration.ofSeconds(5L))
                                     .then(
                                         this.responseBuilder.OK(
                                             registInfo,
                                             format(
                                                 "Registe successful! Welcome my new user: %s",
                                                 registInfo.getUserName()
                                             ),
                                             null, null
                                         )
                                   )
                   ).onErrorResume(
                       DuplicateUserException.class,
                       (exception) ->
                           this.responseBuilder.BAD_REQUEST(exception.getMessage(), exception)
                   );
               })
        );
    }

    @Override
    public Mono<ServerResponse>
    userLogin(@NotNull ServerRequest request)
    {
        return this.genericErrorHandle(
            request.bodyToMono(UserLoginDTO.class)
                    .switchIfEmpty(
                        Mono.error(new EmptyRequestDataException("Login data not be null!")))
                   .flatMap((loginInfo) ->
                       this.loginAuthService.userLoginVarifier(loginInfo)   // 用户表单验证，成功后返回 Token
                           .flatMap((jwt) ->
                               this.userArchiveManager  // 加载用户存档，成功后返回 OK 响应
                                   .readUserArchive(loginInfo.getUserName())
                                   .then(
                                       this.responseBuilder.OK(
                                           jwt, format("User: %s login success!", loginInfo.getUserName()),
                                           null, null
                                  )
                               )
                           )
                      ).onErrorResume(
                          UserLoginFailedException.class,
                         (exception) ->
                             this.responseBuilder.BAD_REQUEST(exception.getMessage(), exception)
                      )
        );
    }

    @Override
    public Mono<ServerResponse>
    userLogout(ServerRequest request) {
        return null;
    }

    @Override
    public Mono<ServerResponse>
    userModifyUserInfo(ServerRequest request) {
        return null;
    }

    @Override
    public Mono<ServerResponse>
    deleteUser(ServerRequest request) {
        return null;
    }

    @Override
    public Mono<ServerResponse>
    sendVarifyCodeEmail(ServerRequest request)
    {
        return this.genericErrorHandle(
            generateVarifyCode(
                Integer.parseInt(this.projectProperties.getVarifyCodeLength())
        ).flatMap((varifyCode) ->
            praseRequestParam(request, "name")
                .flatMap((userName) ->
                    this.userRepository.findEmailByUserName(userName)
                        .flatMap((userEmail) ->
                        {
                            Duration expiration
                                = Duration.ofMinutes(
                                    Long.parseLong(
                                        this.projectProperties.getVarifyCodeExpiration()) / 60
                            );

                            return this.emailSender.sendEmail(
                                EmailContent.fromVarify(
                                    userName, userEmail,
                                    varifyCode, expiration
                                )
                            ).flatMap((sendReport) -> {
                                if (sendReport.getKey())
                                {
                                    return this.userRedisService
                                               .saveUserVarifyCode(userName, varifyCode)
                                               .flatMap((isSuccess) -> {
                                                   if (!isSuccess)
                                                   {
                                                       return this.responseBuilder.INTERNAL_SERVER_ERROR(
                                                           "Save varify code to redis failed!", null
                                                       );
                                                   }
                                                   else
                                                   {
                                                       return this.responseBuilder.OK(
                                                           null, sendReport.getValue(),
                                                           null, null
                                                       );
                                                   }
                                               });
                                }
                                else
                                {
                                    return this.responseBuilder
                                               .BAD_REQUEST(sendReport.getValue(), null);
                                }
                            });
                        })
                ).onErrorResume(
                    IllegalArgumentException.class,
                    (exception) ->
                        this.responseBuilder.BAD_REQUEST(exception.getMessage(), exception)
                )
            )
        );
    }

    @Override
    public Mono<ServerResponse>
    getUserAvatarImage(ServerRequest request) {
        return null;
    }

    @Override
    public Mono<ServerResponse>
    setUserAvatarImage(ServerRequest request) {
        return null;
    }
}