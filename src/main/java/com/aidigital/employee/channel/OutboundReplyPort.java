package com.aidigital.employee.channel;

import com.aidigital.employee.customer.Customer;

public interface OutboundReplyPort {

    void send(Customer customer, String channelCode, String replyContent);
}
