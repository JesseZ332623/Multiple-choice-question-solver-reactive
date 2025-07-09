package com.jesse.examination.core.r2dbc;

import com.jesse.examination.question.converter.JsonToOptionsMapConverter;
import com.jesse.examination.question.converter.StringToAnswerOptionConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.MySqlDialect;
import org.springframework.r2dbc.core.DatabaseClient;

import java.util.List;

/** Spring Data R2DBC 配置类。 */
@Configuration
public class R2dbcConfig
{
    /**
     * 向 R2DBC 提交几个自定义的转换器，
     * 并且阐明使用 MySQL 方言。
     *
     * <ol>
     *     <li>JsonToMapConverter </li>
     *     <li>StringToAnswerOptionConverter</li>
     * </ol>
     */
    @Bean
    public R2dbcCustomConversions
    customConversions(DatabaseClient client)
    {
        return R2dbcCustomConversions.of(
            MySqlDialect.INSTANCE,
            List.of(
                new JsonToOptionsMapConverter(),
                new StringToAnswerOptionConverter()
            )
        );
    }
}
