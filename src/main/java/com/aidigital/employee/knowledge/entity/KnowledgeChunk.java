package com.aidigital.employee.knowledge.entity;

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
@TableName("knowledge_chunks")
public class KnowledgeChunk {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private String tenantId;
    private int chunkIndex;
    private String content;
    private String vectorData;
    private String status;
    private Instant createdAt;
}
