package com.aidigital.employee.common.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Channel channel = new Channel();
    private final Knowledge knowledge = new Knowledge();
    private final Storage storage = new Storage();
    private final Model model = new Model();
    private final Async async = new Async();
    private final Worker worker = new Worker();

    public Channel getChannel() {
        return channel;
    }

    public Knowledge getKnowledge() {
        return knowledge;
    }

    public Storage getStorage() {
        return storage;
    }

    public Model getModel() {
        return model;
    }

    public Async getAsync() {
        return async;
    }

    public Worker getWorker() {
        return worker;
    }

    public static class Channel {
        private String webhookSecret;
        private String defaultChannel;

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public String getDefaultChannel() {
            return defaultChannel;
        }

        public void setDefaultChannel(String defaultChannel) {
            this.defaultChannel = defaultChannel;
        }
    }

    public static class Knowledge {
        private long maxFileSizeBytes;
        private List<String> allowedTypes = new ArrayList<>();

        public long getMaxFileSizeBytes() {
            return maxFileSizeBytes;
        }

        public void setMaxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = maxFileSizeBytes;
        }

        public List<String> getAllowedTypes() {
            return allowedTypes;
        }

        public void setAllowedTypes(List<String> allowedTypes) {
            this.allowedTypes = allowedTypes;
        }
    }

    public static class Storage {
        private String basePath;

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }
    }

    public static class Model {
        private String provider;
        private String modelName;
        private int timeoutMillis;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public int getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }
    }

    public static class Async {
        private boolean rabbitEnabled;
        private String exchange;
        private String routingKey;
        private int maxAttempts = 3;
        private int pollSize = 10;

        public boolean isRabbitEnabled() {
            return rabbitEnabled;
        }

        public void setRabbitEnabled(boolean rabbitEnabled) {
            this.rabbitEnabled = rabbitEnabled;
        }

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getPollSize() {
            return pollSize;
        }

        public void setPollSize(int pollSize) {
            this.pollSize = pollSize;
        }
    }

    public static class Worker {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
