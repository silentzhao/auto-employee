package com.aidigital.employee.infra.model.port;

import com.aidigital.employee.agent.service.ConversationContextService;
import com.aidigital.employee.knowledge.service.KnowledgeService;
import java.util.List;

public interface ChatModelPort {

    /**
     * 基于标准提示词上下文生成回复文本。
     */
    String generateReply(ModelPrompt prompt);

    record ModelPrompt(
            String systemPrompt,
            String memorySummary,
            List<ConversationContextService.ContextLine> context,
            List<KnowledgeService.KnowledgeHit> knowledgeHits,
            String userMessage) {
    }
}
