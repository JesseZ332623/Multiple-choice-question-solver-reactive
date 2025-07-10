package com.jesse.examination.score.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/** 用户成绩查询操作 DTO。*/
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "scoreId")
public class ScoreRecordQueryDTO
{
    private Integer scoreId;

    private String userName;

    @JsonFormat(
        shape   = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime submitDate;

    private Integer correctCount;

    private Integer errorCount;

    private Integer noAnswerCount;
}
