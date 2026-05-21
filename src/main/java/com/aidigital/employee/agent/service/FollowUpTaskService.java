package com.aidigital.employee.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.aidigital.employee.agent.entity.FollowUpTask;
import com.aidigital.employee.agent.mapper.FollowUpTaskMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowUpTaskService {

    private final FollowUpTaskMapper followUpTaskMapper;

    public FollowUpTaskService(FollowUpTaskMapper followUpTaskMapper) {
        this.followUpTaskMapper = followUpTaskMapper;
    }

    /**
     * Creates a sales follow-up task when the latest message carries strong action intent.
     */
    @Transactional
    public void generate(Long customerId, Long sessionId, String latestMessage) {
        String content = latestMessage == null ? "" : latestMessage;
        if (!containsAny(content, "报价", "方案", "采购", "合同", "人工", "销售")) {
            return;
        }
        FollowUpTask existing = followUpTaskMapper.selectOne(new LambdaQueryWrapper<FollowUpTask>()
                .eq(FollowUpTask::getCustomerId, customerId)
                .eq(FollowUpTask::getSessionId, sessionId)
                .eq(FollowUpTask::getStatus, "OPEN")
                .last("limit 1"));
        if (existing != null) {
            return;
        }
        Instant now = Instant.now();
        followUpTaskMapper.insert(FollowUpTask.builder()
                .customerId(customerId)
                .sessionId(sessionId)
                .title("跟进客户最新咨询")
                .priority(containsAny(content, "今天", "马上", "合同") ? "HIGH" : "NORMAL")
                .status("OPEN")
                .dueAt(now.plus(1, ChronoUnit.DAYS))
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    public List<FollowUpTask> listOpen(Long customerId) {
        return followUpTaskMapper.selectList(new LambdaQueryWrapper<FollowUpTask>()
                .eq(customerId != null, FollowUpTask::getCustomerId, customerId)
                .eq(FollowUpTask::getStatus, "OPEN")
                .orderByAsc(FollowUpTask::getDueAt));
    }

    public List<FollowUpTask> list(Long customerId, String status) {
        return followUpTaskMapper.selectList(new LambdaQueryWrapper<FollowUpTask>()
                .eq(customerId != null, FollowUpTask::getCustomerId, customerId)
                .eq(status != null && !status.isBlank(), FollowUpTask::getStatus, normalizeStatus(status))
                .orderByAsc(FollowUpTask::getDueAt));
    }

    @Transactional
    public FollowUpTask complete(Long taskId) {
        return updateStatus(taskId, "COMPLETED");
    }

    @Transactional
    public FollowUpTask cancel(Long taskId) {
        return updateStatus(taskId, "CANCELED");
    }

    private FollowUpTask updateStatus(Long taskId, String status) {
        FollowUpTask task = followUpTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Follow-up task does not exist");
        }
        task.setStatus(status).setUpdatedAt(Instant.now());
        followUpTaskMapper.updateById(task);
        return task;
    }

    private String normalizeStatus(String status) {
        if ("PENDING".equals(status) || "IN_PROGRESS".equals(status)) {
            return "OPEN";
        }
        return status;
    }

    private boolean containsAny(String content, String... keywords) {
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
