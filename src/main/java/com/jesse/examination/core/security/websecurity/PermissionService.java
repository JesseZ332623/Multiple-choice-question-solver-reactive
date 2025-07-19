package com.jesse.examination.core.security.websecurity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** 自定义权限验证器实现。 */
@Slf4j
@Service
public class PermissionService
{
    public Mono<Boolean>
    hasPermission(Authentication authentication, String permission)
    {
        return Mono.justOrEmpty(authentication)
                   .flatMap(auth ->
                       auth.getAuthorities().stream()
                           .anyMatch((grantedAuthority)
                               -> grantedAuthority.getAuthority().equals(permission))
                            ? Mono.just(true)
                            : Mono.just(false))
                   .defaultIfEmpty(false);
    }
}
