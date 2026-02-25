package com.library.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Data
@Component
@ConfigurationProperties(prefix = "jade")
/*
  用于解析yml中jade的自定义配置项
 */
public class JadeProperties {
    private String host = "localhost";
    private String port = "1099";
    private boolean gui = true;
    private String agents = "";  // 格式: agentName:ClassName(args);agentName2:ClassName2
}