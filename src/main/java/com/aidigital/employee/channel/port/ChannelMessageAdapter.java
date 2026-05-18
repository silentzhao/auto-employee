package com.aidigital.employee.channel.port;

import com.aidigital.employee.channel.dto.StandardInboundMessage;

public interface ChannelMessageAdapter {

    /**
     * 将渠道原始报文转换为系统统一入站消息模型。
     */
    StandardInboundMessage adapt(String payload);
}
