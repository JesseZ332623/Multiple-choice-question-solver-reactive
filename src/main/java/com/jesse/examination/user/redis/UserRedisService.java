package com.jesse.examination.user.redis;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/** 用户模块 Redis 服务接口类。 */
public interface UserRedisService
{
    /**
     * 某用户登录时，
     * 将从该用户存档中读取所有问题答对次数数据以哈希表的形式存入 Redis。
     *
     * @param userName 用户名
     * @param quesCorrectTimesMap
     *        从 JSON 存档文件中读出，并映射后的哈希表，
     *        映射关系是：问题 ID -> 问题答对次数
     *
     * @return 是否成功存入 Redis?
     */
    Mono<Boolean>
    loadUserQuestionCorrectTimes(
        String userName,
        Map<String, Long> quesCorrectTimesMap
    );

    /**
     * 某用户登出时，
     * 将从 Redis 中读取所有问题答对次数数据哈希表，再存回用户存档中。
     *
     * @param userName 用户名
     *
     * @return 从 Redis 中读取的所有问题答对次数数据哈希表
     */
    Mono<Map<String, Long>>
    getUserQuestionCorrectTimes(String userName);

    /**
     * 用户在登出且数据存回存档后，
     * 需要删除他在 Redis 中的所有信息。
     */
    Mono<Boolean>
    deleteUserInfo(String userName);

    /**
     * 获取所有的用户名。
     *
     * @return 承载了所有用户名的 Flux
     */
    Flux<String>  getAllUsers();

    /** 获取响应式 Redis 模板，
     *  测试时用（生产环境拒绝这种破坏封装性的行为）。
     */
    ReactiveRedisTemplate<String, Object>
    getRedisTemplate();
}
