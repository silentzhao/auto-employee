package com.aidigital.employee.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.aidigital.employee.agent.entity.ConversationSession;
import com.aidigital.employee.agent.service.SessionService;
import com.aidigital.employee.channel.entity.ChatMessage;
import com.aidigital.employee.channel.mapper.ChatMessageMapper;
import com.aidigital.employee.common.api.ApiResponse;
import com.aidigital.employee.customer.entity.CustomerMemory;
import com.aidigital.employee.customer.mapper.CustomerMemoryMapper;
import com.aidigital.employee.knowledge.entity.KnowledgeDocument;
import com.aidigital.employee.knowledge.service.KnowledgeService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SessionService sessionService;
    private final ChatMessageMapper chatMessageMapper;
    private final CustomerMemoryMapper customerMemoryMapper;
    private final KnowledgeService knowledgeService;

    public AdminController(
            SessionService sessionService,
            ChatMessageMapper chatMessageMapper,
            CustomerMemoryMapper customerMemoryMapper,
            KnowledgeService knowledgeService) {
        this.sessionService = sessionService;
        this.chatMessageMapper = chatMessageMapper;
        this.customerMemoryMapper = customerMemoryMapper;
        this.knowledgeService = knowledgeService;
    }

    /**
     * 查询客户最近会话列表，供后台管理端查看跟进状态。
     */
    @GetMapping("/customers/{customerId}/sessions")
    public ApiResponse<List<ConversationSession>> sessions(@PathVariable Long customerId) {
        return ApiResponse.ok(sessionService.recentSessions(customerId));
    }

    /**
     * 查询指定会话最近消息，用于回放对话链路。
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<ChatMessage>> messages(@PathVariable Long sessionId) {
        return ApiResponse.ok(chatMessageMapper.selectRecentBySessionId(sessionId, 20));
    }

    /**
     * 查询客户长期记忆快照。
     */
    @GetMapping("/customers/{customerId}/memory")
    public ApiResponse<CustomerMemory> memory(@PathVariable Long customerId) {
        return ApiResponse.ok(customerMemoryMapper.selectOne(new LambdaQueryWrapper<CustomerMemory>()
                .eq(CustomerMemory::getCustomerId, customerId)
                .last("limit 1")));
    }

    /**
     * 更新客户长期记忆，支持后台人工修正画像信息。
     */
    @PutMapping("/customers/{customerId}/memory")
    public ApiResponse<CustomerMemory> updateMemory(@PathVariable Long customerId, @Valid @RequestBody MemoryUpsertRequest request) {
        CustomerMemory memory = customerMemoryMapper.selectOne(new LambdaQueryWrapper<CustomerMemory>()
                .eq(CustomerMemory::getCustomerId, customerId)
                .last("limit 1"));
        if (memory == null) {
            throw new IllegalArgumentException("客户记忆不存在");
        }
        memory.setSummary(request.summary())
                .setPreferences(request.preferences())
                .setBudget(request.budget())
                .setIntent(request.intent())
                .setConcerns(request.concerns())
                .setCommitments(request.commitments())
                .setSource("manual-admin")
                .setUpdatedAt(Instant.now());
        customerMemoryMapper.updateById(memory);
        return ApiResponse.ok(memory);
    }

    /**
     * 上传知识文档并完成解析、切片与索引。
     */
    @PostMapping("/knowledge/documents")
    public ApiResponse<KnowledgeDocument> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "default-tenant") String tenantId) {
        return ApiResponse.ok(knowledgeService.uploadAndIndex(tenantId, file));
    }

    /**
     * 按租户检索知识切片，验证知识召回效果。
     */
    @GetMapping("/knowledge/search")
    public ApiResponse<List<KnowledgeService.KnowledgeHit>> search(
            @RequestParam String tenantId,
            @RequestParam String question,
            @RequestParam(defaultValue = "3") int topK) {
        return ApiResponse.ok(knowledgeService.search(tenantId, question, topK));
    }

    public record MemoryUpsertRequest(
            String summary,
            String preferences,
            String budget,
            String intent,
            String concerns,
            String commitments) {
    }
}
