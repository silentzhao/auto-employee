package com.aidigital.employee.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.aidigital.employee.customer.entity.CustomerTag;
import com.aidigital.employee.customer.mapper.CustomerTagMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerTagService {

    private final CustomerTagMapper customerTagMapper;

    public CustomerTagService(CustomerTagMapper customerTagMapper) {
        this.customerTagMapper = customerTagMapper;
    }

    /**
     * Applies deterministic fallback tags until model-based extraction is enabled.
     */
    @Transactional
    public void extractRuleTags(Long customerId, String text) {
        String content = text == null ? "" : text;
        if (containsAny(content, "预算", "价格", "报价", "多少钱")) {
            upsert(customerId, "BUDGET_SENSITIVE", "预算敏感");
        }
        if (containsAny(content, "人工", "客服", "销售", "转人")) {
            upsert(customerId, "HUMAN_REQUESTED", "要求人工");
        }
        if (containsAny(content, "尽快", "今天", "马上", "采购", "签约")) {
            upsert(customerId, "HIGH_INTENT", "高意向");
        }
    }

    public List<CustomerTag> listByCustomer(Long customerId) {
        return customerTagMapper.selectList(new LambdaQueryWrapper<CustomerTag>()
                .eq(CustomerTag::getCustomerId, customerId)
                .orderByDesc(CustomerTag::getCreatedAt));
    }

    private void upsert(Long customerId, String code, String name) {
        CustomerTag existing = customerTagMapper.selectOne(new LambdaQueryWrapper<CustomerTag>()
                .eq(CustomerTag::getCustomerId, customerId)
                .eq(CustomerTag::getTagCode, code)
                .last("limit 1"));
        if (existing != null) {
            return;
        }
        customerTagMapper.insert(CustomerTag.builder()
                .customerId(customerId)
                .tagCode(code)
                .tagName(name)
                .source("RULE")
                .confidence(new BigDecimal("0.8000"))
                .createdAt(Instant.now())
                .build());
    }

    private boolean containsAny(String content, String... keywords) {
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
