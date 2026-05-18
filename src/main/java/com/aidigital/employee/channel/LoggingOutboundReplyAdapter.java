package com.aidigital.employee.channel;

import com.aidigital.employee.agent.ConversationSession;
import com.aidigital.employee.customer.Customer;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingOutboundReplyAdapter implements OutboundReplyPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingOutboundReplyAdapter.class);

    private final ChatMessageMapper chatMessageMapper;

    public LoggingOutboundReplyAdapter(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    /**
     * 将出站回复持久化为消息记录，确保会话链路完整可追溯。
     */
    public void persist(Customer customer, ConversationSession session, String replyContent) {
        ChatMessage message = ChatMessage.builder()
                .externalMessageId("out-" + session.getId() + "-" + Instant.now().toEpochMilli())
                .sessionId(session.getId())
                .customerId(customer.getId())
                .direction("OUTBOUND")
                .senderId("ai-agent")
                .receiverId(customer.getExternalKey())
                .content(replyContent)
                .status("SENT")
                .rawPayloadRef("local-log")
                .createdAt(Instant.now())
                .build();
        chatMessageMapper.insert(message);
    }

    @Override
    public void send(Customer customer, String channelCode, String replyContent) {
        log.info("Dispatching outbound message channel={} customer={} content={}",
                channelCode, customer.getExternalKey(), replyContent);
    }
}
