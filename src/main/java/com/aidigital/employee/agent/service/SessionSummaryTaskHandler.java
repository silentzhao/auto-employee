package com.aidigital.employee.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aidigital.employee.agent.entity.ConversationSession;
import com.aidigital.employee.agent.mapper.ConversationSessionMapper;
import com.aidigital.employee.customer.entity.Customer;
import com.aidigital.employee.customer.mapper.CustomerMapper;
import com.aidigital.employee.ops.entity.AsyncTask;
import com.aidigital.employee.ops.service.AsyncTaskHandler;
import org.springframework.stereotype.Component;

@Component
public class SessionSummaryTaskHandler implements AsyncTaskHandler {

    public static final String TASK_TYPE = "SESSION_SUMMARY";

    private final ObjectMapper objectMapper;
    private final CustomerMapper customerMapper;
    private final ConversationSessionMapper conversationSessionMapper;
    private final SessionSummaryService sessionSummaryService;

    public SessionSummaryTaskHandler(
            ObjectMapper objectMapper,
            CustomerMapper customerMapper,
            ConversationSessionMapper conversationSessionMapper,
            SessionSummaryService sessionSummaryService) {
        this.objectMapper = objectMapper;
        this.customerMapper = customerMapper;
        this.conversationSessionMapper = conversationSessionMapper;
        this.sessionSummaryService = sessionSummaryService;
    }

    @Override
    public String taskType() {
        return TASK_TYPE;
    }

    @Override
    public void handle(AsyncTask task) {
        try {
            Stage2TaskPayloads.SessionTask payload = objectMapper.readValue(task.getPayload(), Stage2TaskPayloads.SessionTask.class);
            Customer customer = customerMapper.selectById(payload.customerId());
            ConversationSession session = conversationSessionMapper.selectById(payload.sessionId());
            if (customer == null || session == null) {
                throw new IllegalStateException("Customer or session no longer exists");
            }
            sessionSummaryService.updateAfterReply(customer, session);
        } catch (Exception ex) {
            throw new IllegalStateException("Session summary task failed", ex);
        }
    }
}
