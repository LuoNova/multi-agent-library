package com.library.service;

import com.library.entity.NotificationTemplate;
import java.util.List;

/**
 * 通知模板服务接口
 */
public interface NotificationTemplateService {

    /**
     * 获取所有模板
     *
     * @return 模板列表
     */
    List<NotificationTemplate> getAllTemplates();

    /**
     * 根据类型和渠道获取模板
     *
     * @param type    通知类型
     * @param channel 通知渠道
     * @return 模板对象
     */
    NotificationTemplate getTemplate(String type, String channel);

    /**
     * 创建模板
     *
     * @param template 模板对象
     * @return 创建结果
     */
    NotificationTemplate createTemplate(NotificationTemplate template);

    /**
     * 更新模板
     *
     * @param template 模板对象
     * @return 更新结果
     */
    NotificationTemplate updateTemplate(NotificationTemplate template);

    /**
     * 删除模板
     *
     * @param templateId 模板ID
     */
    void deleteTemplate(Long templateId);

    /**
     * 启用/禁用模板
     *
     * @param templateId 模板ID
     * @param enabled    是否启用
     */
    void toggleTemplate(Long templateId, boolean enabled);
}
