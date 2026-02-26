package com.library;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

//开启定时任务
@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties
public class MultiAgentLibraryApplication {

    public static void main(String[] args) {
        //禁用Headless模式，允许AWT/Swing启动JADE GUI
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(MultiAgentLibraryApplication.class, args);
    }

}
