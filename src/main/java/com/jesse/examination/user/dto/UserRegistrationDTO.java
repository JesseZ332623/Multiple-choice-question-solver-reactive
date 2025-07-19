package com.jesse.examination.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/** 用户注册时所用的 DTO。*/
@Data
@ToString
@Accessors(chain = true)
@EqualsAndHashCode
@AllArgsConstructor
public class UserRegistrationDTO
{
    private String userName;            // 用户名

    private String password;            // 用户密码

    private String fullName;            // 用户全名

    private String telephoneNumber;     // 手机号

    private String email;               // 邮箱
}
