package com.aidigital.employee.channel.adapter;

import com.aidigital.employee.channel.dto.StandardInboundMessage;
import com.aidigital.employee.channel.port.ChannelMessageAdapter;
import com.aidigital.employee.common.config.AppProperties;
import com.aidigital.employee.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SimpleWebhookChannelAdapter implements ChannelMessageAdapter {

    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public SimpleWebhookChannelAdapter(ObjectMapper objectMapper, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    /**
     * 解析渠道 JSON 负载并转换为统一消息结构。
     */
    @Override
    public StandardInboundMessage adapt(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String externalMessageId = require(root, "messageId");
            String externalUserId = require(root, "fromUserId");
            String receiverId = require(root, "toUserId");
            String tenantId = require(root, "tenantId");
            String content = require(root, "content");
            return new StandardInboundMessage(
                    appProperties.getChannel().getDefaultChannel(),
                    externalMessageId,
                    externalUserId,
                    receiverId,
                    tenantId,
                    content,
                    payload);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请求体不是合法 JSON");
        }
    }

    /**
     * 强制提取必须字段，避免脏数据进入后续链路。
     */
    private String require(JsonNode node, String field) {
        if (!node.hasNonNull(field) || node.get(field).asText().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "缺少字段: " + field);
        }
        return node.get(field).asText();
    }
}
