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
public class UserSeviceRouteConfig
{
    @Autowired
    private UserService userService;

    @Bean
    RouterFunction<ServerResponse>
    userRouterFunction()
    {
        return RouterFunctions.route()
                .POST(USER_RIGISTER_URI, this.userService::userRegister)
                .POST(USER_LOGIN_URI, this.userService::userLogin)
                .POST(SEND_VARIFY_EMAIL, this.userService::sendVarifyCodeEmail)
                .build();
    }
}
