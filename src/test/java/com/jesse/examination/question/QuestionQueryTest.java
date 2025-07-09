package com.jesse.examination.question;

import com.jesse.examination.question.dto.FullQuestionInfoDTO;
import com.jesse.examination.question.entity.Option.Option;
import com.jesse.examination.question.repository.OptionRepository;
import com.jesse.examination.question.repository.QuestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
public class QuestionQueryTest
{
    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Test
    void testFindByQuestionId()
    {
        List<Option> option
            = optionRepository.findByQuestionId(1)
                              .collectList().block();

        System.out.println(option);
    }

    @Test
    void testFindQuestionWithAllOptions()
    {
        List<FullQuestionInfoDTO> allQuestion
            = this.questionRepository.findQuestionWithAllOptions()
                  .collectList().block();

        System.out.println(allQuestion);
    }
}
