package com.aidigital.employee.channel.controller;

import com.aidigital.employee.channel.dto.StandardInboundMessage;
import com.aidigital.employee.channel.port.ChannelMessageAdapter;
import com.aidigital.employee.common.api.ApiResponse;
import com.aidigital.employee.common.config.AppProperties;
import com.aidigital.employee.common.exception.BusinessException;
import com.aidigital.employee.common.util.Digests;
import com.aidigital.employee.channel.service.ChannelMessageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/channel/webhook")
public class ChannelWebhookController {

    private final AppProperties appProperties;
    private final ChannelMessageAdapter channelMessageAdapter;
    private final ChannelMessageService channelMessageService;

    public ChannelWebhookController(
            AppProperties appProperties,
            ChannelMessageAdapter channelMessageAdapter,
            ChannelMessageService channelMessageService) {
        this.appProperties = appProperties;
        this.channelMessageAdapter = channelMessageAdapter;
        this.channelMessageService = channelMessageService;
    }

    /**
     * 接收渠道 Webhook 原始消息，完成签名校验与标准化入站处理。
     */
    @PostMapping
    public ApiResponse<ChannelMessageService.ProcessResult> inbound(
            @RequestHeader("X-Signature") String signature,
            @RequestBody String payload) {
        String expected = "sha256=" + Digests.hmacSha256(appProperties.getChannel().getWebhookSecret(), payload);
        if (!expected.equals(signature)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "签名校验失败");
        }
        StandardInboundMessage message = channelMessageAdapter.adapt(payload);
        return ApiResponse.ok(channelMessageService.process(message));
    }
}
