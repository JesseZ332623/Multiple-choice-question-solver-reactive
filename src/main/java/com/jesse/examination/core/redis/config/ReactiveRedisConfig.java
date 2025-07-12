package com.jesse.examination.core.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** 响应式 Redis 服务的配置类。*/
@Configuration
public class ReactiveRedisConfig
{
    /**
     * Redis 响应式模板的构建。
     *
     * @param factory Redis 连接工厂，
     *                Spring 会自动读取配置文件中的属性去构建。
     *
     * @return 配置好的 Redis 响应式模板
     */
    @Bean
    public ReactiveRedisTemplate<String, Object>
    reactiveRedisTemplate(ReactiveRedisConnectionFactory factory)
    {
        /* Redis 键使用字符串进行序列化。 */
        RedisSerializer<String> keySerializer
            = new StringRedisSerializer();

        /* Redis 值使用 Jackson 进行序列化。 */
        Jackson2JsonRedisSerializer<Object> valueSerializer
            = new Jackson2JsonRedisSerializer<>(Object.class);

        LongRedisSerializer longRedisSerializer
            = new LongRedisSerializer();

        /* Redis Hash Key/Value 的序列化。 */
        RedisSerializationContext.RedisSerializationContextBuilder<String, Object>
            builder = RedisSerializationContext.newSerializationContext(keySerializer);

        /* 创建 Redis 序列化上下文，设置序列化方式。 */
        RedisSerializationContext<String, Object> context
            = builder.value(valueSerializer)
                     .hashKey(keySerializer)
                     .hashValue(valueSerializer)
                     .hashValue(longRedisSerializer)
                     .build();

        /* 根据上述配置构建 ReactiveRedisTemplate。 */
        return new ReactiveRedisTemplate<>(factory, context);
    }

    static class LongRedisSerializer implements RedisSerializer<Long>
    {
        private final Charset charset = StandardCharsets.UTF_8;

        /** Long 类型的序列化。*/
        @Override
        public byte[] serialize(Long number) throws SerializationException
        {
            return number == null
                ? null
                : number.toString().getBytes(charset);
        }

        /** Long 类型的反序列化。*/
        @Override
        public Long deserialize(byte[] bytes) throws SerializationException
        {
            return bytes == null
                ? null
                : Long.parseLong(new String(bytes, charset));
        }
    }
}
