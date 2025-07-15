package com.jesse.examination.core.redis.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** 响应式 Redis 服务的配置类。*/
@Configuration
public class ReactiveRedisConfig
{
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    /** Redis 响应式连接工厂配置类。 */
    @Bean
    @Primary
    public ReactiveRedisConnectionFactory
    reactiveRedisConnectionFactory()
    {
        // 1. 创建独立 Redis 配置
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);       // Redis 地址
        config.setPort(redisPort);           // Redis 端口

        // 密码
        config.setPassword(RedisPassword.of(redisPassword));

        // 2. 创建客户端配置
        LettuceClientConfiguration clientConfig
            = LettuceClientConfiguration.builder()
            .clientOptions(
                ClientOptions.builder()
                    .autoReconnect(true)
                    .socketOptions(
                        SocketOptions.builder()
                            .connectTimeout(Duration.ofSeconds(3L)) // 连接超时
                            .keepAlive(true) // 自动管理 TCP 连接存活
                            .build()
                    )
                    .timeoutOptions(
                        TimeoutOptions.builder()
                            .fixedTimeout(Duration.ofSeconds(3L)) // 操作超时
                            .build()
                    ).build()
            )
            .commandTimeout(Duration.ofSeconds(3L))  // 命令超时时间
            .shutdownTimeout(Duration.ofSeconds(3L)) // 关闭超时时间
            .build();

        // 3. 创建连接工厂
        return new LettuceConnectionFactory(config, clientConfig);
    }

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

    /** 配置 Long 类型的序列化与反序列化。 */
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
