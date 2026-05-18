package com.aidigital.employee.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.aidigital.employee.customer.Customer;
import com.aidigital.employee.customer.CustomerMemory;
import com.aidigital.employee.customer.CustomerMemoryMapper;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionSummaryService {

    private final ConversationContextService conversationContextService;
    private final SessionService sessionService;
    private final CustomerMemoryMapper customerMemoryMapper;

    public SessionSummaryService(
            ConversationContextService conversationContextService,
            SessionService sessionService,
            CustomerMemoryMapper customerMemoryMapper) {
        this.conversationContextService = conversationContextService;
        this.sessionService = sessionService;
        this.customerMemoryMapper = customerMemoryMapper;
    }

    /**
     * 在回复完成后刷新会话摘要，并同步更新客户长期记忆摘要。
     */
    @Transactional
    public void updateAfterReply(Customer customer, ConversationSession session) {
        String summary = conversationContextService.buildContext(session.getId(), 6, 800).stream()
                .map(line -> line.role() + ":" + line.content())
                .collect(Collectors.joining(" | "));
        sessionService.updateSummary(session, summary);
        CustomerMemory memory = customerMemoryMapper.selectOne(new LambdaQueryWrapper<CustomerMemory>()
                .eq(CustomerMemory::getCustomerId, customer.getId())
                .last("limit 1"));
        if (memory == null) {
            memory = new CustomerMemory().setCustomerId(customer.getId());
            memory.setUpdatedAt(Instant.now()).setSummary(summary).setSource("session-summary");
            customerMemoryMapper.insert(memory);
            return;
        }
        memory.setSummary(summary).setSource("session-summary").setUpdatedAt(Instant.now());
        customerMemoryMapper.updateById(memory);
    }
}
