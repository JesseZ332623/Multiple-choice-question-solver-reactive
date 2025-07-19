package com.jesse.examination.question;

import com.jesse.examination.question.repository.OptionRepository;
import com.jesse.examination.question.repository.QuestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 问题模块查询测试类。 */
@Slf4j
@SpringBootTest
public class QuestionQueryTest
{
    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    /* 测试使用 Postman 皆通过，这里可以先暂时不写 */
}
