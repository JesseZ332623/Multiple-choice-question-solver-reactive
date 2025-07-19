package com.jesse.examination.user.entity;

import com.jesse.examination.user.dto.UserRegistrationDTO;
import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/** 用户实体类。*/
@Data
@Accessors(chain = true)
@Table(name = "users")
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class UserEntity
{
    @Id
    @Column(value = "user_id")
    private Long userId;

    @Column(value = "user_name")
    private String userName;

    @Column(value = "password")
    private String password;

    @Column(value = "full_name")
    private String fullName;

    @Column(value = "telephone_number")
    private String telephoneNumber;

    @Column(value = "email")
    private String email;

    @Column(value = "register_datetime")
    private LocalDateTime registerDate;

    public static @NotNull UserEntity
    fromUserRegistrationDTO(@NotNull UserRegistrationDTO userRegistrationDTO)
    {
        UserEntity newUser = new UserEntity();

        newUser.setUserName(userRegistrationDTO.getUserName())
               .setFullName(userRegistrationDTO.getFullName())
               .setPassword(userRegistrationDTO.getPassword())
               .setTelephoneNumber(userRegistrationDTO.getTelephoneNumber())
               .setEmail(userRegistrationDTO.getEmail())
               .setRegisterDate(LocalDateTime.now());

        return newUser;
    }
}
