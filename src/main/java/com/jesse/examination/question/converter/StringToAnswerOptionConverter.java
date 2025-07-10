package com.jesse.examination.question.converter;

import com.jesse.examination.question.entity.Option.AnswerOption;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/** 将字符串转化成 AnswerOption 枚举类的转换器。*/
@ReadingConverter
public class StringToAnswerOptionConverter implements Converter<String, AnswerOption>
{
    @Override
    public AnswerOption convert(@NotNull String source) {
        return AnswerOption.valueOf(source);
    }
}