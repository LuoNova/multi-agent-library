package com.library.controller;

import jade.wrapper.AgentContainer;
import jade.wrapper.ControllerException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private AgentContainer jadeContainer;

    @PostConstruct
    public void init() {
        log.info("TestController初始化，AgentContainer注入状态: {}",
                jadeContainer != null ? "成功" : "失败");
    }

    @GetMapping("/jade-status")
    public String getJadeStatus() {
        try {
            if (jadeContainer == null) {
                return "❌ JADE容器未注入（检查JadeConfig是否报错）";
            }
            return "✅ JADE运行中: " + jadeContainer.getContainerName()
                    + ", 平台: " + jadeContainer.getPlatformName();
        } catch (ControllerException e) {
            return "❌ 获取容器信息失败: " + e.getMessage();
        }
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong - 系统运行正常";
    }
}