package com.jesse.examination.user.service.impl;

import com.jesse.examination.user.service.UserService;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** 用户模块服务实现类。*/
public class UserServiceImpl implements UserService
{
    @Override
    public Mono<ServerResponse>
    userRegister(ServerRequest request) {
        return null;
    }

    @Override
    public Mono<ServerResponse>
    userLogin(ServerRequest request) {
        return null;
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
    getUserAvatarImage(ServerRequest request) {
        return null;
    }

    @Override
    public Mono<ServerResponse>
    setUserAvatar(ServerRequest request) {
        return null;
    }
}