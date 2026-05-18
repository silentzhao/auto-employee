package com.aidigital.employee.infra.model.adapter;

import com.aidigital.employee.infra.model.port.EmbeddingPort;
import org.springframework.stereotype.Component;

@Component
public class HashEmbeddingAdapter implements EmbeddingPort {

    /**
     * 使用确定性哈希生成简化向量，满足 MVP 本地检索需要。
     */
    @Override
    public float[] embed(String text) {
        float[] vector = new float[16];
        String normalized = text == null ? "" : text.toLowerCase();
        for (String token : normalized.split("\\s+|,|，|。|！|？")) {
            if (token.isBlank()) {
                continue;
            }
            int slot = Math.abs(token.hashCode()) % vector.length;
            vector[slot] += 1.0F;
        }
        return vector;
    }
}
