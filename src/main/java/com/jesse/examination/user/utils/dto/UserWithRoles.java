package com.jesse.examination.user.utils.dto;

import com.jesse.examination.user.entity.UserEntity;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Set;

/** 带角色数据的用户 DTO。*/
@Data
@Accessors(chain = true)
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserWithRoles
{
    private Long userId;

    private String userName;

    private String password;

    private String fullName;

    private String telephoneNumber;

    private String email;

    private LocalDateTime registerDate;

    private Set<String> roles;

    public static @NotNull UserWithRoles
    fromUserEntity(@NotNull UserEntity user, Set<String> roles)
    {
        UserWithRoles userWithRoles = new UserWithRoles();

        userWithRoles.setUserId(user.getUserId())
                     .setUserName(user.getUserName())
                     .setPassword(user.getPassword())
                     .setFullName(user.getFullName())
                     .setTelephoneNumber(user.getTelephoneNumber())
                     .setEmail(user.getEmail())
                     .setRegisterDate(user.getRegisterDate())
                     .setRoles(roles);

        return userWithRoles;
    }
}
