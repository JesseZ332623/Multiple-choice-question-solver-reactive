package com.jesse.examination.user.repository;

import com.jesse.examination.user.entity.UserEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository
    extends ReactiveCrudRepository<UserEntity, Long>
{
    /** 根据用户 ID 查询对应的用户名。 */
    @Query("""
            SELECT user_id FROM users
            WHERE user_name = :userName
        """)
    Mono<Long>
    findIdByUserName(
        @Param("userName") String userName
    );
}
