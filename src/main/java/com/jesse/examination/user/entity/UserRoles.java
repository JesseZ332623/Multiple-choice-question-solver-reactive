package com.jesse.examination.user.entity;

import lombok.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 用户角色实体类。*/
@Data
@Table(name = "user_roles")
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
public class UserRoles
{
    @Column("id")
    private @NonNull Long roleId;

    @Column("role_name")
    private @NonNull String roleName;
}
