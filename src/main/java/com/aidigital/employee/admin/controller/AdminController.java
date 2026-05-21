package com.aidigital.employee.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.aidigital.employee.admin.entity.Tenant;
import com.aidigital.employee.admin.entity.TenantConfig;
import com.aidigital.employee.admin.service.TenantConfigService;
import com.aidigital.employee.agent.entity.ConversationSession;
import com.aidigital.employee.agent.entity.FollowUpTask;
import com.aidigital.employee.agent.service.FollowUpTaskService;
import com.aidigital.employee.agent.service.HumanTakeoverService;
import com.aidigital.employee.agent.service.SessionService;
import com.aidigital.employee.channel.entity.ChatMessage;
import com.aidigital.employee.channel.mapper.ChatMessageMapper;
import com.aidigital.employee.common.api.ApiResponse;
import com.aidigital.employee.customer.entity.CustomerMemory;
import com.aidigital.employee.customer.entity.CustomerTag;
import com.aidigital.employee.customer.mapper.CustomerMemoryMapper;
import com.aidigital.employee.customer.service.CustomerTagService;
import com.aidigital.employee.knowledge.entity.KnowledgeDocument;
import com.aidigital.employee.knowledge.service.KnowledgeService;
import com.aidigital.employee.ops.entity.AsyncTask;
import com.aidigital.employee.ops.service.AsyncTaskWorker;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    private final HumanTakeoverService humanTakeoverService;
    private final CustomerTagService customerTagService;
    private final FollowUpTaskService followUpTaskService;
    private final TenantConfigService tenantConfigService;
    private final AsyncTaskWorker asyncTaskWorker;

    public AdminController(
            SessionService sessionService,
            ChatMessageMapper chatMessageMapper,
            CustomerMemoryMapper customerMemoryMapper,
            KnowledgeService knowledgeService,
            HumanTakeoverService humanTakeoverService,
            CustomerTagService customerTagService,
            FollowUpTaskService followUpTaskService,
            TenantConfigService tenantConfigService,
            AsyncTaskWorker asyncTaskWorker) {
        this.sessionService = sessionService;
        this.chatMessageMapper = chatMessageMapper;
        this.customerMemoryMapper = customerMemoryMapper;
        this.knowledgeService = knowledgeService;
        this.humanTakeoverService = humanTakeoverService;
        this.customerTagService = customerTagService;
        this.followUpTaskService = followUpTaskService;
        this.tenantConfigService = tenantConfigService;
        this.asyncTaskWorker = asyncTaskWorker;
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

    @GetMapping("/knowledge/documents")
    public ApiResponse<List<KnowledgeDocument>> knowledgeDocuments(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(knowledgeService.listDocuments(tenantId, status));
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

    /**
     * 更新知识库文档状态，支持启用、停用和删除标记。
     */
    @PutMapping("/knowledge/documents/{documentId}/status")
    public ApiResponse<KnowledgeDocument> updateKnowledgeStatus(
            @PathVariable Long documentId,
            @RequestBody KnowledgeStatusRequest request) {
        return ApiResponse.ok(knowledgeService.updateStatus(documentId, request.status()));
    }

    @PostMapping("/knowledge/documents/{documentId}/enable")
    public ApiResponse<KnowledgeDocument> enableKnowledgeDocument(@PathVariable Long documentId) {
        return ApiResponse.ok(knowledgeService.updateStatus(documentId, "ENABLED"));
    }

    @PostMapping("/knowledge/documents/{documentId}/disable")
    public ApiResponse<KnowledgeDocument> disableKnowledgeDocument(@PathVariable Long documentId) {
        return ApiResponse.ok(knowledgeService.updateStatus(documentId, "DISABLED"));
    }

    /**
     * 将会话切换为人工接管状态，暂停后续 AI 自动回复。
     */
    @PostMapping("/sessions/{sessionId}/takeover")
    public ApiResponse<ConversationSession> takeover(@PathVariable Long sessionId, @RequestBody TakeoverRequest request) {
        return ApiResponse.ok(humanTakeoverService.takeover(sessionId, request.reason()));
    }

    @PostMapping("/sessions/{sessionId}/handover")
    public ApiResponse<ConversationSession> handover(@PathVariable Long sessionId, @RequestBody TakeoverRequest request) {
        return ApiResponse.ok(humanTakeoverService.takeover(sessionId, request.reason()));
    }

    /**
     * 恢复指定会话的 AI 自动接管能力。
     */
    @PostMapping("/sessions/{sessionId}/resume-ai")
    public ApiResponse<ConversationSession> resumeAi(@PathVariable Long sessionId) {
        return ApiResponse.ok(humanTakeoverService.resumeAi(sessionId));
    }

    /**
     * 查询客户自动标签，辅助销售判断意向和风险。
     */
    @GetMapping("/customers/{customerId}/tags")
    public ApiResponse<List<CustomerTag>> customerTags(@PathVariable Long customerId) {
        return ApiResponse.ok(customerTagService.listByCustomer(customerId));
    }

    /**
     * 查询待处理跟进任务，可按客户过滤。
     */
    @GetMapping("/follow-up-tasks")
    public ApiResponse<List<FollowUpTask>> followUpTasks(@RequestParam(required = false) Long customerId) {
        return ApiResponse.ok(followUpTaskService.listOpen(customerId));
    }

    @GetMapping("/follow-ups")
    public ApiResponse<List<FollowUpTask>> followUps(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(followUpTaskService.list(customerId, status));
    }

    @PostMapping("/follow-ups/{taskId}/complete")
    public ApiResponse<FollowUpTask> completeFollowUp(@PathVariable Long taskId) {
        return ApiResponse.ok(followUpTaskService.complete(taskId));
    }

    @PostMapping("/follow-ups/{taskId}/cancel")
    public ApiResponse<FollowUpTask> cancelFollowUp(@PathVariable Long taskId) {
        return ApiResponse.ok(followUpTaskService.cancel(taskId));
    }

    /**
     * 创建或更新租户主数据。
     */
    @PutMapping("/tenants/{tenantId}")
    public ApiResponse<Tenant> upsertTenant(@PathVariable String tenantId, @RequestBody TenantUpsertRequest request) {
        return ApiResponse.ok(tenantConfigService.upsertTenant(tenantId, request.name(), request.status()));
    }

    /**
     * 创建或更新租户级配置项。
     */
    @PutMapping("/tenants/{tenantId}/configs/{key}")
    public ApiResponse<TenantConfig> upsertTenantConfig(
            @PathVariable String tenantId,
            @PathVariable String key,
            @RequestBody TenantConfigRequest request) {
        return ApiResponse.ok(tenantConfigService.upsertConfig(tenantId, key, request.value()));
    }

    /**
     * 查询租户配置快照，供运行时开关和交付排障使用。
     */
    @GetMapping("/tenants/{tenantId}/configs")
    public ApiResponse<List<TenantConfig>> tenantConfigs(@PathVariable String tenantId) {
        return ApiResponse.ok(tenantConfigService.listConfigs(tenantId));
    }

    @GetMapping("/tenants/{tenantId}/config")
    public ApiResponse<Map<String, Object>> tenantConfigSnapshot(@PathVariable String tenantId) {
        return ApiResponse.ok(tenantConfigService.snapshot(tenantId));
    }

    @PutMapping("/tenants/{tenantId}/config")
    public ApiResponse<Map<String, Object>> updateTenantConfigSnapshot(
            @PathVariable String tenantId,
            @RequestBody Map<String, Object> request) {
        return ApiResponse.ok(tenantConfigService.upsertSnapshot(tenantId, request));
    }

    /**
     * 查询失败异步任务，用于恢复和售后排障。
     */
    @GetMapping("/async-tasks/failures")
    public ApiResponse<List<AsyncTask>> failedTasks() {
        return ApiResponse.ok(asyncTaskWorker.listFailures());
    }

    @GetMapping("/tasks")
    public ApiResponse<List<AsyncTask>> tasks(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(asyncTaskWorker.listTasks(type, status));
    }

    /**
     * 将失败任务重新放回可执行队列。
     */
    @PostMapping("/async-tasks/{taskId}/retry")
    public ApiResponse<AsyncTask> retryTask(@PathVariable Long taskId) {
        return ApiResponse.ok(asyncTaskWorker.retry(taskId));
    }

    @PostMapping("/tasks/{taskId}/retry")
    public ApiResponse<AsyncTask> retryTaskAlias(@PathVariable Long taskId) {
        return ApiResponse.ok(asyncTaskWorker.retry(taskId));
    }

    public record MemoryUpsertRequest(
            String summary,
            String preferences,
            String budget,
            String intent,
            String concerns,
            String commitments) {
    }

    public record TakeoverRequest(String reason) {
    }

    public record TenantUpsertRequest(String name, String status) {
    }

    public record TenantConfigRequest(String value) {
    }

    public record KnowledgeStatusRequest(String status) {
    }
}
   RabbitMQ AMQP  端口号：35672
   RabbitMQ 管理台 端口号：35672