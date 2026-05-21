package com.aidigital.employee.customer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aidigital.employee.agent.service.Stage2TaskPayloads;
import com.aidigital.employee.ops.entity.AsyncTask;
import com.aidigital.employee.ops.service.AsyncTaskHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomerTagTaskHandler implements AsyncTaskHandler {

    public static final String TASK_TYPE = "CUSTOMER_TAG_EXTRACTION";

    private final ObjectMapper objectMapper;
    private final CustomerTagService customerTagService;

    public CustomerTagTaskHandler(ObjectMapper objectMapper, CustomerTagService customerTagService) {
        this.objectMapper = objectMapper;
        this.customerTagService = customerTagService;
    }

    @Override
    public String taskType() {
        return TASK_TYPE;
    }

    @Override
    public void handle(AsyncTask task) {
        try {
            Stage2TaskPayloads.SessionTask payload = objectMapper.readValue(task.getPayload(), Stage2TaskPayloads.SessionTask.class);
            customerTagService.extractRuleTags(payload.customerId(), payload.latestMessage());
        } catch (Exception ex) {
            throw new IllegalStateException("Customer tag extraction task failed", ex);
        }
    }
}
