package com.travlejava.ai.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travlejava.ai.pojo.auth.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
