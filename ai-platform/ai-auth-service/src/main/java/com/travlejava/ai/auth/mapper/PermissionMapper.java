package com.travlejava.ai.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travlejava.ai.pojo.auth.Permission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}
