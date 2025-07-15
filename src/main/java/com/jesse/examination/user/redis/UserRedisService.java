package com.jesse.examination.user.redis;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/** 用户模块 Redis 服务接口类。 */
public interface UserRedisService
{
    /**
     * <p>
     *     用户登录时，发送验证码邮件之后，存储验证码到 Redis。
     *     键值对格式如下：
     * </p>
     *
     * <code><pre>
     * [Key]    user:[userName]:varify-code
     * [Value]  (Numeric String)
     * </pre></code>
     *
     * @param userName   用户名
     * @param varifyCode 验证码
     *
     * @return 是否成功存入 Redis?
     */
    Mono<Boolean>
    saveUserVarifyCode(String userName, String varifyCode);

    /**
     * <p>
     *     某用户登录时，
     *     将从该用户存档中读取所有问题答对次数数据以哈希表的形式存入 Redis。
     *     键值对格式如下：
     * </p>
     *
     * <code><pre>
     * [Key]    user:[userName]:ques-correct-times
     * [Value]  (Hash Map)
     * </pre></code>
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
     * <p>
     *     读取指定用户的验证码。
     * </p>
     *
     * <code><pre>
     * [Key]    user:[userName]:varify-code
     * </pre></code>
     *
     * @param userName   用户名
     *
     * @return 承载了指定用户验证码数据的 Mono
     */
    Mono<String>
    getUserVarifyCode(String userName);

    /**
     * <p>
     *     某用户登出时，
     *     将从 Redis 中读取所有问题答对次数数据哈希表，再存回用户存档中。
     *     键格式如下：
     * </p>
     *
     * <code><pre>
     * [Key]    user:[userName]:ques-correct-times
     * </pre></code>
     *
     * @param userName 用户名
     *
     * @return 从 Redis 中读取的所有问题答对次数数据哈希表
     */
    Mono<Map<String, Long>>
    getUserQuestionCorrectTimes(String userName);

    /**
     * 将某个用户下的所有数据删除。
     *
     * @param userName 用户名
     *
     * @return 是否删除成功？
     */
    Mono<Boolean>
    deleteUserInfo(String userName);

    /**
     * 获取所有的用户名。
     *
     * @return 承载了所有用户名的 Flux
     */
    Flux<String>  getAllUsers();
}
