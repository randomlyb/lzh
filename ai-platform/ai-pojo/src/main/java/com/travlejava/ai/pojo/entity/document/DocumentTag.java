package com.travlejava.ai.pojo.entity.document;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_document_tag")
public class DocumentTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private String tag;
    private LocalDateTime createTime;
}
