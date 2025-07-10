package com.jesse.examination.score.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/** 成绩记录表实体类。*/
@Data
@ToString
@Table(name = "score_record")
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(exclude = "scoreId")
public class ScoreRecord
{
    @Id
    @Column("score_id")
    private Integer scoreId;      // 成绩记录 ID

    @Column("user_id")
    private @NonNull Long userId; // 用户 ID

    @Column("submit_date")
    @JsonFormat(
        shape   = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private @NonNull
    LocalDateTime submitDate; // 成绩提交日期

    @Column("correct_count")
    private @NonNull
    Integer correctCount;       // 正确数

    @Column("error_count")
    private @NonNull
    Integer errorCount;         // 错误数

    @Column("no_answer_count")
    private @NonNull
    Integer noAnswerCount;      // 未答数
}