package com.aidigital.employee.channel.dto;

public record StandardInboundMessage(
        String channelCode,
        String externalMessageId,
        String externalUserId,
        String receiverId,
        String tenantId,
        String content,
        String rawPayload) {
}
