package com.jesse.examination.user.repository;

import com.jesse.examination.user.entity.UserRoles;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 用户角色表仓储类。*/
public interface RolesRepository
    extends ReactiveCrudRepository<UserRoles, Long>
{
    /** 通过角色名查询整个角色实体。 */
    @Query("SELECT * FROM user_roles WHERE role_name = :roleName")
    Mono<UserRoles>
    findRoleByRoleName(
        @Param("roleName") String roleName
    );

    /** 查询指定用户的角色（们）。*/
    @Query("""
            SELECT role_name FROM users
            INNER JOIN user_role_relation USING(user_id)
            INNER JOIN user_roles
            ON user_role_relation.role_id = user_roles.id
            WHERE user_id = :userId
        """)
    Flux<String>
    findRolesByUserId(
        @Param("userId") Long userId
    );

    /** 为指定 ID 的用户添加指定 ID 的角色。*/
    @Query("""
        INSERT INTO
            user_role_relation(user_id, role_id)
        VALUES(:userId, :roleId)
        """)
    @Modifying
    Mono<Integer>
    addNewRole(
        @Param("user_id") Long userId,
        @Param("role_id") Long roleId
    );
}
