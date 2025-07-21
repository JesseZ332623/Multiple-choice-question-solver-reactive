package com.jesse.examination.question.redis.impl;

import com.jesse.examination.question.redis.QuestionRedisService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.jesse.examination.core.redis.errorhandle.RedisGenericErrorHandle.redisGenericErrorHandel;
import static com.jesse.examination.core.redis.keys.ConcatRedisKey.correctTimesHashKey;
import static java.lang.String.format;

/** 问题数据统计 Redis 服务实现类。*/
@Slf4j
@Component
public class QuestionRedisServiceImpl implements QuestionRedisService
{
    /** 响应式 Redis 模板。*/
    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    /**
     * <p>
     *     从模板中获取的 Redis 哈希数据操作器。
     *     他的 3 个类型参数需要我说明一下：
     * </p>
     *
     * <code><pre>
     * ReactiveHashOperations{@literal <H, HK, HV>}
     * H:  整个 Hash 结构的键类型
     * HK: 哈希结构内部的键类型
     * HV: 哈希结构内部的值类型
     * </pre></code>
     */
    private
    ReactiveHashOperations<String, String, Long> hashOperations;

    /** 在依赖注入完成后，获取哈希数据操作器。*/
    @PostConstruct
    private void setHashOperator()
    {
        this.hashOperations
            = Objects.requireNonNull(this.redisTemplate)
                     .opsForHash();
    }

    /**
     * 用户在练习时答对了一道题，这题的答对次数 + 1。
     *
     * @param userName   用户名
     * @param questionId 问题 ID
     *
     * @return 增加后该用户本题的答对次数
     */
    @Override
    public Mono<Long>
    incrementUserQuestionCorrectTime(
        String userName, Long questionId
    )
    {
        String key = correctTimesHashKey(userName);

        return this.hashOperations.hasKey(key, String.valueOf(questionId))
                   .flatMap(
                       (isExist) ->
                           (isExist)
                               ? this.hashOperations
                                     .increment(
                                         key, String.valueOf(questionId), 1L)
                                    .timeout(Duration.ofSeconds(3L))
                                    .onErrorResume((exception) ->
                                        redisGenericErrorHandel(exception, -1L)
                                   )
                               : redisGenericErrorHandel(
                                   new IllegalArgumentException(
                                       format("Key: %s not exist!", key + ":" + questionId)
                                   ), null
                           )
                   );
    }

    /**
     * 将某用户的某道问题的答对次数设为 value。
     *
     * @param userName     用户名
     * @param questionId   问题 ID
     * @param specifiedVal 要设置的值
     *
     * @return 设置后该用户本题的答对次数
     */
    @Override
    public Mono<Long>
    setUserQuestionCorrectTime(
        String userName, Long questionId, Long specifiedVal
    )
    {
        String key = correctTimesHashKey(userName);

        return this.hashOperations.hasKey(key, String.valueOf(questionId))
                .flatMap((isExist) ->
                    (!isExist)
                        ? redisGenericErrorHandel(
                            new IllegalArgumentException(
                                format("Key: %s not exist!", key + ":" + questionId)
                            ), null)
                        : this.hashOperations
                              .put(key, String.valueOf(questionId), specifiedVal)
                              .timeout(Duration.ofSeconds(3L))
                        .flatMap((isSuccess) ->
                            (isSuccess)
                                ? Mono.just(specifiedVal)
                                : Mono.just(-1L)
                        ).onErrorResume((exception) ->
                            redisGenericErrorHandel(exception, -1L)
                        )
                );
    }

    /**
     * <p>
     *     将某用户所有问题的答对次数清空为 0。</br>
     *     思路很简单，从 Redis 中获取键，
     *     再把每个键的值设成 0L，把这一整个 Map 提交给 Redis。
     * </p>
     *
     * @param userName  用户名
     *
     * @return 是否清空成功？
     */
    @Override
    public Mono<Boolean>
    clearUserQuestionCorrectTime(String userName)
    {
        String key = correctTimesHashKey(userName);

        return this.redisTemplate.hasKey(key)
                   .flatMap((isExist) ->
                       (isExist)
                           ? this.hashOperations
                                 .keys(key)
                                 .timeout(Duration.ofSeconds(3L))
                                 .onErrorResume((exception) ->
                                     redisGenericErrorHandel(exception, null))
                                 .collectList()
                                 .flatMap((keys) ->
                                 {
                                     if (keys.isEmpty()) { return Mono.just(true); }

                                     Map<String, Long> clearedMap
                                         = keys.stream()
                                               .collect(
                                                   Collectors.toMap(
                                                       k -> k, v -> 0L
                                                   )
                                               );

                                     return this.hashOperations
                                                .putAll(key, clearedMap)
                                                .timeout(Duration.ofSeconds(3L))
                                                .onErrorResume((exception) ->
                                                    redisGenericErrorHandel(exception, false)
                                                );
                               })
                           : redisGenericErrorHandel(
                               new IllegalArgumentException(
                                   format("Key: %s not exist!", key)
                               ), null
                       )
                   );
    }
}
