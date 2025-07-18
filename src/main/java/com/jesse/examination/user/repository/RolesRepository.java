package com.jesse.examination.user.repository;

import com.jesse.examination.user.entity.UserRoles;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/** 用户角色表仓储类。*/
public interface RolesRepository
    extends ReactiveCrudRepository<UserRoles, Long>
{
    @Query("SELECT * FROM user_roles WHERE role_name = :roleName")
    Mono<UserRoles>
    findRoleByRoleName(
        @Param("roleName") String roleName
    );
}
