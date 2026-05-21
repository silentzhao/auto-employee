package com.aidigital.employee.agent.entity;

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
@TableName("follow_up_tasks")
public class FollowUpTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private Long sessionId;
    private String title;
    private String priority;
    private String status;
    private String ownerId;
    private Instant dueAt;
    private Instant createdAt;
    private Instant updatedAt;
}
