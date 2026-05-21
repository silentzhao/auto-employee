package com.aidigital.employee.ops.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aidigital.employee.ops.entity.AsyncTask;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AsyncTaskMapper extends BaseMapper<AsyncTask> {

    @Select("""
            select * from async_tasks
            where status in ('PENDING', 'RETRY') and next_run_at <= #{now}
            order by next_run_at asc, id asc
            limit #{limit}
            """)
    List<AsyncTask> selectReadyTasks(@Param("now") Instant now, @Param("limit") int limit);
}
