package com.jesse.examination.core.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 从 application.properties 配置文件中读取的部分属性信息。 */
@Slf4j
@Getter
@Component
@ToString
public class ProjectProperties
{
    @Value("${server.address}")
    private String serverAddress;

    @Value("${server.port}")
    private String serverPort;

    @Value("${file.upload.default-avatar-dir}")
    private String defaultAvatarPath;

    @Value("${file.upload-dir}")
    private String userArchivePath;

    @Value("${jwt.secret-key}")
    private String jwtSecretKey;

    @Value("${jwt.expiration}")
    private String jwtExpiration;

    @Value("${app.redis.varify-code-expiration}")
    private String varifyCodeExpiration;

    @Value("${app.varify-code-length}")
    private String varifyCodeLength;

    @PostConstruct
    void showAllProperties() {
        log.info(this.toString());
    }

    /**
     * 获取本服务器根 URL，示例如下：</br>
     * <code>
     *      <a href="http://192.168.60.12:8081/">
     *          http://192.168.60.12:8081/
     *      </a>
     * </code>
     *
     * @return 本服务器根 URL
     */
    public String getServerRootURL() {
        return "http://" + serverAddress + ":" + serverPort;
    }
}
