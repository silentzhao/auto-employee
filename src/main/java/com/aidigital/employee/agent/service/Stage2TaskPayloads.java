package com.aidigital.employee.agent.service;

public final class Stage2TaskPayloads {

    private Stage2TaskPayloads() {
    }

    public record SessionTask(Long customerId, Long sessionId, String tenantId, String latestMessage) {
    }
}
