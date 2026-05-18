package com.aidigital.employee.infra.model.adapter;

import com.aidigital.employee.common.config.AppProperties;
import com.aidigital.employee.common.util.Digests;
import com.aidigital.employee.infra.model.port.ChatModelPort;
import com.aidigital.employee.ops.entity.ModelAuditLog;
import com.aidigital.employee.ops.mapper.ModelAuditLogMapper;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class LocalModelAdapter implements ChatModelPort {

    private final AppProperties appProperties;
    private final ModelAuditLogMapper modelAuditLogMapper;

    public LocalModelAdapter(AppProperties appProperties, ModelAuditLogMapper modelAuditLogMapper) {
        this.appProperties = appProperties;
        this.modelAuditLogMapper = modelAuditLogMapper;
    }

    /**
     * 生成可复现的本地模型回复，并记录最小化模型审计日志。
     */
    @Override
    public String generateReply(ModelPrompt prompt) {
        long start = System.currentTimeMillis();
        String assembled = prompt.context().stream()
                .map(item -> item.role() + ":" + item.content())
                .collect(Collectors.joining(" | "));
        String knowledge = prompt.knowledgeHits().stream()
                .map(hit -> hit.documentName() + ":" + hit.content())
                .collect(Collectors.joining(" | "));
        String reply = "已收到你的消息：" + prompt.userMessage()
                + "。结合历史上下文与知识库，建议优先关注：" + truncate(knowledge.isBlank() ? assembled : knowledge, 120);

        ModelAuditLog auditLog = ModelAuditLog.builder()
                .provider(appProperties.getModel().getProvider())
                .modelName(appProperties.getModel().getModelName())
                .promptDigest(Digests.sha256(prompt.userMessage() + assembled + knowledge))
                .responseDigest(Digests.sha256(reply))
                .status("SUCCESS")
                .latencyMs(System.currentTimeMillis() - start)
                .createdAt(Instant.now())
                .build();
        modelAuditLogMapper.insert(auditLog);
        return reply;
    }

    private String truncate(String content, int max) {
        if (content == null || content.isBlank()) {
            return "暂无额外上下文";
        }
        return content.length() <= max ? content : content.substring(0, max) + "...";
    }
}
