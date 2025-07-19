package com.jesse.examination.user.route;

/** 用户模块 URI 配置类。*/
public class UserServiceURL
{
    private static final String USER_SERVICE_ROOT_URI
        = "/api/user";

    public static final String USER_RIGISTER_URI
        = USER_SERVICE_ROOT_URI + "/register";

    public static final String USER_LOGIN_URI
        = USER_SERVICE_ROOT_URI + "/login";

    public static final String SEND_VARIFY_EMAIL
        = USER_SERVICE_ROOT_URI + "/send_verify_code_email";
}
