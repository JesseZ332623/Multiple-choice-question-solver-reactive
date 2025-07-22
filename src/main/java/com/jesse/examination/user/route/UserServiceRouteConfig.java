package com.jesse.examination.user.route;

import com.jesse.examination.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.jesse.examination.user.route.UserServiceURL.*;

/** 用户模块路由函数配置类。*/
@Configuration
public class UserServiceRouteConfig
{
    @Autowired
    private UserService userService;

    @Bean
    RouterFunction<ServerResponse>
    userRouterFunction()
    {
        return RouterFunctions.route()
                .GET(GET_AVATAR_IMAGE,   this.userService::getUserAvatarImage)
                .POST(USER_REGISTER_URI, this.userService::userRegister)
                .POST(USER_LOGIN_URI,    this.userService::userLogin)
                .POST(SEND_VARIFY_EMAIL, this.userService::sendVarifyCodeEmail)
                .PUT(SET_AVTAR_IMAGE,    this.userService::setUserAvatarImage)
                .PUT(USER_MODIFY_URI,    this.userService::modifyUserInfo)
                .DELETE(USER_DELETE_URI, this.userService::deleteUser)
                .build();
    }
}
