package com.aidigital.employee.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("tenant_configs")
public class TenantConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private String configKey;
    private String configValue;
    private Instant updatedAt;
}
