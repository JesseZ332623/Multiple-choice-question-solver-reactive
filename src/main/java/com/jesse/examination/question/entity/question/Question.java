package com.jesse.examination.question.entity.question;

import com.jesse.examination.question.entity.option.AnswerOption;
import com.jesse.examination.question.entity.option.Option;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;

/** 问题数据表实体类。*/
@Data
@Table(name = "questions")
@ToString
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
public class Question
{
    @Id
    @Column("id")
    private Integer id;

    @Column("content")
    private String content;

    @Column("answer")
    private AnswerOption answer;

    @Transient
    private List<Option> options;

    /** 添加一条选项。*/
    public void addOption(Option option) {
        this.options.add(option);
    }
}
