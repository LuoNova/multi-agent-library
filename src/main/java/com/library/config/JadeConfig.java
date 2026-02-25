package com.library.config;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
public class JadeConfig implements ApplicationRunner {

    @Autowired
    private JadeProperties jadeProperties;

    private AgentContainer mainContainer;
    //记录已启动的Agent控制器，用于优雅关闭
    private List<AgentController> runningAgents = new ArrayList<>();

    @Bean(destroyMethod = "")  //禁用Spring的自动destroy，使用钩子手动处理
    public AgentContainer jadeContainer() {
        try {
            Runtime runtime = Runtime.instance();
            ProfileImpl profile = new ProfileImpl();
            profile.setParameter(Profile.MAIN_HOST, jadeProperties.getHost());
            profile.setParameter(Profile.MAIN_PORT, jadeProperties.getPort());
            profile.setParameter(Profile.GUI, String.valueOf(jadeProperties.isGui()));

            mainContainer = runtime.createMainContainer(profile);
            log.info("JADE容器创建成功");

            //关闭钩子：先停Agent，再停容器，忽略异常
            java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    log.info("正在停止JADE Agents...");
                    //先逐个停止Agent
                    for (AgentController agent : runningAgents) {
                        try {
                            agent.kill();
                        } catch (Exception ignored) {
                            //忽略单个Agent停止失败
                        }
                    }
                    //再停止容器（忽略异常）
                    if (mainContainer != null) {
                        mainContainer.kill();
                    }
                } catch (Exception ignored) {
                    //忽略关闭异常，因为进程即将退出
                }
            }));

            return mainContainer;
        } catch (Exception e) {
            log.error("JADE容器创建失败", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (mainContainer == null) return;

        Thread.sleep(1000); //等待Spring初始化

        if (jadeProperties.getAgents() != null && !jadeProperties.getAgents().isEmpty()) {
            startAgents(mainContainer, jadeProperties.getAgents());
        }
    }

    private void startAgents(AgentContainer container, String agentsConfig) {
        String[] agentDefs = agentsConfig.split(";");
        for (String agentDef : agentDefs) {
            if (agentDef.trim().isEmpty()) continue;
            try {
                //...原有解析逻辑...
                int colonIndex = agentDef.indexOf(':');
                String agentName = agentDef.substring(0, colonIndex).trim();
                String classInfo = agentDef.substring(colonIndex + 1).trim();

                String className;
                Object[] args = null;
                int parenIndex = classInfo.indexOf('(');
                if (parenIndex > 0 && classInfo.endsWith(")")) {
                    className = classInfo.substring(0, parenIndex).trim();
                    String argStr = classInfo.substring(parenIndex + 1, classInfo.length() - 1);
                    args = parseArgs(argStr);
                } else {
                    className = classInfo.trim();
                }

                AgentController controller = container.createNewAgent(agentName, className, args);
                controller.start();
                runningAgents.add(controller); //记录到列表
                log.info("启动Agent: {}", agentName);
            } catch (Exception e) {
                log.error("启动Agent失败: {}", agentDef, e);
            }
        }
    }

    private Object[] parseArgs(String argStr) {
        if (argStr.trim().isEmpty()) return null;
        String[] parts = argStr.split(",");
        Object[] args = new Object[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String arg = parts[i].trim();
            try {
                args[i] = Long.parseLong(arg);
            } catch (NumberFormatException e) {
                args[i] = arg;
            }
        }
        return args;
    }
}