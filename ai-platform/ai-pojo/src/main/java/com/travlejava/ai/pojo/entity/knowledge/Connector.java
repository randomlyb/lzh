package com.travlejava.ai.pojo.entity.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_connector")
public class Connector {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;
    private String configJson;
    private LocalDateTime lastSyncTime;
    private Integer status;
}
