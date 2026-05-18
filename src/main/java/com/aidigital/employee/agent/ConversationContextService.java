package com.aidigital.employee.agent;

import com.aidigital.employee.channel.ChatMessage;
import com.aidigital.employee.channel.ChatMessageMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ConversationContextService {

    private final ChatMessageMapper chatMessageMapper;

    public ConversationContextService(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    /**
     * 加载并裁剪会话最近消息，拼装为模型可消费的上下文结构。
     */
    public List<ContextLine> buildContext(Long sessionId, int maxMessages, int maxCharacters) {
        List<ChatMessage> messages = chatMessageMapper.selectRecentBySessionId(sessionId, 20).stream()
                .limit(maxMessages)
                .toList();
        StringBuilder builder = new StringBuilder();
        return messages.stream()
                .sorted((left, right) -> left.getCreatedAt().compareTo(right.getCreatedAt()))
                .map(message -> new ContextLine(message.getDirection(), message.getContent()))
                .filter(line -> {
                    if (builder.length() + line.content().length() > maxCharacters) {
                        return false;
                    }
                    builder.append(line.content());
                    return true;
                })
                .collect(Collectors.toList());
    }

    public record ContextLine(String role, String content) {
    }
}
