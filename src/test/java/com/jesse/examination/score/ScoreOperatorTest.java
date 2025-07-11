package com.jesse.examination.score;

import com.jesse.examination.score.entity.ScoreRecord;
import com.jesse.examination.score.repository.ScoreRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
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

    private final ThreadLocalRandom random
        = ThreadLocalRandom.current();

    private ScoreRecord
    produceOneScoreRecord()
    {
        return new ScoreRecord(
                random.nextLong(1, 4),
                randomBetween(
                    LocalDateTime.of(
                        2020, 1, 1,
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
        if (Objects.requireNonNull(
            this.scoreRecordRepository
                .count().block()).compareTo(0L) > 0
        ) { return; }

        /* 生成总数据量。*/
        final int INSERT_AMOUNT = 500000;
        final int BUFFER_SIZE   = 10000;
        final int PARALLELISM   = 16;    // 根据 DB 连接池调整并发度

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
