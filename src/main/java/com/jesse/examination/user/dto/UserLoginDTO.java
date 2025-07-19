package com.jesse.examination.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

/** 用户登录时所用的 DTO。*/
@Data
@ToString
@Accessors(chain = true)
@EqualsAndHashCode
@AllArgsConstructor
public class UserLoginDTO
{
    private String userName;

    private String password;

    private String varifyCode;
}
