package com.jesse.examination.question.repository;

import com.jesse.examination.question.dto.FullQuestionInfoDTO;
import com.jesse.examination.question.dto.QuestionWithCorrectDTO;
import com.jesse.examination.question.entity.question.Question;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 问题数据表仓储类。*/
public interface QuestionRepository
    extends ReactiveCrudRepository<Question, Integer>
{
    /**
     * 根据问题 ID 查询某一条完整的问题。
     *
     * @param questionId 问题 ID
     *
     * @return 承载了单条问题完整信息的 Mono
     */
    @Query(
        value = """
            SELECT
                questions.id        AS question_id,
                questions.content   AS question_content,
                questions.answer    AS correct_answer,
                JSON_OBJECTAGG(options.option_key, options.content) AS options
            FROM
                questions
            INNER JOIN
                options ON questions.id = options.question_id
            WHERE question_id = :questionId
            GROUP BY questions.id
            """
    )
    Mono<FullQuestionInfoDTO>
    findOneQuestionWithAllOptions(
        @Param(value = "questionId") Integer questionId
    );

    /**
     * 分页的获取所有问题的 ID，内容，正确选项，正确答案。
     */
    @Query(value = """
        SELECT
            questions.id      AS question_id,
            questions.content AS question_content,
            questions.answer  AS correct_answer,
            options.content   AS answer_content
        FROM questions
        INNER JOIN options ON questions.id = options.question_id
        WHERE options.option_key = questions.answer
        GROUP BY questions.id
        LIMIT :limit OFFSET :offset
        """
    )
    Flux<QuestionWithCorrectDTO>
    findAllQuestionWithCorrectAnswer(
        @Param(value = "limit")  int limit,
        @Param(value = "offset") int offset
    );

    /**
     * 分页的获取所有问题的 ID，内容，正确选项，所有选项和内容。
     */
    @Query(
        value = """
            SELECT
                questions.id        AS question_id,
                questions.content   AS question_content,
                questions.answer    AS correct_answer,
                JSON_OBJECTAGG(options.option_key, options.content) AS options
            FROM
                questions
            INNER JOIN
                options ON questions.id = options.question_id
            GROUP BY questions.id
            LIMIT :limit OFFSET :offset
            """
    )
    Flux<FullQuestionInfoDTO>
    findAllQuestionWithAllOptions(
        @Param(value = "limit")  int limit,
        @Param(value = "offset") int offset
    );
}