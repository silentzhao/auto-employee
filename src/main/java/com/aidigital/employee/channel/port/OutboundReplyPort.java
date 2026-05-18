package com.aidigital.employee.channel.port;

import com.aidigital.employee.agent.entity.ConversationSession;
import com.aidigital.employee.customer.entity.Customer;

public interface OutboundReplyPort {

    /**
     * 将 AI 回复发送到指定渠道。
     */
    void send(Customer customer, String channelCode, String replyContent);

    /**
     * 将出站回复落库，补齐消息链路审计信息。
     */
    void persist(Customer customer, ConversationSession session, String replyContent);
}
