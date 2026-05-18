package com.aidigital.employee.agent.service;

import com.aidigital.employee.agent.entity.ConversationSession;
import com.aidigital.employee.agent.mapper.ConversationSessionMapper;
import com.aidigital.employee.customer.entity.Customer;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final ConversationSessionMapper conversationSessionMapper;

    public SessionService(ConversationSessionMapper conversationSessionMapper) {
        this.conversationSessionMapper = conversationSessionMapper;
    }

    /**
     * 复用客户最近活跃会话，若不存在则初始化新会话。
     */
    @Transactional
    public ConversationSession openOrReuse(Customer customer, String channelCode) {
        Instant now = Instant.now();
        ConversationSession session = conversationSessionMapper.selectLatestByCustomerAndChannel(customer.getId(), channelCode);
        if (session == null) {
            return createSession(customer, channelCode, now);
        }
        session.setLastActiveAt(now).setUpdatedAt(now);
        conversationSessionMapper.updateById(session);
        return session;
    }

    /**
     * 查询客户最近活跃的会话列表，供后台和排障场景使用。
     */
    public List<ConversationSession> recentSessions(Long customerId) {
        return conversationSessionMapper.selectRecentByCustomerId(customerId);
    }

    /**
     * 更新会话摘要，供摘要生成与后台查看复用。
     */
    @Transactional
    public void updateSummary(ConversationSession session, String summary) {
        session.setSummary(summary).setUpdatedAt(Instant.now());
        conversationSessionMapper.updateById(session);
    }

    private ConversationSession createSession(Customer customer, String channelCode, Instant now) {
        ConversationSession session = ConversationSession.builder()
                .customerId(customer.getId())
                .channelCode(channelCode)
                .status("ACTIVE")
                .summary("")
                .createdAt(now)
                .updatedAt(now)
                .lastActiveAt(now)
                .build();
        conversationSessionMapper.insert(session);
        return session;
    }
}
