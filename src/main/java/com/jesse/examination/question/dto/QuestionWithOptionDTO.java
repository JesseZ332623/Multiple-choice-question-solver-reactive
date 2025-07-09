package com.jesse.examination.question.dto;

import com.jesse.examination.question.entity.Option.AnswerOption;
import com.jesse.examination.question.entity.Question.Question;
import lombok.*;

import java.util.Map;
import java.util.TreeMap;

/** 单条问题查询时用到的 DTO。 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class QuestionWithOptionDTO
{
    private Integer id;
    private String content;
    private Map<AnswerOption, String> optionMap;

    public QuestionWithOptionDTO(Question question)
    {
        this.optionMap  = new TreeMap<>();
        this.id         = question.getId();
        this.content    = question.getContent();

        question.getOptions().forEach(
            (option) -> {
                // 只从 option 中提取需要的信息
                optionMap.put(
                    option.getOptionKey(),
                    option.getContent()
                );
            }
        );
    }
}
