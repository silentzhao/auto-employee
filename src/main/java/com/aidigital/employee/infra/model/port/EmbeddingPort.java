package com.aidigital.employee.infra.model.port;

public interface EmbeddingPort {

    /**
     * 将文本编码为定长向量，供知识检索相似度计算使用。
     */
    float[] embed(String text);
}
