package com.jesse.examination.question.redis;

import reactor.core.publisher.Mono;

import java.util.Map;

/** 问题数据统计 Redis 服务接口。 */
public interface QuestionRedisService
{
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
     * 将某用户的某道问题的答对次数设为 value。
     *
     * @param userName     用户名
     * @param questionId   问题 ID
     * @param specifiedVal 要设置的值
     *
     * @return 设置后该用户本题的答对次数
     */
   Mono<Long>
   setUserQuestionCorrectTime(
       String userName, Long questionId, Long specifiedVal
   );

   /**
    * 将某用户所有问题的答对次数清空为 0。
    *
    * @param userName  用户名
    *
    * @return 是否清空成功？
    */
    Mono<Boolean>
    clearUserQuestionCorrectTime(String userName);
}
