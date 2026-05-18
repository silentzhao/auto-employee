package com.aidigital.employee.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.aidigital.employee.customer.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
