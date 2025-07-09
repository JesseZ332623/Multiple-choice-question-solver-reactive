package com.jesse.examination.core.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 从 application.properties 配置文件中读取的部分属性信息。 */
@Data
@Component
public class ProjectProperties
{
    @Value("${server.address}")
    private String serverAddress;

    @Value("${server.port}")
    private String serverPort;

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
