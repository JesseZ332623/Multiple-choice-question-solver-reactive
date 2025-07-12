package com.jesse.examination.question.redis;

import reactor.core.publisher.Mono;

import java.util.Map;

/** 问题数据统计 Redis 服务接口。 */
public interface QuestionRedisService
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
    * 用户在练习时答对了一道题，这题的答对次数 + 1。
    *
    * @param userName   用户名
    * @param questionId 问题 ID
    *
    * @return 增加后该用户本题的答对次数
    */
   Mono<Long>
   incrementUserQuestionCorrectTime(
        String userName, Long questionId
   );

    /**
     * 用户在登出且数据存回存档后，
     * 需要删除他在 Redis 中的所有信息。
     */
    Mono<Boolean>
    deleteUserInfo(String userName);
}
