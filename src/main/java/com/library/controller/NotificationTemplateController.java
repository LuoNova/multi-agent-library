package com.library.controller;

import com.library.entity.NotificationTemplate;
import com.library.service.NotificationTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知模板管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/notification/template")
@Tag(name = "通知模板管理", description = "通知模板的增删改查")
public class NotificationTemplateController {

    @Autowired
    private NotificationTemplateService templateService;

    @GetMapping("/list")
    @Operation(summary = "获取所有模板", description = "获取所有通知模板列表")
    public List<NotificationTemplate> getAllTemplates() {
        return templateService.getAllTemplates();
    }

    @GetMapping("/get")
    @Operation(summary = "获取模板", description = "根据类型和渠道获取模板")
    public NotificationTemplate getTemplate(
            @Parameter(description = "通知类型", required = true) @RequestParam String type,
            @Parameter(description = "通知渠道", required = true) @RequestParam String channel) {
        return templateService.getTemplate(type, channel);
    }

    @PostMapping("/create")
    @Operation(summary = "创建模板", description = "创建新的通知模板")
    public NotificationTemplate createTemplate(@RequestBody NotificationTemplate template) {
        return templateService.createTemplate(template);
    }

    @PutMapping("/update")
    @Operation(summary = "更新模板", description = "更新通知模板")
    public NotificationTemplate updateTemplate(@RequestBody NotificationTemplate template) {
        return templateService.updateTemplate(template);
    }

    @DeleteMapping("/delete/{templateId}")
    @Operation(summary = "删除模板", description = "删除通知模板")
    public String deleteTemplate(
            @Parameter(description = "模板ID", required = true) @PathVariable Long templateId) {
        templateService.deleteTemplate(templateId);
        return "删除成功";
    }

    @PutMapping("/toggle/{templateId}")
    @Operation(summary = "启用/禁用模板", description = "切换模板的启用状态")
    public String toggleTemplate(
            @Parameter(description = "模板ID", required = true) @PathVariable Long templateId,
            @Parameter(description = "是否启用", required = true) @RequestParam boolean enabled) {
        templateService.toggleTemplate(templateId, enabled);
        return enabled ? "启用成功" : "禁用成功";
    }
}
