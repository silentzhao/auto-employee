package com.aidigital.employee.infra.storage;

public interface ObjectStoragePort {

    void save(String objectKey, byte[] content);
}
