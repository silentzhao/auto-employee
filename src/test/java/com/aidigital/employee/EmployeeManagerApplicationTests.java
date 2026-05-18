package com.aidigital.employee;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.aidigital.employee.channel.ChatMessageMapper;
import com.aidigital.employee.common.util.Digests;
import com.aidigital.employee.customer.CustomerMemoryMapper;
import com.aidigital.employee.knowledge.KnowledgeChunkMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeManagerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private CustomerMemoryMapper customerMemoryMapper;

    @Autowired
    private KnowledgeChunkMapper knowledgeChunkMapper;

    @Test
    void contextLoads() {
        assertThat(mockMvc).isNotNull();
    }

    @Test
    void healthEndpointWorks() throws Exception {
        mockMvc.perform(get("/api/ops/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void webhookSupportsNormalDuplicateAndInvalidSignature() throws Exception {
        String payload = """
                {"messageId":"m-1","fromUserId":"u-1","toUserId":"robot","tenantId":"tenant-a","content":"你好，介绍一下套餐"}
                """;
        String signature = "sha256=" + Digests.hmacSha256("test-secret", payload);

        mockMvc.perform(post("/api/channel/webhook")
                        .header("X-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicated").value(false));

        mockMvc.perform(post("/api/channel/webhook")
                        .header("X-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicated").value(true));

        mockMvc.perform(post("/api/channel/webhook")
                        .header("X-Signature", "sha256=bad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        assertThat(chatMessageMapper.selectCount(null)).isGreaterThanOrEqualTo(2);
        assertThat(customerMemoryMapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void knowledgeUploadIndexesDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "faq.txt",
                "text/plain",
                "套餐A适合预算有限客户。套餐B适合需要上门服务客户。".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/admin/knowledge/documents")
                        .file(file)
                        .param("tenantId", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INDEXED"));

        assertThat(knowledgeChunkMapper.selectCount(Wrappers.emptyWrapper())).isGreaterThan(0);
    }
}
