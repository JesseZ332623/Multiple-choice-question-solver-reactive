package com.jesse.examination.core.security.websecurity;

import com.jesse.examination.core.properties.ProjectProperties;
import io.jsonwebtoken.security.Keys;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;

/** 一个纯 Restful 响应式服务器的 Web Security 配置类。*/
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class WebSecurityConfig
{
    /** 自定义权限验证器，暂时没用到。*/
    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProjectProperties projectProperties;


    private Converter<Jwt, Mono<AbstractAuthenticationToken>>
    grantedAuthoritiesConverter()
    {
        JwtGrantedAuthoritiesConverter authoritiesConverter
            = new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");

        ReactiveJwtAuthenticationConverter converter
            = new ReactiveJwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
            (jwt) ->
                Mono.justOrEmpty(authoritiesConverter.convert(jwt))
                    .flatMapMany(Flux::fromIterable)
        );

         return converter;
    }

    /** 密码解码器的构建。*/
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 响应式 JWT（Json Web Token）的解码器构建 */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder()
    {
        SecretKey key = Keys.hmacShaKeyFor(
            this.projectProperties
                .getJwtSecretKey().getBytes()
        );

        return NimbusReactiveJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public SecurityWebFilterChain
    securityWebFilterChain(@NotNull ServerHttpSecurity httpSecurity)
    {
        return httpSecurity
            .csrf(ServerHttpSecurity.CsrfSpec::disable)             // 禁用 CSRF 跨站请求伪造防护
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)   // 禁用 Spring-Security 的默认登录页面
            .logout(ServerHttpSecurity.LogoutSpec::disable)         // 禁用 Spring-Security 的默认登出页面
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)   // 严格要求 HTTPS 连接
            .exceptionHandling((handle) ->
                handle
                    // 未授权时返回的错误码
                    .authenticationEntryPoint(
                        new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                    // 授权但权限不足时返回的错误码
                    .accessDeniedHandler(
                        new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN))
            )
            /* 对于不同的 API，划定不同的权限（目前处于开发阶段，暂时放行所有 /api/ 下的请求）。 */
            .authorizeExchange((exchange) ->
//                exchange.pathMatchers(HttpMethod.GET, "/api/public/**").permitAll()
//                        .pathMatchers(HttpMethod.POST, "/api/admin/login").permitAll()
//                        .pathMatchers(HttpMethod.POST, "/api/user/login").permitAll()
//                        .pathMatchers("/api/admin/**").hasRole("ROLE_ADMIN")
//                        .pathMatchers("/api/user/**").hasAnyRole("ROLE_USER", "ROLE_ADMIN")
//                        .pathMatchers("/api/**").authenticated()
//                        .anyExchange().denyAll()    // 若发起上述 API 之外的请求，通通拒绝
                exchange.pathMatchers("/api/**").permitAll()
                        .anyExchange().denyAll()
            )
            .oauth2ResourceServer((oauth2) ->
                oauth2.jwt(
                    jwt -> jwt.jwtAuthenticationConverter(
                        grantedAuthoritiesConverter()
                    )
                )
            )
            .build();
    }
}
