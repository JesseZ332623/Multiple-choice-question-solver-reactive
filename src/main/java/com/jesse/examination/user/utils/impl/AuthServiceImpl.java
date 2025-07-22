package com.jesse.examination.user.utils.impl;

import com.jesse.examination.user.redis.UserRedisService;
import com.jesse.examination.user.utils.AuthService;
import com.jesse.examination.user.utils.exception.PasswordMissmatchException;
import com.jesse.examination.user.utils.exception.VarifyCodeMismatchException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;

import static java.lang.String.format;

/** 通用用户数据校验器实现类。*/
@Slf4j
@Component
final public class AuthServiceImpl implements AuthService
{
    @Autowired
    private UserRedisService userRedisService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public @NotNull Mono<Void>
    passwordVerifier(String rawPassword, String encodedPassword)
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

    public @NotNull Mono<Void>
    verifyCodeCheck(String userName, String varifyCodeFromInput)
    {
        return this.userRedisService.getUserVarifyCode(userName)
            .switchIfEmpty(
                Mono.error(
                    new VarifyCodeMismatchException(
                        format("Verify code of user: %s not exist or expired!", userName)
                    )
                )
            )
            .flatMap((varifyCode) ->
                (!varifyCode.equals(varifyCodeFromInput))
                    ? Mono.error(
                    new VarifyCodeMismatchException("Incorrect verify code! Please try again!"))
                    : this.userRedisService
                    .deleteUserVerifyCode(userName)
                    .then()
            );
    }

    public @NotNull Mono<Void>
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
}
