package com.aidigital.employee.agent.service;

import com.aidigital.employee.agent.entity.ConversationSession;
import com.aidigital.employee.agent.mapper.ConversationSessionMapper;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HumanTakeoverService {

    private final ConversationSessionMapper conversationSessionMapper;

    public HumanTakeoverService(ConversationSessionMapper conversationSessionMapper) {
        this.conversationSessionMapper = conversationSessionMapper;
    }

    /**
     * Switches a conversation to manual handling so the channel keeps receiving messages without AI replies.
     */
    @Transactional
    public ConversationSession takeover(Long sessionId, String reason) {
        ConversationSession session = requireSession(sessionId);
        session.setStatus("MANUAL")
                .setTakeoverReason(reason)
                .setTakeoverAt(Instant.now())
                .setUpdatedAt(Instant.now());
        conversationSessionMapper.updateById(session);
        return session;
    }

    /**
     * Restores AI handling for a manually operated conversation.
     */
    @Transactional
    public ConversationSession resumeAi(Long sessionId) {
        ConversationSession session = requireSession(sessionId);
        session.setStatus("ACTIVE")
                .setTakeoverReason(null)
                .setTakeoverAt(null)
                .setUpdatedAt(Instant.now());
        conversationSessionMapper.updateById(session);
        return session;
    }

    private ConversationSession requireSession(Long sessionId) {
        ConversationSession session = conversationSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session does not exist");
        }
        return session;
    }
}
