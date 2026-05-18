package com.aidigital.employee.agent;

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
@TableName("conversation_sessions")
public class ConversationSession {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String channelCode;
    private String status;
    private String summary;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastActiveAt;
}
