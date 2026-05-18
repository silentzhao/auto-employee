package com.aidigital.employee.channel;

public interface ChannelMessageAdapter {

    StandardInboundMessage adapt(String payload);
}
