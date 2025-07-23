package com.jesse.examination.user.dto;

import lombok.*;
import lombok.experimental.Accessors;

/** 用户修改自己账户信息时所用的 DTO。*/
@Data
@ToString
@Accessors(chain = true)
@EqualsAndHashCode
@NoArgsConstructor
public class UserModifyDTO
{
    private String oldUserName;            // 旧用户名

    private String newUserName;            // 新用户名
    
    private String oldPassword;            // 旧密码

    private String newPassword;            // 新用户密码

    private String newFullName;            // 新用户全名

    private String newTelephoneNumber;     // 新手机号

    private String newEmail;               // 新邮箱
}
