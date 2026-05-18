package com.aidigital.employee.infra.model;

import com.aidigital.employee.agent.ConversationContextService;
import com.aidigital.employee.knowledge.KnowledgeService;
import java.util.List;

public interface ChatModelPort {

    String generateReply(ModelPrompt prompt);

    record ModelPrompt(
            String systemPrompt,
            String memorySummary,
            List<ConversationContextService.ContextLine> context,
            List<KnowledgeService.KnowledgeHit> knowledgeHits,
            String userMessage) {
    }
}
