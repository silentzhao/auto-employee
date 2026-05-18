package com.aidigital.employee.customer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.aidigital.employee.channel.StandardInboundMessage;
import com.aidigital.employee.common.exception.BusinessException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerIdentityService {

    private static final Logger log = LoggerFactory.getLogger(CustomerIdentityService.class);

    private final CustomerMapper customerMapper;
    private final ChannelAccountMapper channelAccountMapper;

    public CustomerIdentityService(
            CustomerMapper customerMapper,
            ChannelAccountMapper channelAccountMapper) {
        this.customerMapper = customerMapper;
        this.channelAccountMapper = channelAccountMapper;
    }

    /**
     * 按渠道身份识别客户，并在首次触达时建立客户与渠道账号绑定。
     */
    @Transactional
    public Customer resolveCustomer(StandardInboundMessage message) {
        ChannelAccount channelAccount = channelAccountMapper.selectOne(new LambdaQueryWrapper<ChannelAccount>()
                .eq(ChannelAccount::getChannelCode, message.channelCode())
                .eq(ChannelAccount::getExternalUserId, message.externalUserId())
                .last("limit 1"));
        if (channelAccount != null) {
            return customerMapper.selectById(channelAccount.getCustomerId());
        }
        return createCustomerBinding(message);
    }

    /**
     * 在并发建档场景下优先复用已存在客户，避免重复主数据。
     */
    private Customer createCustomerBinding(StandardInboundMessage message) {
        Instant now = Instant.now();
        String externalKey = message.channelCode() + ":" + message.externalUserId();
        Customer customer = Customer.builder()
                .externalKey(externalKey)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            customerMapper.insert(customer);
            ChannelAccount channelAccount = ChannelAccount.builder()
                    .channelCode(message.channelCode())
                    .externalUserId(message.externalUserId())
                    .customerId(customer.getId())
                    .createdAt(now)
                    .build();
            channelAccountMapper.insert(channelAccount);
            return customer;
        } catch (DataIntegrityViolationException ex) {
            log.info("Concurrent customer creation detected for {}", externalKey);
            Customer existingCustomer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                    .eq(Customer::getExternalKey, externalKey)
                    .last("limit 1"));
            if (existingCustomer == null) {
                throw new BusinessException(HttpStatus.CONFLICT, "客户创建冲突");
            }
            return existingCustomer;
        }
    }
}
