package com.jesse.examination.question.repository;

import com.jesse.examination.question.dto.FullQuestionInfoDTO;
import com.jesse.examination.question.entity.Question.Question;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface QuestionRepository
    extends ReactiveCrudRepository<Question, Integer>
{
    /**
     * <p>获取所有问题的 id，内容，正确答案，所有选项。示例如下：</p>
     *
     * <code>
     *      <pre>
     * QuestionID: 1
     * QuestionContent: 若某机器数为10000000，它代表-127，则它是
     * CorrectAnswer: B
     * OptionsJSON: {"A": "原码", "B": "反码", "C": "补码", "D": "移码"}
     *      </pre>
     * </code>
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
            """
    )
    Flux<FullQuestionInfoDTO> findQuestionWithAllOptions();
}