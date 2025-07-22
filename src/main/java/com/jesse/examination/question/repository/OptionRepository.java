package com.jesse.examination.question.repository;

import com.jesse.examination.question.entity.option.AnswerOption;
import com.jesse.examination.question.entity.option.Option;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 问题选项数据表仓储类。*/
public interface OptionRepository
    extends ReactiveCrudRepository<Option, Long>
{
    /**
     * 根据问题的 ID 获取问题的所有选项。
     *
     * @param questionId 问题 ID
     *
     * @return 承载了这个问题所有选项的 Flux
     */
    @Query(value = """
            SELECT question_id, option_key, content
            FROM exam_question.options
            WHERE question_id = :questionId
        """)
    Flux<Option> findByQuestionId(
        @Param(value = "questionId") Integer questionId
    );

    /**
     * 通过复合主键去查询某个问题的某一条选项。
     *
     * @param questionId 问题 ID
     * @param option     选项号
     *
     * @return 承载了执行问题指定选项的 Mono
     */
    @Query(value = """
            SELECT question_id, option_key, content
            FROM exam_question.options
            WHERE question_id = :questionId
                  AND
                  option_key = :option
        """)
    Mono<Option> findOneOptionById(
        @Param(value = "questionId") Integer questionId,
        @Param(value = "option")     AnswerOption option
    );
}
