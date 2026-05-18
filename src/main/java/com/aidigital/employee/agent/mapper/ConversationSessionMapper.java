package com.aidigital.employee.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aidigital.employee.agent.entity.ConversationSession;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConversationSessionMapper extends BaseMapper<ConversationSession> {

    @Select("""
            select * from conversation_sessions
            where customer_id = #{customerId} and channel_code = #{channelCode}
            order by last_active_at desc
            limit 1
            """)
    ConversationSession selectLatestByCustomerAndChannel(@Param("customerId") Long customerId, @Param("channelCode") String channelCode);

    @Select("""
            select * from conversation_sessions
            where customer_id = #{customerId}
            order by last_active_at desc
            limit 20
            """)
    List<ConversationSession> selectRecentByCustomerId(@Param("customerId") Long customerId);
}
