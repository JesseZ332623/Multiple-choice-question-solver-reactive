package com.jesse.examination.score;

import com.jesse.examination.core.exception.ResourceNotFoundException;
import com.jesse.examination.score.entity.ScoreRecord;
import com.jesse.examination.score.repository.ScoreRecordRepository;
import com.jesse.examination.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static com.jesse.examination.score.RandomTimeGenerator.randomBetween;

/** 用户成绩表测试类。*/
@Slf4j
@SpringBootTest
public class ScoreOperatorTest
{
    @Autowired
    private ScoreRecordRepository scoreRecordRepository;

    @Autowired
    private UserRepository userRepository;

    private final ThreadLocalRandom random
        = ThreadLocalRandom.current();

    private List<Long> allIdsList;

    @PostConstruct
    void cacheAllIdsList()
    {
        allIdsList =
            this.userRepository
                .findAllIds()
                .timeout(Duration.ofSeconds(5L))
                .collectList()
                .block();

        log.info("All user_id = {}", allIdsList);
    }

    @Contract(" -> new")
    private @NotNull ScoreRecord
    produceOneScoreRecord()
    {
        if (allIdsList.isEmpty())
        {
            throw new ResourceNotFoundException(
                "No users exist! Can't produce score record!"
            );
        }

        return new ScoreRecord(
                    allIdsList.get(random.nextInt(0, allIdsList.size())),
                    randomBetween(
                        LocalDateTime.of(
                            2015, 1, 1,
                            0, 0, 0
                        ),
                        LocalDateTime.now()
                    ),
                    random.nextInt(1, 30),
                    random.nextInt(1, 30),
                    random.nextInt(1, 30)
                );
    }

    @Test
    public void TestNewScoreGenerate()
    {
        // 若数据表中有数据了，这个测试用例就不要运行。
        if (!Objects.requireNonNull(
            this.scoreRecordRepository
                .count().block()).equals(0L)
        ) { return; }

        // 若用户表为空，插入成绩会因为外键限制而失败，因此不会允许本测试用例。
        if (Objects.requireNonNull(
            this.userRepository
                .count().block()).equals(0L)
        ) { return; }

        /* 生成总数据量。*/
        final int INSERT_AMOUNT = 500000;
        final int BUFFER_SIZE   = 10000;
        final int PARALLELISM   = 32;    // 根据 DB 连接池调整并发度

        log.info(
            "Start to generate scores. " +
            "(INSERT_AMOUNT = {}, BUFFER_SIZE = {}, PARALLELISM = {})",
            INSERT_AMOUNT, BUFFER_SIZE, PARALLELISM
        );

        /*
         * 设计并行计划：
         * 池中有 36 个线程，队列长度 4500，执行线程名：Batch-Insert。
         */
        Scheduler scheduler
            = Schedulers.newBoundedElastic(
            36, 4500, "Batch-Insert"
        );

        Mono<Long> insertStream
            = Flux.range(0, INSERT_AMOUNT)
                  .map((index) -> this.produceOneScoreRecord())
                  .buffer(BUFFER_SIZE)
                  .flatMap((scores) ->
                      Mono.defer(() ->
                              this.scoreRecordRepository
                                  .saveAll(scores)
                                  .then(Mono.just((long) scores.size())
                      )
                      ).subscribeOn(scheduler), PARALLELISM
                  )
            .reduce(0L, Long::sum)
            .doOnSuccess((count) ->
                log.info("Successfully inserted {} rows.", count)
            )
            .doOnError((exception) ->
                log.error("Insert failed! Cause: {}.", exception.getMessage())
            );

        StepVerifier.create(insertStream)
                    .expectNext((long) INSERT_AMOUNT)
                    .verifyComplete();
    }
}
