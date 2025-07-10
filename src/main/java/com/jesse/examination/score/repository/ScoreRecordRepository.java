package com.jesse.examination.score.repository;

import com.jesse.examination.score.dto.ScoreRecordQueryDTO;
import com.jesse.examination.score.entity.ScoreRecord;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/** 成绩表仓储类。 */
public interface ScoreRecordRepository
    extends ReactiveCrudRepository<ScoreRecord, Integer>
{
    /** 查找指定用户的成绩总数。*/
    @Query("""
        SELECT COUNT(*)
        FROM score_record
        INNER JOIN users USING(user_id)
        WHERE user_name = :userName
        GROUP BY user_name
        """)
    Mono<Long>
    findScoreAmountByUserName(
        @Param("userName") String userName
    );

    /** 分页查找指定用户的所有成绩记录。*/
    @Query("""
        SELECT score_id, user_name, submit_date,
        	   correct_count, error_count, no_answer_count
        FROM score_record
        INNER JOIN users USING(user_id)
        WHERE user_name = :userName
        LIMIT :limit OFFSET :offset
        """)
    Flux<ScoreRecordQueryDTO>
    findPaginatedScoreRecordByUserName(
        @Param("userName") String  userName,
        @Param("limit")    Integer limit,
        @Param("offset")   Integer offset
    );

    /** 找出指定用户的最新一条成绩记录。*/
    @Query("""
        SELECT score_id, user_name, submit_date,
        	    correct_count, error_count, no_answer_count
        FROM score_record
        INNER JOIN users USING(user_id)
        WHERE user_name = :userName
        ORDER BY submit_date DESC
        LIMIT 1 OFFSET 0
        """)
    Mono<ScoreRecordQueryDTO>
    findLatestScoreRecordByUserName(
        @Param("userName") String userName
    );

    /** 为指定用户插入一条新的成绩。*/
    @Modifying
    @Query("""
           INSERT INTO score_record(
                user_id, submit_date,
                correct_count, error_count, no_answer_count
           )
           VALUES(
               :userId, :submitDate,
               :correctCount, :errorCount, :noAnswerCount
           )
           """
    )
    Mono<Void>
    insertNewScoreRecordByUserId(
        @Param("userId")        Long userId,
        @Param("submitDate")    LocalDateTime submitDate,
        @Param("correct_count") Integer correctCount,
        @Param("errorCount")    Integer errorCount,
        @Param("noAnswerCount") Integer noAnswerCount
    );

    /** 删除指定用户对应的所有成绩，返回删除的行数。*/
    @Modifying
    @Query("""
        DELETE FROM score_record
        WHERE user_id IN (
            SELECT user_id FROM users
            WHERE user_name = :userName
        )
        """)
    Mono<Integer>
    deleteAllScoreRecordByUserName(
        @Param("userName") String userName
    );

    /** 清空成绩表。*/
    @Modifying
    @Query("TRUNCATE score_record")
    Mono<Void>
    truncateScoreRecordTable();
}
