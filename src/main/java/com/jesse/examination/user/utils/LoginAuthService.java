package com.jesse.examination.user.utils;

import com.jesse.examination.user.dto.UserLoginDTO;
import reactor.core.publisher.Mono;

/** 用户登录时验证器接口。*/
public interface LoginAuthService
{
    /**
     * 用户登录时，
     * 验证用户提交表单的所有数据，并返回 JWT 字符串。
     */
    Mono<String>
    userLoginVarifier(UserLoginDTO userLoginDTO);
}
