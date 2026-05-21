package com.aidigital.employee.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
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
@TableName("customer_tags")
public class CustomerTag {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String tagCode;
    private String tagName;
    private String source;
    private BigDecimal confidence;
    private Instant createdAt;
}
