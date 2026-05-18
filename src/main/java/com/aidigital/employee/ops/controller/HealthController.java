package com.aidigital.employee.ops.controller;

import com.aidigital.employee.common.api.ApiResponse;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ops")
public class HealthController {

    /**
     * 提供最小健康检查信号，便于部署探针与本地联调。
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of("status", "UP", "timestamp", Instant.now().toString()));
    }
}
