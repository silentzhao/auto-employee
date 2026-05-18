package com.aidigital.employee.infra.model;

public interface EmbeddingPort {

    float[] embed(String text);
}
