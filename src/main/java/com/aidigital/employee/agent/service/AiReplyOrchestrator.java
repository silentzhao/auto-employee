package com.aidigital.employee.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.aidigital.employee.customer.entity.CustomerMemory;
import com.aidigital.employee.customer.mapper.CustomerMemoryMapper;
import com.aidigital.employee.infra.model.port.ChatModelPort;
import com.aidigital.employee.knowledge.service.KnowledgeService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiReplyOrchestrator {

    private final ConversationContextService conversationContextService;
    private final CustomerMemoryMapper customerMemoryMapper;
    private final KnowledgeService knowledgeService;
    private final ChatModelPort chatModelPort;

    public AiReplyOrchestrator(
            ConversationContextService conversationContextService,
            CustomerMemoryMapper customerMemoryMapper,
            KnowledgeService knowledgeService,
            ChatModelPort chatModelPort) {
        this.conversationContextService = conversationContextService;
        this.customerMemoryMapper = customerMemoryMapper;
        this.knowledgeService = knowledgeService;
        this.chatModelPort = chatModelPort;
    }

    /**
     * 统一拼装客户记忆、短期上下文和知识召回结果，生成 AI 回复。
     */
    public String generateReply(Long customerId, Long sessionId, String tenantId, String message) {
        List<ConversationContextService.ContextLine> context = conversationContextService.buildContext(sessionId, 8, 2000);
        CustomerMemory memory = customerMemoryMapper.selectOne(new LambdaQueryWrapper<CustomerMemory>()
                .eq(CustomerMemory::getCustomerId, customerId)
                .last("limit 1"));
        List<KnowledgeService.KnowledgeHit> knowledgeHits = knowledgeService.search(tenantId, message, 3);
        return chatModelPort.generateReply(new ChatModelPort.ModelPrompt(
                "你是企业 AI 数字员工，回复要简洁、专业。",
                memory == null ? "" : nullSafe(memory.getSummary()),
                context,
                knowledgeHits,
                message));
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
