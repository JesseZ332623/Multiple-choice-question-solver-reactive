package com.jesse.examination.user.redis.impl;

import com.jesse.examination.core.properties.ProjectProperties;
import com.jesse.examination.user.redis.UserRedisService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

import static com.jesse.examination.core.redis.errorhandle.RedisGenericErrorHandle.redisGenericErrorHandel;
import static com.jesse.examination.core.redis.keys.ConcatRedisKey.*;
import static java.lang.String.format;

/** 用户模块 Redis 服务实现类。*/
@Slf4j
@Component
public class UserRedisServiceImpl implements UserRedisService
{
    /** 响应式 Redis 模板。*/
    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProjectProperties projectProperties;

    private
    ReactiveHashOperations<String, String, Long> hashOperations;

    /** 在依赖注入完成后，获取哈希数据操作器。*/
    @PostConstruct
    private void setHashOperator()
    {
        Objects.requireNonNull(this.redisTemplate);
        this.hashOperations = this.redisTemplate.opsForHash();
    }

    @Override
    public Mono<Boolean>
    saveUserVarifyCode(String userName, String varifyCode)
    {
        if (userName == null || userName.isEmpty())
        {
            return redisGenericErrorHandel(
                new IllegalArgumentException(
                    "User name not be null or empty!"
                ), null
            );
        }

        if (varifyCode == null || varifyCode.isEmpty())
        {
            return redisGenericErrorHandel(
                new IllegalArgumentException(
                    "Varify code name not be null or empty!"
                ), null
            );
        }

        String varifyCodeKey = varifyCodeKey(userName);

        return this.redisTemplate.hasKey(varifyCodeKey)
                   .flatMap((isExist) ->
                       (!isExist)
                        ? this.redisTemplate.opsForValue()
                              .set(
                                  varifyCodeKey, varifyCode,
                                  Duration.ofSeconds(
                                      Long.parseLong(projectProperties.getVarifyCodeExpiration())
                                  )
                              )
                              .timeout(Duration.ofSeconds(3L))
                              .onErrorResume((exception) ->
                                  redisGenericErrorHandel(exception, null)
                              )
                        : this.redisTemplate.delete(varifyCodeKey)
                              .timeout(Duration.ofSeconds(3L))
                              .onErrorResume((exception) ->
                                  redisGenericErrorHandel(exception, null)
                              )
                              .flatMap((ignore) ->
                                  this.redisTemplate.opsForValue()
                                      .set(varifyCodeKey, varifyCode)
                                      .timeout(Duration.ofSeconds(3L))
                                      .onErrorResume((exception) ->
                                          redisGenericErrorHandel(exception, null)
                                      )
                              )
                  );
    }

    @Override
    public Mono<Boolean>
    deleteUserVarifyCode(String userName)
    {
        if (userName == null || userName.isEmpty())
        {
            return redisGenericErrorHandel(
                new IllegalArgumentException(
                    "User name not be null or empty!"
                ), null
            );
        }

        String varifyCodeKey = varifyCodeKey(userName);

        return this.redisTemplate.hasKey(varifyCodeKey)
            .flatMap((isExist) ->
                (isExist)
                    ? this.redisTemplate.opsForValue()
                          .delete(varifyCodeKey)
                          .timeout(Duration.ofSeconds(3L))
                          .onErrorResume((exception) ->
                              redisGenericErrorHandel(exception, null)
                          )
                    : redisGenericErrorHandel(
                        new IllegalArgumentException(
                        format(
                            "Deltete varidy code for %s failed! Cause: Key: %s not exist!",
                            userName, varifyCodeKey)
                        ), null
                )
            );
    }

    @Override
    public Mono<String>
    getUserVarifyCode(String userName)
    {
        if (userName == null || userName.isEmpty())
        {
            return redisGenericErrorHandel(
                new IllegalArgumentException(
                    "User name not be null or empty!"
                ), null
            );
        }

        String varifyCodeKey = varifyCodeKey(userName);

        return this.redisTemplate.hasKey(varifyCodeKey)
                    .timeout(Duration.ofSeconds(3L))
                    .onErrorResume((exception) ->
                        redisGenericErrorHandel(exception, null))
                    .flatMap((isExist) ->
                       (!isExist)
                        ? redisGenericErrorHandel(
                            new IllegalArgumentException(
                                format("Varify code of user: %s not exist or expired!", userName)
                            ), null)
                        : this.redisTemplate.opsForValue()
                              .get(varifyCodeKey)
                              .map((res) -> (String) res)
                              .timeout(Duration.ofSeconds(3L))
                              .onErrorResume((exception) ->
                                  redisGenericErrorHandel(exception, null))
                   );
    }

