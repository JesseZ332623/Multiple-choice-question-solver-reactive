package com.jesse.examination.question.entity.option;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 问题选项数据表实体类。*/
@Data
@ToString
@Table(name = "options")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
public class Option
{
    /**
     * 哑 ID，没有别的意义，只是因为实体必须要有一个 @Id 注解。
     * R2DBC 没有 JPA 那般灵活，
     * 对复合主键的支持有限，所以这块需要手动操作。
     */
    @Id
    private Long dummyId = -1L;

    @Column("question_id")  // 显式映射 question_id
    private Integer questionId;

    @Column("option_key")   // 显式映射 option_key
    private AnswerOption optionKey;

    @Column(value = "content")
    private String content;
}
