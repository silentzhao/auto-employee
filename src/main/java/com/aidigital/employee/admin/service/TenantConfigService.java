package com.aidigital.employee.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.aidigital.employee.admin.entity.Tenant;
import com.aidigital.employee.admin.entity.TenantConfig;
import com.aidigital.employee.admin.mapper.TenantConfigMapper;
import com.aidigital.employee.admin.mapper.TenantMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantConfigService {

    private final TenantMapper tenantMapper;
    private final TenantConfigMapper tenantConfigMapper;

    public TenantConfigService(TenantMapper tenantMapper, TenantConfigMapper tenantConfigMapper) {
        this.tenantMapper = tenantMapper;
        this.tenantConfigMapper = tenantConfigMapper;
    }

    /**
     * Creates or updates tenant metadata used by paid delivery configuration.
     */
    @Transactional
    public Tenant upsertTenant(String tenantId, String name, String status) {
        Tenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getTenantId, tenantId)
                .last("limit 1"));
        Instant now = Instant.now();
        if (tenant == null) {
            tenant = Tenant.builder()
                    .tenantId(tenantId)
                    .name(name)
                    .status(status == null ? "ACTIVE" : status)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            tenantMapper.insert(tenant);
            return tenant;
        }
        tenant.setName(name).setStatus(status == null ? tenant.getStatus() : status).setUpdatedAt(now);
        tenantMapper.updateById(tenant);
        return tenant;
    }

    @Transactional
    public TenantConfig upsertConfig(String tenantId, String key, String value) {
        TenantConfig config = tenantConfigMapper.selectOne(new LambdaQueryWrapper<TenantConfig>()
                .eq(TenantConfig::getTenantId, tenantId)
                .eq(TenantConfig::getConfigKey, key)
                .last("limit 1"));
        if (config == null) {
            config = TenantConfig.builder()
                    .tenantId(tenantId)
                    .configKey(key)
                    .configValue(value)
                    .updatedAt(Instant.now())
                    .build();
            tenantConfigMapper.insert(config);
            return config;
        }
        config.setConfigValue(value).setUpdatedAt(Instant.now());
        tenantConfigMapper.updateById(config);
        return config;
    }

    public List<TenantConfig> listConfigs(String tenantId) {
        return tenantConfigMapper.selectList(new LambdaQueryWrapper<TenantConfig>()
                .eq(TenantConfig::getTenantId, tenantId)
                .orderByAsc(TenantConfig::getConfigKey));
    }

    public Map<String, Object> snapshot(String tenantId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("modelProvider", "default");
        snapshot.put("promptTemplate", "standard-sales-assistant");
        snapshot.put("channelEnabled", true);
        snapshot.put("knowledgeEnabled", true);
        snapshot.put("asyncSummaryEnabled", true);
        snapshot.put("autoTagEnabled", true);
        for (TenantConfig config : listConfigs(tenantId)) {
            snapshot.put(config.getConfigKey(), parseValue(config.getConfigValue()));
        }
        return snapshot;
    }

    @Transactional
    public Map<String, Object> upsertSnapshot(String tenantId, Map<String, Object> values) {
        values.forEach((key, value) -> {
            if (!"operator".equals(key)) {
                upsertConfig(tenantId, key, value == null ? null : String.valueOf(value));
            }
        });
        return snapshot(tenantId);
    }

    private Object parseValue(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return value;
    }
}
