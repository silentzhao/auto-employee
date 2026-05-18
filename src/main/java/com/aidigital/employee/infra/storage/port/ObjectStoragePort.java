package com.aidigital.employee.infra.storage.port;

public interface ObjectStoragePort {

    /**
     * 按对象键保存原始内容。
     */
    void save(String objectKey, byte[] content);
}
