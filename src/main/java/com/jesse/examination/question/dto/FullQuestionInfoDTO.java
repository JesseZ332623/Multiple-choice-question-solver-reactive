package com.jesse.examination.question.dto;
import com.jesse.examination.question.entity.option.AnswerOption;
import lombok.*;

import java.util.Map;
import java.util.TreeMap;

/** 包含一个问题的所有信息的 DTO。*/
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FullQuestionInfoDTO
{
    private Integer                   questionId;
    private String                    questionContent;
    private AnswerOption              correctAnswer;
    private Map<AnswerOption, String> options = new TreeMap<>();
}
