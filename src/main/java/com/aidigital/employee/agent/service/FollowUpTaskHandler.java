package com.aidigital.employee.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aidigital.employee.ops.entity.AsyncTask;
import com.aidigital.employee.ops.service.AsyncTaskHandler;
import org.springframework.stereotype.Component;

@Component
public class FollowUpTaskHandler implements AsyncTaskHandler {

    public static final String TASK_TYPE = "FOLLOW_UP_GENERATION";

    private final ObjectMapper objectMapper;
    private final FollowUpTaskService followUpTaskService;

    public FollowUpTaskHandler(ObjectMapper objectMapper, FollowUpTaskService followUpTaskService) {
        this.objectMapper = objectMapper;
        this.followUpTaskService = followUpTaskService;
    }

    @Override
    public String taskType() {
        return TASK_TYPE;
    }

    @Override
    public void handle(AsyncTask task) {
        try {
            Stage2TaskPayloads.SessionTask payload = objectMapper.readValue(task.getPayload(), Stage2TaskPayloads.SessionTask.class);
            followUpTaskService.generate(payload.customerId(), payload.sessionId(), payload.latestMessage());
        } catch (Exception ex) {
            throw new IllegalStateException("Follow-up task generation failed", ex);
        }
    }
}
