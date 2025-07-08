package com.jesse.examination.core.email.dto;

import jakarta.annotation.Nullable;
import lombok.*;

/**
 * 向指定用户发送邮件的数据传输类。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EmailContent
{
    private String to;              // 发给谁（如 PerterGriffen@gmail.com）
    private String subject;         // 邮箱主题
    private String textBody;        // 邮件正文

    @Nullable
    private String attachmentPath;  // 附件路径（可以为 null 表示没有附件）
}
