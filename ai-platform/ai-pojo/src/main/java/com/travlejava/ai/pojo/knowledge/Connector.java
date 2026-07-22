
package com.travlejava.ai.pojo.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 连接器实体类
 * 用于配置和管理外部数据源的连接，如GitHub、RSS、CSDN 等
 */
@Data
@TableName("ai_connector")
public class Connector {
    
    /**
     * 主键ID，自动生成
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 连接器类型：GITHUB/RSS/CSDN 等
     */
    private String type;
    
    /**
     * 连接器配置JSON，存储该类型连接器的特定配置信息
     */
    private String configJson;
    
    /**
     * 最后同步时间，记录该连接器上次成功同步的时间
     */
    private LocalDateTime lastSyncTime;
    
    /**
     * 连接器状态：0-禁用，1-启用
     */
    private Integer status;
}
