package com.jesse.examination.core.email.service;

import com.jesse.examination.core.email.entity.EmailAuthTable;
import reactor.core.publisher.Mono;

public interface EmailAuthQueryService
{
    /**
     * 通过邮箱号查询这个邮箱的服务授权码。
     *
     * @param email 邮箱号
     *
     * @return 授权码字符串
     */
    Mono<String>
    findAuthCodeByEmail(String email);

    /**
     * 通过 ID 查询整个邮箱发布者信息。
     *
     * <p>Tips: 这只是一个安全措施，id 是明确的。</p>
     */
    Mono<EmailAuthTable>
    findEmailPublisherInfoById(Integer id);
}
