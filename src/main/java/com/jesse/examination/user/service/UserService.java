package com.jesse.examination.user.service;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** 用户模块服务接口类。*/
public interface UserService
{
    /** 新用户进行注册服务。*/
    Mono<ServerResponse>
    userRegister(ServerRequest request);

    /** 用户登录服务。*/
    Mono<ServerResponse>
    userLogin(ServerRequest request);

    /** 用户登出服务。 */
    Mono<ServerResponse>
    userLogout(ServerRequest request);

    /** 用户修改账户数据服务（用户在修改前需要验证一次账户）。*/
    Mono<ServerResponse>
    userModifyUserInfo(ServerRequest request);

    /** 用户删除自己账户的服务。*/
    Mono<ServerResponse>
    deleteUser(ServerRequest request);

    /** 通过用户名获取用户头像数据。 */
    Mono<ServerResponse>
    getUserAvatarImage(ServerRequest request);

    /** 设置指定用户的头像数据。*/
    Mono<ServerResponse>
    setUserAvatar(ServerRequest request);
}
