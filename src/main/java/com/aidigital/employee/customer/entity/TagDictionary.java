package com.aidigital.employee.customer.entity;

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
@TableName("tag_dictionaries")
public class TagDictionary {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String tagCode;
    private String tagName;
    private String ruleKeywords;
    private Instant createdAt;
}
