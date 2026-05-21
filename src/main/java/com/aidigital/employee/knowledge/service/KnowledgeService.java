package com.aidigital.employee.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.aidigital.employee.common.config.AppProperties;
import com.aidigital.employee.common.exception.BusinessException;
import com.aidigital.employee.infra.model.port.EmbeddingPort;
import com.aidigital.employee.infra.storage.port.ObjectStoragePort;
import com.aidigital.employee.knowledge.entity.KnowledgeChunk;
import com.aidigital.employee.knowledge.entity.KnowledgeDocument;
import com.aidigital.employee.knowledge.mapper.KnowledgeChunkMapper;
import com.aidigital.employee.knowledge.mapper.KnowledgeDocumentMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final AppProperties appProperties;
    private final ObjectStoragePort objectStoragePort;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final EmbeddingPort embeddingPort;

    public KnowledgeService(
            AppProperties appProperties,
            ObjectStoragePort objectStoragePort,
            KnowledgeDocumentMapper knowledgeDocumentMapper,
            KnowledgeChunkMapper knowledgeChunkMapper,
            EmbeddingPort embeddingPort) {
        this.appProperties = appProperties;
        this.objectStoragePort = objectStoragePort;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.embeddingPort = embeddingPort;
    }

    /**
     * 完成知识文档上传、对象存储落地和同步切片索引。
     */
    @Transactional
    public KnowledgeDocument uploadAndIndex(String tenantId, MultipartFile file) {
        validateFile(file);
        Instant now = Instant.now();
        String objectKey = tenantId + "/" + now.toEpochMilli() + "-" + file.getOriginalFilename();
        KnowledgeDocument document = new KnowledgeDocument();
        document.setTenantId(tenantId);
        document.setFileName(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document.setSizeBytes(file.getSize());
        document.setObjectKey(objectKey);
        document.setStatus("UPLOADED");
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        knowledgeDocumentMapper.insert(document);

        try {
            objectStoragePort.save(objectKey, file.getBytes());
            String content = extractText(file);
            indexDocument(document, content);
            document.setStatus("INDEXED");
            document.setUpdatedAt(Instant.now());
            knowledgeDocumentMapper.updateById(document);
            return document;
        } catch (Exception ex) {
            log.error("Knowledge upload/index failed for {}", file.getOriginalFilename(), ex);
            document.setStatus("FAILED");
            document.setFailureReason(ex.getMessage());
            document.setUpdatedAt(Instant.now());
            knowledgeDocumentMapper.updateById(document);
            return document;
        }
    }

    /**
     * 按租户检索知识片段，并返回可直接参与提示词拼装的结果。
     */
    public List<KnowledgeHit> search(String tenantId, String question, int topK) {
        float[] queryVector = embeddingPort.embed(question);
        List<KnowledgeChunk> chunks = knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getTenantId, tenantId)
                .eq(KnowledgeChunk::getStatus, "READY"));
        Map<Long, KnowledgeDocument> documents = loadDocuments(chunks);
        return chunks.stream()
                .filter(chunk -> {
                    KnowledgeDocument document = documents.get(chunk.getDocumentId());
                    return document != null && ("INDEXED".equals(document.getStatus()) || "ENABLED".equals(document.getStatus()));
                })
                .map(chunk -> new KnowledgeHit(
                        chunk.getDocumentId(),
                        documents.getOrDefault(chunk.getDocumentId(), new KnowledgeDocument()).getFileName(),
                        chunk.getContent(),
                        cosine(queryVector, deserialize(chunk.getVectorData()))))
                .sorted(Comparator.comparingDouble(KnowledgeHit::score).reversed())
                .limit(topK)
                .toList();
    }

    public List<KnowledgeDocument> listDocuments(String tenantId, String status) {
        return knowledgeDocumentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(tenantId != null && !tenantId.isBlank(), KnowledgeDocument::getTenantId, tenantId)
                .eq(status != null && !status.isBlank(), KnowledgeDocument::getStatus, status)
                .orderByDesc(KnowledgeDocument::getUpdatedAt));
    }

    /**
     * Updates document visibility for paid tenant delivery without deleting traceable source data.
     */
    @Transactional
    public KnowledgeDocument updateStatus(Long documentId, String status) {
        KnowledgeDocument document = knowledgeDocumentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "知识库文档不存在");
        }
        document.setStatus(status).setUpdatedAt(Instant.now());
        knowledgeDocumentMapper.updateById(document);
        List<KnowledgeChunk> chunks = knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getDocumentId, documentId));
        for (KnowledgeChunk chunk : chunks) {
            chunk.setStatus("DISABLED".equals(status) || "DELETED".equals(status) ? "DISABLED" : "READY");
            knowledgeChunkMapper.updateById(chunk);
        }
        return document;
    }

    /**
     * 校验上传文件的体积与类型，避免异常文档进入索引流程。
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "上传文件不能为空");
        }
        if (file.getSize() > appProperties.getKnowledge().getMaxFileSizeBytes()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "文件大小超限");
        }
        String contentType = file.getContentType();
        if (contentType == null || !appProperties.getKnowledge().getAllowedTypes().contains(contentType)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "文件类型不支持");
        }
    }

    /**
     * 从不同文档格式中抽取纯文本内容，供切片与向量化使用。
     */
    private String extractText(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        byte[] bytes = file.getBytes();
        if ("text/plain".equals(contentType) || "text/markdown".equals(contentType)) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if ("application/pdf".equals(contentType)) {
            try (var document = Loader.loadPDF(bytes)) {
                return new PDFTextStripper().getText(document);
            }
        }
        if ("application/msword".equals(contentType)) {
            try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes));
                 WordExtractor extractor = new WordExtractor(document)) {
                return extractor.getText();
            }
        }
        if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                return extractor.getText();
            }
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "文件类型不支持解析");
    }

    /**
     * 将文档内容切片并生成向量索引。
     */
    private void indexDocument(KnowledgeDocument document, String content) {
        if (content.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "文档内容为空");
        }
        List<String> chunks = split(content, 300);
        for (int index = 0; index < chunks.size(); index++) {
            KnowledgeChunk chunk = KnowledgeChunk.builder()
                    .documentId(document.getId())
                    .tenantId(document.getTenantId())
                    .chunkIndex(index)
                    .content(chunks.get(index))
                    .vectorData(serialize(embeddingPort.embed(chunks.get(index))))
                    .status("READY")
                    .createdAt(Instant.now())
                    .build();
            knowledgeChunkMapper.insert(chunk);
        }
    }

    private Map<Long, KnowledgeDocument> loadDocuments(List<KnowledgeChunk> chunks) {
        Map<Long, KnowledgeDocument> documents = new HashMap<>();
        List<Long> documentIds = chunks.stream().map(KnowledgeChunk::getDocumentId).distinct().toList();
        if (documentIds.isEmpty()) {
            return documents;
        }
        for (KnowledgeDocument document : knowledgeDocumentMapper.selectBatchIds(documentIds)) {
            documents.put(document.getId(), document);
        }
        return documents;
    }

    private List<String> split(String content, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < content.length(); start += chunkSize) {
            chunks.add(content.substring(start, Math.min(content.length(), start + chunkSize)));
        }
        return chunks;
    }

    private String serialize(float[] vector) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(vector[i]);
        }
        return builder.toString();
    }

    private float[] deserialize(String serialized) {
        String[] parts = serialized.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }
        return vector;
    }

    private double cosine(float[] left, float[] right) {
        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        return leftNorm == 0 || rightNorm == 0 ? 0D : dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    public record KnowledgeHit(Long documentId, String documentName, String content, double score) {
    }
}
