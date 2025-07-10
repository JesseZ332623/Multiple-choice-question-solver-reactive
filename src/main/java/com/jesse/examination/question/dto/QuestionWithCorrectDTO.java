package com.jesse.examination.question.dto;

import com.jesse.examination.question.entity.Option.AnswerOption;
import lombok.*;

/** 包含一个问题 + 它的正确答案的 DTO。 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class QuestionWithCorrectDTO
{
    private Integer       questionId;
    private String        questionContent;
    private AnswerOption  correctAnswer;
    private String        answerContent;
}
