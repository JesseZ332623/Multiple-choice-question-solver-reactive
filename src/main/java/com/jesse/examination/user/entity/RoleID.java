package com.jesse.examination.user.entity;

import lombok.Getter;

/** 角色表 ID 枚举类。*/
public enum RoleID
{
    ROLE_ADMIN(1), ROLE_USER(2);

    @Getter
    final long roleId;

    RoleID(int id) { this.roleId = id; }
}
