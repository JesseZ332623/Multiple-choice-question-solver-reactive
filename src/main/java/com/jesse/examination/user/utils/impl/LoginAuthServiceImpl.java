package com.jesse.examination.user.utils.impl;

import com.jesse.examination.core.properties.ProjectProperties;
import com.jesse.examination.user.dto.UserLoginDTO;
import com.jesse.examination.user.redis.UserRedisService;
import com.jesse.examination.user.repository.RolesRepository;
import com.jesse.examination.user.repository.UserRepository;
import com.jesse.examination.user.utils.LoginAuthService;
import com.jesse.examination.user.utils.dto.UserWithRoles;
import com.jesse.examination.user.utils.exception.PasswordMissmatchException;
import com.jesse.examination.user.utils.exception.UserLoginFailedException;
import com.jesse.examination.user.utils.exception.VarifyCodeMismatchException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

/** 用户登录时验证器实现。*/
@Slf4j
@Component
public class LoginAuthServiceImpl implements LoginAuthService
{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RolesRepository rolesRepository;

    @Autowired
    private UserRedisService userRedisService;

    @Autowired
    private ProjectProperties projectProperties;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionalOperator transactionalOperator;

    @Override
    public Mono<String>
    userLoginVarifier(@NotNull UserLoginDTO userLoginDTO)
    {
        log.info(userLoginDTO.toString());

        Mono<Void> checkVarifyCode
            = this.varifyCodeCheck(userLoginDTO.getUserName(), userLoginDTO.getVarifyCode())
            .onErrorResume((exception) -> {
                log.error("{}", exception.getMessage());
                return Mono.error(exception);
            });

        Mono<String> checkPasswordAndGenerateJWTToken
            = this.userRepository
                  .findUserByUserName(userLoginDTO.getUserName())
                  .flatMap((user) -> {
                      Mono<Void> checkPassword
                        = this.passwordVarifier(userLoginDTO.getPassword(), user.getPassword());

                      Mono<String> generateJWTToken
                          = this.rolesRepository
                                .findRolesByUserId(user.getUserId())
                                .collect(Collectors.toSet())
                                .map((roles) ->
                                    this.generateJWTToken(UserWithRoles.fromUserEntity(user, roles))
                                );

                      return checkPassword.then(generateJWTToken);
                  }).onErrorResume(Mono::error);

        return transactionalOperator.transactional(
                    checkVarifyCode.then(checkPasswordAndGenerateJWTToken))
                    .timeout(Duration.ofSeconds(5L))
                    .doOnSuccess(ignore ->
                        log.info("User: {} login success!", userLoginDTO.getUserName()))
                    .doOnError(exception ->
                        log.error("Login failed! Cause: {}", exception.getMessage(), exception))
                    .onErrorResume((exception) ->
                        Mono.error(new UserLoginFailedException(
                            format(
                                "User: %s login failed! Cause: %s",
                                userLoginDTO.getUserName(), exception.getMessage()
                            )
                        ))
                    );
    }

    private @NotNull Mono<Void>
    passwordVarifier(String rawPassword, String encodedPassword)
    {
        return Mono.fromRunnable(
            () -> {
                if (!this.passwordEncoder.matches(rawPassword, encodedPassword))
                {
                    throw new PasswordMissmatchException(
                        "Incorrect password! Please try again!"
                    );
                }
            }
        );
    }

    private @NotNull Mono<Void>
    varifyCodeCheck(String userName, String varifyCodeFromInput)
    {
        return this.userRedisService.getUserVarifyCode(userName)
            .flatMap((varifyCode) -> {
                log.info(
                    "Varify code from input: {}, varify code from redis: {}",
                    varifyCodeFromInput, varifyCode
                );

                return (!varifyCode.equals(varifyCodeFromInput))
                    ? Mono.error(new VarifyCodeMismatchException("Incorrect varify code! Please try again!"))
                    : this.userRedisService.deleteUserVarifyCode(userName).then();
            });
    }

    private @NotNull Mono<Void>
    roleVarifier(String userName, String targetRoleName, Set<String> roles)
    {
        return Mono.fromRunnable(
            () -> {
                boolean isAdmin = false;

                for (String role : roles)
                {
                    if (role.equals(targetRoleName))
                    {
                        isAdmin = true;
                        break;
                    }
                }

                if (!isAdmin)
                {
                    throw new InsufficientAuthenticationException(
                        format("User: %s are not admin! Login request rejected!", userName)
                    );
                }
            }
        );
    }

    /**
     * 当其他验证都完毕后，
     * 为这个登录用户生成一个 JWT。
     */
    private String
    generateJWTToken(@NotNull UserWithRoles user)
    {
        SecretKey key
            = Keys.hmacShaKeyFor(projectProperties.getJwtSecretKey().getBytes());

        long expiration =
            Long.parseLong(this.projectProperties.getJwtExpiration()) * 1000L;

        Map<String, Set<String>> claims = new HashMap<>();
        claims.put("roles", user.getRoles());

        return Jwts.builder()
                   .subject(user.getUserName())
                   .claims(claims)
                   .issuedAt(Date.from(Instant.now()))
                   .expiration(Date.from(Instant.now().plusMillis(expiration)))
                   .signWith(key)
                   .compact();
    }
}
