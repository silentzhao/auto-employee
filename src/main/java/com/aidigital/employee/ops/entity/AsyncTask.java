package com.aidigital.employee.ops.entity;

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
@TableName("async_tasks")
public class AsyncTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskType;
    private String businessKey;
    private String payload;
    private String status;
    private Integer attempts;
    private Integer maxAttempts;
    private Instant nextRunAt;
    private String lastError;
    private Instant createdAt;
    private Instant updatedAt;
}
