package com.aidigital.employee.infra.storage;

import com.aidigital.employee.common.config.AppProperties;
import com.aidigital.employee.common.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class LocalObjectStorage implements ObjectStoragePort {

    private final Path basePath;

    public LocalObjectStorage(AppProperties appProperties) {
        this.basePath = Path.of(appProperties.getStorage().getBasePath());
    }

    @Override
    public void save(String objectKey, byte[] content) {
        try {
            Path target = basePath.resolve(objectKey);
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "对象存储写入失败");
        }
    }
}
