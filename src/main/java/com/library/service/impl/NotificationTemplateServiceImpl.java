package com.library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.entity.NotificationTemplate;
import com.library.mapper.NotificationTemplateMapper;
import com.library.service.NotificationTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通知模板服务实现类
 */
@Slf4j
@Service
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    @Autowired
    private NotificationTemplateMapper templateMapper;

    @Override
    public List<NotificationTemplate> getAllTemplates() {
        return templateMapper.selectList(null);
    }

    @Override
    public NotificationTemplate getTemplate(String type, String channel) {
        LambdaQueryWrapper<NotificationTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationTemplate::getType, type)
                .eq(NotificationTemplate::getChannel, channel);
        return templateMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationTemplate createTemplate(NotificationTemplate template) {
        // 检查是否已存在相同类型和渠道的模板
        NotificationTemplate existing = getTemplate(template.getType(), template.getChannel());
        if (existing != null) {
            throw new RuntimeException("模板已存在: type=" + template.getType() + ", channel=" + template.getChannel());
        }

        templateMapper.insert(template);
        log.info("创建通知模板成功: id={}", template.getId());
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationTemplate updateTemplate(NotificationTemplate template) {
        NotificationTemplate existing = templateMapper.selectById(template.getId());
        if (existing == null) {
            throw new RuntimeException("模板不存在: id=" + template.getId());
        }

        templateMapper.updateById(template);
        log.info("更新通知模板成功: id={}", template.getId());
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long templateId) {
        templateMapper.deleteById(templateId);
        log.info("删除通知模板成功: id={}", templateId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleTemplate(Long templateId, boolean enabled) {
        NotificationTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在: id=" + templateId);
        }

        template.setIsEnabled(enabled ? 1 : 0);
        templateMapper.updateById(template);
        log.info("{}通知模板成功: id={}", enabled ? "启用" : "禁用", templateId);
    }
}
