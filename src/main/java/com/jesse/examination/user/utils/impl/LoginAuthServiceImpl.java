package com.jesse.examination.user.utils.impl;

import com.jesse.examination.core.exception.ResourceNotFoundException;
import com.jesse.examination.core.properties.ProjectProperties;
import com.jesse.examination.user.dto.UserLoginDTO;
import com.jesse.examination.user.redis.UserRedisService;
import com.jesse.examination.user.utils.LoginAuthService;
import com.jesse.examination.user.utils.exception.PasswordMissmatchException;
import com.jesse.examination.user.utils.exception.UserLoginFailedException;
import com.jesse.examination.user.utils.exception.VarifyCodeMismatchException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

/** 用户登录时验证器实现。*/
@Slf4j
@Component
public class LoginAuthServiceImpl implements LoginAuthService
{
    @Autowired
    private ReactiveUserDetailsService userDetailsService;

    @Autowired
    private UserRedisService userRedisService;

    @Autowired
    private ProjectProperties projectProperties;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Mono<String>
    userLoginVarifier(@NotNull UserLoginDTO userLoginDTO)
    {
        Mono<Void> checkVarifyCode
            = this.varifyCodeCheck(
                userLoginDTO.getUserName(), userLoginDTO.getVarifyCode()
            );

        Mono<UserDetails> getUserDetails
            = this.userDetailsService
            .findByUsername(userLoginDTO.getUserName())
            .onErrorResume((exception) ->
                Mono.error(new UserLoginFailedException(
                    format(
                        "User: %s login failed! Cause: %s",
                        userLoginDTO.getUserName(), exception.getMessage()
                    ))
                )
            );

        Mono<String> checkPasswordAndGenerateJWTToken
            = getUserDetails.flatMap((user) -> {
                      Mono<Void> checkPassword
                          = this.passwordVarifier(
                              userLoginDTO.getPassword(), user.getPassword()
                          );

                      Mono<String> generateJWTToken
                          = this.generateJWTToken(user);

                      return checkPassword.then(generateJWTToken);
                  });

        return checkVarifyCode.then(checkPasswordAndGenerateJWTToken)
                    .timeout(Duration.ofSeconds(5L))
                    .onErrorResume((exception) -> {
                        log.error(
                            "User: {} login failed! Cause: {}",
                            userLoginDTO.getUserName(),
                            exception.getMessage(), exception
                        );

                        return Mono.error(new UserLoginFailedException(
                            format(
                                "User: %s login failed! Cause: %s",
                                userLoginDTO.getUserName(), exception.getMessage()
                            )
                        ));
                    });
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
            .switchIfEmpty(
                Mono.error(
                    new VarifyCodeMismatchException(
                        format("Varify code of user: %s not exist or expired!", userName)
                    )
                )
            )
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
    private Mono<String>
    generateJWTToken(@NotNull UserDetails user)
    {
        SecretKey key
            = Keys.hmacShaKeyFor(projectProperties.getJwtSecretKey().getBytes());

        long expirationSeconds =
            Long.parseLong(this.projectProperties.getJwtExpiration());

       List<String> claims =
            user.getAuthorities()
                .stream()
                .map(a ->
                    a.getAuthority().replaceFirst("^ROLE_", ""))
                .collect(Collectors.toList());

        return Mono.just(
                Jwts.builder()
                    .subject(user.getUsername())
                    .claim("roles", claims)
                    .issuedAt(Date.from(Instant.now()))
                    .issuer("JesseZ332623")
                    .expiration(Date.from(Instant.now().plusSeconds(expirationSeconds)))
                    .signWith(key)
                    .compact()
        );
    }
}
