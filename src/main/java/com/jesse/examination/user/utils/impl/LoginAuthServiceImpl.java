package com.jesse.examination.user.utils.impl;

import com.jesse.examination.core.properties.ProjectProperties;
import com.jesse.examination.user.dto.UserLoginDTO;
import com.jesse.examination.user.utils.LoginAuthService;
import com.jesse.examination.user.utils.exception.UserLoginFailedException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
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
    private AuthServiceImpl authService;

    @Autowired
    private ProjectProperties projectProperties;

    @Override
    public Mono<String>
    userLoginVerifier(@NotNull UserLoginDTO userLoginDTO)
    {
        Mono<Void> checkVarifyCode
            = this.authService.verifyCodeCheck(
                userLoginDTO.getUserName(), userLoginDTO.getVerifyCode()
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
                          = this.authService.passwordVerifier(
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

    /**
     * 当其他验证都完毕后，
     * 为这个登录用户生成一个 JWT。
     */
    private @NotNull Mono<String>
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
