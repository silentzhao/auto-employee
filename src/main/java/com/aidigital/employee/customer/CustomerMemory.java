package com.aidigital.employee.customer;

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
@TableName("customer_memories")
public class CustomerMemory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String summary;
    private String preferences;
    private String budget;
    private String intent;
    private String concerns;
    private String commitments;
    private String source;
    private Instant updatedAt;
}
