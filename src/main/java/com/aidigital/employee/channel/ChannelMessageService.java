package com.aidigital.employee.channel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.aidigital.employee.agent.AiReplyOrchestrator;
import com.aidigital.employee.agent.ConversationSession;
import com.aidigital.employee.agent.SessionService;
import com.aidigital.employee.agent.SessionSummaryService;
import com.aidigital.employee.common.exception.BusinessException;
import com.aidigital.employee.common.util.Digests;
import com.aidigital.employee.customer.Customer;
import com.aidigital.employee.customer.CustomerIdentityService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelMessageService {

    private static final Logger log = LoggerFactory.getLogger(ChannelMessageService.class);

    private final ChatMessageMapper chatMessageMapper;
    private final CustomerIdentityService customerIdentityService;
    private final SessionService sessionService;
    private final AiReplyOrchestrator aiReplyOrchestrator;
    private final LoggingOutboundReplyAdapter outboundReplyAdapter;
    private final SessionSummaryService sessionSummaryService;

    public ChannelMessageService(
            ChatMessageMapper chatMessageMapper,
            CustomerIdentityService customerIdentityService,
            SessionService sessionService,
            AiReplyOrchestrator aiReplyOrchestrator,
            LoggingOutboundReplyAdapter outboundReplyAdapter,
            SessionSummaryService sessionSummaryService) {
        this.chatMessageMapper = chatMessageMapper;
        this.customerIdentityService = customerIdentityService;
        this.sessionService = sessionService;
        this.aiReplyOrchestrator = aiReplyOrchestrator;
        this.outboundReplyAdapter = outboundReplyAdapter;
        this.sessionSummaryService = sessionSummaryService;
    }

    /**
     * 处理渠道入站消息，完成幂等、会话绑定、落库、回复与摘要更新。
     */
    @Transactional
    public ProcessResult process(StandardInboundMessage inboundMessage) {
        ChatMessage duplicatedMessage = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getExternalMessageId, inboundMessage.externalMessageId())
                .last("limit 1"));
        if (duplicatedMessage != null) {
            return new ProcessResult(true, "消息重复，已忽略");
        }
        Customer customer = customerIdentityService.resolveCustomer(inboundMessage);
        ConversationSession session = sessionService.openOrReuse(customer, inboundMessage.channelCode());
        ChatMessage inbound = ChatMessage.builder()
                .externalMessageId(inboundMessage.externalMessageId())
                .sessionId(session.getId())
                .customerId(customer.getId())
                .direction("INBOUND")
                .senderId(inboundMessage.externalUserId())
                .receiverId(inboundMessage.receiverId())
                .content(inboundMessage.content())
                .status("RECEIVED")
                .rawPayloadDigest(Digests.sha256(inboundMessage.rawPayload()))
                .rawPayloadRef("webhook")
                .createdAt(Instant.now())
                .build();
        chatMessageMapper.insert(inbound);

        String reply;
        try {
            reply = aiReplyOrchestrator.generateReply(customer.getId(), session.getId(), inboundMessage.tenantId(), inboundMessage.content());
        } catch (Exception ex) {
            log.error("Model orchestration failed", ex);
            reply = "当前系统繁忙，请稍后再试。";
        }
        if (reply == null || reply.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "模型返回空响应");
        }
        outboundReplyAdapter.send(customer, inboundMessage.channelCode(), reply);
        outboundReplyAdapter.persist(customer, session, reply);
        sessionSummaryService.updateAfterReply(customer, session);
        return new ProcessResult(false, reply);
    }

    public record ProcessResult(boolean duplicated, String result) {
    }
}
