package com.aidigital.employee.ops.config;

import com.aidigital.employee.common.config.AppProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.async", name = "rabbit-enabled", havingValue = "true")
public class RabbitTaskQueueConfig {

    @Bean
    DirectExchange employeeTaskExchange(AppProperties appProperties) {
        return new DirectExchange(appProperties.getAsync().getExchange(), true, false);
    }

    @Bean
    Queue employeeTaskQueue() {
        return new Queue("employee.async.tasks", true);
    }

    @Bean
    Binding employeeTaskBinding(DirectExchange employeeTaskExchange, Queue employeeTaskQueue, AppProperties appProperties) {
        return BindingBuilder.bind(employeeTaskQueue)
                .to(employeeTaskExchange)
                .with(appProperties.getAsync().getRoutingKey());
    }
}
