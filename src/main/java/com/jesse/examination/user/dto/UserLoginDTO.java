package com.jesse.examination.user.dto;

import lombok.*;
import lombok.experimental.Accessors;

/** 用户登录时所用的 DTO。*/
@Data
@ToString
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode
public class UserLoginDTO
{
    private String userName;

    private String password;

    private String verifyCode;

    public static UserLoginDTO
    of (String name, String pass, String verify)
    {
        return new UserLoginDTO()
            .setUserName(name)
            .setPassword(pass)
            .setVerifyCode(verify);
    }
}
