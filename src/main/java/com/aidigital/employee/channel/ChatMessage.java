package com.aidigital.employee.channel;

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
@TableName("chat_messages")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String externalMessageId;
    private Long sessionId;
    private Long customerId;
    private String direction;
    private String senderId;
    private String receiverId;
    private String content;
    private String status;
    private String rawPayloadDigest;
    private String rawPayloadRef;
    private Instant createdAt;
}
