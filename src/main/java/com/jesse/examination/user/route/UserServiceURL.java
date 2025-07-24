package com.jesse.examination.user.route;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** 用户模块 URI 配置类。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class UserServiceURL
{
    private static final String USER_SERVICE_ROOT_URI
        = "/api/user";

    public static final String USER_REGISTER_URI
        = USER_SERVICE_ROOT_URI + "/register";

    public static final String USER_LOGIN_URI
        = USER_SERVICE_ROOT_URI + "/login";

    public static final String USER_LOGOUT_URI
        = USER_SERVICE_ROOT_URI + "/logout";

    public static final String USER_MODIFY_URI
        = USER_SERVICE_ROOT_URI + "/modify";

    public static final String USER_DELETE_URI
        = USER_SERVICE_ROOT_URI + "/delete";

    public static final String SEND_VARIFY_EMAIL
        = USER_SERVICE_ROOT_URI + "/send_verify_code_email";

    public static final String GET_AVATAR_IMAGE
        = USER_SERVICE_ROOT_URI + "/get_avatar";

    public static final String SET_AVTAR_IMAGE
        = USER_SERVICE_ROOT_URI + "/set_avatar";
}
