package com.aidigital.employee.ops.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.aidigital.employee.common.api.ApiResponse;
import com.aidigital.employee.common.config.AppProperties;
import com.aidigital.employee.ops.entity.AsyncTask;
import com.aidigital.employee.ops.mapper.AsyncTaskMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ops")
public class HealthController {

    private final AsyncTaskMapper asyncTaskMapper;
    private final AppProperties appProperties;

    public HealthController(AsyncTaskMapper asyncTaskMapper, AppProperties appProperties) {
        this.asyncTaskMapper = asyncTaskMapper;
        this.appProperties = appProperties;
    }

    /**
     * 提供最小健康检查信号，便于部署探针与本地联调。
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of("status", "UP", "timestamp", Instant.now().toString()));
    }

    @GetMapping("/metrics/summary")
    public ApiResponse<Map<String, Object>> metricsSummary() {
        long pending = countTasks("PENDING") + countTasks("RETRY");
        long failed = countTasks("FAILED");
        long succeeded = countTasks("SUCCEEDED");
        return ApiResponse.ok(Map.of(
                "timestamp", Instant.now().toString(),
                "asyncTaskPending", pending,
                "asyncTaskFailed", failed,
                "asyncTaskSucceeded", succeeded,
                "workerEnabled", appProperties.getWorker().isEnabled(),
                "rabbitEnabled", appProperties.getAsync().isRabbitEnabled()));
    }

    @GetMapping("/queues")
    public ApiResponse<List<Map<String, Object>>> queues() {
        long pending = countTasks("PENDING") + countTasks("RETRY");
        long failed = countTasks("FAILED");
        return ApiResponse.ok(List.of(Map.of(
                "name", appProperties.getAsync().getRoutingKey(),
                "exchange", appProperties.getAsync().getExchange(),
                "pending", pending,
                "failed", failed,
                "rabbitEnabled", appProperties.getAsync().isRabbitEnabled())));
    }

    private long countTasks(String status) {
        return asyncTaskMapper.selectCount(new LambdaQueryWrapper<AsyncTask>()
                .eq(AsyncTask::getStatus, status));
    }
}