    /**
     * 某用户登录时，
     * 将从该用户存档中读取所有问题答对次数数据以哈希表的形式存入 Redis。
     *
     * @param userName            用户名
     * @param quesCorrectTimesMap 从 JSON 存档文件中读出，并映射后的哈希表，
     *                            映射关系是：问题 ID -> 问题答对次数
     * @return 是否成功存入 Redis?
     */
    @Override
    public Mono<Boolean>
    loadUserQuestionCorrectTimes(
        String userName,
        Map<String, Long> quesCorrectTimesMap
    )
    {
        if (userName == null || userName.isEmpty())
        {
            return redisGenericErrorHandel(
                new IllegalArgumentException(
                    "User name not be null or empty!"
                ), null
            );
        }

        if (quesCorrectTimesMap == null || quesCorrectTimesMap.isEmpty())
        {
            return redisGenericErrorHandel(
                new IllegalArgumentException(
                    "Question Correct TimesMap not be null or empty!"
                ), null
            );
        }

        return this.hashOperations
            .putAll(
                correctTimesHashKey(userName),
                quesCorrectTimesMap)
            .timeout(Duration.ofSeconds(3L))
            .onErrorResume(
                (exception) ->
                    redisGenericErrorHandel(exception, false)
            );
    }

    /**
     * 某用户登出时，
     * 将从 Redis 中读取所有问题答对次数数据哈希表，再存回用户存档中。
     *
     * @param userName 用户名
     *
     * @throws IllegalArgumentException 用户名对应的 key 不存在时抛出
     *
     * @return 从 Redis 中读取的所有问题答对次数数据哈希表
     */
    @Override
    public Mono<Map<String, Long>>
    getUserQuestionCorrectTimes(String userName)
    {
        if (userName == null || userName.isEmpty())
        {
            return redisGenericErrorHandel(
                new IllegalArgumentException(
                    "User name not be null or empty!"
                ), null
            );
        }

        String key = correctTimesHashKey(userName);

        return this.redisTemplate.hasKey(key)
                   .flatMap((isExist) ->
                       (isExist)
                       ? this.hashOperations.entries(correctTimesHashKey(userName))
                             .timeout(Duration.ofSeconds(3L))
                             .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                             .onErrorResume((exception) ->
                                 redisGenericErrorHandel(exception, new TreeMap<>())
                             )
                             .map((unorderedMap) -> {
                                /*
                                 * 由于 Redis 配置中键是按照 String 存储的，
                                 * 这里需要准备一个 TreeMap，把键转成整数再比较。
                                 */
                                 TreeMap<String, Long> sortedMap = new TreeMap<>(
                                     Comparator.comparing(Long::parseLong)
                                 );

                                 sortedMap.putAll(unorderedMap);

                                 return sortedMap;
                           }).flatMap((sortedMap) ->
                                this.redisTemplate.delete(key)
                                    .timeout(Duration.ofSeconds(3L))
                                    .onErrorResume((exception) ->
                                        redisGenericErrorHandel(exception, -1L)
                                    )
                                    .thenReturn(sortedMap)
                           )
                       : redisGenericErrorHandel(
                           new IllegalArgumentException(format("Key: %s is not exist!", key)),
                           null
                       )
                   );
    }

    /**
     * 将某个用户下的所有数据删除。
     *
     * @param userName 用户名
     *
     * @return 是否成功从 Redis 中删除?
     */
    @Override
    public Mono<Boolean>
    deleteUserInfo(String userName)
    {
        if (userName == null || userName.isEmpty())
        {
            return redisGenericErrorHandel(
                new IllegalArgumentException(
                    "User name not be null or empty!"
                ), null
            );
        }

        return this.redisTemplate.scan(
            ScanOptions.scanOptions()
                       .match(allKeysOfUserPattern(userName))
                       .build()
            )
            .timeout(Duration.ofSeconds(3L))
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null)
            )
            .flatMap((key) ->
                this.redisTemplate.delete(key)
                    .timeout(Duration.ofSeconds(3L))
                    .onErrorResume((exception) ->
                        redisGenericErrorHandel(exception, null)
                    )
            )
            .then(Mono.just(true));
    }

    @Override
    public Flux<String> getAllUsers()
    {
        return this.redisTemplate.scan(
            ScanOptions.scanOptions()
                       .match(allUserPatten())
                       .build()
        )
        .timeout(Duration.ofSeconds(3L))
        .onErrorResume((exception) ->
            redisGenericErrorHandel(exception, null)
        )
        .map((key) -> {
                String[] subStr = key.split(":");

                log.info("{}", Arrays.toString(subStr));

                return subStr[1];
            }
        );
    }
}
