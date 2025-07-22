package com.jesse.examination.user.utils;

import reactor.core.publisher.Mono;

import java.util.Set;

/** 通用用户数据校验器接口。*/
public interface AuthService
{
    /** 密码验证。*/
    Mono<Void>
    passwordVerifier(String rawPassword, String encodedPassword);

    /** 验证码验证。*/
    Mono<Void>
    verifyCodeCheck(String userName, String varifyCodeFromInput);

    /** 用户角色验证。*/
    Mono<Void>
    roleVarifier(String userName, String targetRoleName, Set<String> roles);
}
