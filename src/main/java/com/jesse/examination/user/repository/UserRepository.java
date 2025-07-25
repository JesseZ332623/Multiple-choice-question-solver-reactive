package com.jesse.examination.user.repository;

import com.jesse.examination.user.entity.UserEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 用户数据表仓储类。*/
public interface UserRepository
    extends ReactiveCrudRepository<UserEntity, Long>
{
    /** 返回表中用户最大的 ID。*/
    @Query(" SELECT MAX(user_id) FROM users")
    Mono<Long> findMaxUserId();

    /** 返回表中所有的用户 ID。*/
    @Query("SELECT user_id FROM users")
    Flux<Long> findAllIds();

    @Query("SELECT user_name FROM users")
    Flux<String> findAllUserName();

    /** 根据用户名查询对应的用户 ID。*/
    @Query("""
            SELECT user_id FROM users
            WHERE user_name = :userName
        """)
    Mono<Long>
    findIdByUserName(
        @Param("userName") String userName
    );

    /** 根据用户名查询对应的用户邮箱。*/
    @Query("""
            SELECT email FROM users
            WHERE user_name = :userName
        """)
    Mono<String>
    findEmailByUserName(
        @Param("userName") String userName
    );

    /** 根据用户名查询整个用户实体。*/
    @Query("""
            SELECT * FROM users
            WHERE user_name = :userName
        """)
    Mono<UserEntity>
    findUserByUserName(
        @Param("userName") String userName
    );

    /** 检查指定用户名是否存在。*/
    @Query("""
            SELECT EXISTS(
                SELECT 1 FROM users
                WHERE user_name = :userName
            )
        """)
    Mono<Integer>
    existsByUserName(
        @Param("userName") String userName
    );

    /** 检查指定用户全名是否存在。*/
    @Query("""
            SELECT EXISTS(
                SELECT 1 FROM users
                WHERE full_name = :fullName
            )
        """)
    Mono<Integer>
    existsByFullName(
        @Param("fallName") String fullName
    );

    /**
     * 通过用户名删除指定的用户数据。
     * 需要注意的是，由于外键的存在，
     * 删除用户数据的操作分为 3 个部分并放在一个事务内执行，如下所式：
     *
     * <li>删除该用户的所有成绩记录</li>
     * <li>删除该用户的所有角色</li>
     * <li>删除用户本体数据</li>
     */
    @Modifying
    @Query("""
            DELETE FROM users
            WHERE user_name = :userName
        """)
    Mono<Void>
    deleteUserByUserName(
        @Param("userName") String userName
    );

    /** 将用户表的 id 自增重设为 1。（一般是测试结束后用）*/
    @Modifying
    @Query("ALTER TABLE users AUTO_INCREMENT = 1")
    Mono<Void> resumeAutoIncrement();
}
