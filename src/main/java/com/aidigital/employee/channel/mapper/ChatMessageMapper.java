package com.aidigital.employee.channel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aidigital.employee.channel.entity.ChatMessage;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("""
            select * from chat_messages
            where session_id = #{sessionId}
            order by created_at desc
            limit #{limit}
            """)
    List<ChatMessage> selectRecentBySessionId(@Param("sessionId") Long sessionId, @Param("limit") int limit);
}
