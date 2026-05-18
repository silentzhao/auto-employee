package com.aidigital.employee.ops;

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
@TableName("model_audit_logs")
public class ModelAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String provider;
    private String modelName;
    private String promptDigest;
    private String responseDigest;
    private String status;
    private long latencyMs;
    private String errorMessage;
    private Instant createdAt;
}
