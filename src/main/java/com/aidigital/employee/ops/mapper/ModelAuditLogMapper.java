package com.aidigital.employee.ops.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aidigital.employee.ops.entity.ModelAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ModelAuditLogMapper extends BaseMapper<ModelAuditLog> {
}
