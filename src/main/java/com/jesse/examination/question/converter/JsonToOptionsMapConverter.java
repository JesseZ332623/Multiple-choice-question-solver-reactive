package com.jesse.examination.question.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import com.jesse.examination.question.entity.option.AnswerOption;
import org.springframework.data.convert.ReadingConverter;

import java.util.Map;
import java.util.TreeMap;

/** 将一个 JSON 字符串转换成 {@literal Map<AnswerOption, String>} 的映射表的转换器。*/
@ReadingConverter
public class JsonToOptionsMapConverter
    implements Converter<String, Map<AnswerOption, String>>
{
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Map<AnswerOption, String> convert(@NotNull String optionsJson)
    {
        try
        {
            Map<String, String> tempMap
                = mapper.readValue(
                optionsJson, new TypeReference<>() {
                }
            );

            Map<AnswerOption, String> result = new TreeMap<>();

            tempMap.forEach((option, content) ->
                result.put(AnswerOption.valueOf(option), content));

            return result;
        }
        catch (JsonProcessingException exception) {
            throw new RuntimeException("Failed to parse options JSON.", exception);
        }
    }
}
