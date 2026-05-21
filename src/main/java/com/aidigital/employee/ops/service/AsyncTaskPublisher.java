package com.aidigital.employee.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aidigital.employee.common.config.AppProperties;
import com.aidigital.employee.ops.entity.AsyncTask;
import com.aidigital.employee.ops.mapper.AsyncTaskMapper;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsyncTaskPublisher {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskPublisher.class);

    private final AsyncTaskMapper asyncTaskMapper;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;

    public AsyncTaskPublisher(
            AsyncTaskMapper asyncTaskMapper,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            ObjectProvider<RabbitTemplate> rabbitTemplateProvider) {
        this.asyncTaskMapper = asyncTaskMapper;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.rabbitTemplateProvider = rabbitTemplateProvider;
    }

    /**
     * Stores every async task durably, then optionally notifies RabbitMQ when it is enabled.
     */
    @Transactional
    public AsyncTask publish(String taskType, String businessKey, Object payload) {
        AsyncTask existing = asyncTaskMapper.selectOne(new LambdaQueryWrapper<AsyncTask>()
                .eq(AsyncTask::getTaskType, taskType)
                .eq(AsyncTask::getBusinessKey, businessKey)
                .last("limit 1"));
        if (existing != null && !"FAILED".equals(existing.getStatus())) {
            return existing;
        }
        Instant now = Instant.now();
        AsyncTask task = AsyncTask.builder()
                .taskType(taskType)
                .businessKey(businessKey)
                .payload(writePayload(payload))
                .status("PENDING")
                .attempts(0)
                .maxAttempts(appProperties.getAsync().getMaxAttempts())
                .nextRunAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        asyncTaskMapper.insert(task);
        notifyRabbit(task);
        return task;
    }

    private String writePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Async task payload cannot be serialized", ex);
        }
    }

    private void notifyRabbit(AsyncTask task) {
        if (!appProperties.getAsync().isRabbitEnabled()) {
            return;
        }
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            log.warn("RabbitMQ is enabled but RabbitTemplate is unavailable; task remains in database id={}", task.getId());
            return;
        }
        rabbitTemplate.convertAndSend(
                appProperties.getAsync().getExchange(),
                appProperties.getAsync().getRoutingKey(),
                task.getId());
    }
}
