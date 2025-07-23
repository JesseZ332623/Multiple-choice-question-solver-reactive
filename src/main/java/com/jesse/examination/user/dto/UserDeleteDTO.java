package com.jesse.examination.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/** 用户删除操作所用的 DTO。*/
@Data
@ToString
@Accessors(chain = true)
@NoArgsConstructor
public class UserDeleteDTO
{
    private String userName;

    private String password;

    private String varifyCode;

    public static UserDeleteDTO
    of (String name, String pass, String verify)
    {
        UserDeleteDTO userDelete
            = new UserDeleteDTO();

        return userDelete.setUserName(name)
                    .setPassword(pass)
                    .setVarifyCode(verify);
    }
}
