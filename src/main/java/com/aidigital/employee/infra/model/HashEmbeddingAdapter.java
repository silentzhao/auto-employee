package com.aidigital.employee.infra.model;

import org.springframework.stereotype.Component;

@Component
public class HashEmbeddingAdapter implements EmbeddingPort {

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
