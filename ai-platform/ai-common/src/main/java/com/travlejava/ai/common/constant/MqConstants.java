package com.travlejava.ai.common.constant;

/**
 * 消息队列常量接口
 * 定义了RabbitMQ中使用的交换机、队列和路由键名
 */
public interface MqConstants {

    /**
     * 文档处理交换机名
     */
    String EXCHANGE_DOCUMENT = "ai.document.direct";
    
    /**
     * 文档上传队列：文档上传后触发解析任务
     */
    String QUEUE_DOCUMENT_UPLOADED = "ai.document.uploaded";
    
    /**
     * 解析完成队列：文档解析完成后触发分片任务
     */
    String QUEUE_PARSE_FINISHED = "ai.document.parse.finished";
    
    /**
     * 分片完成队列：文档分片完成后触发向量化任务
     */
    String QUEUE_CHUNK_FINISHED = "ai.document.chunk.finished";
    
    /**
     * 向量化完成队列：文档向量化完成后触发索引任务
     */
    String QUEUE_EMBEDDING_FINISHED = "ai.document.embedding.finished";
    
    /**
     * 索引完成队列：文档索引完成后触发通知或后续处理
     */
    String QUEUE_INDEX_FINISHED = "ai.document.index.finished";
    
    /**
     * 文档上传路由键
     * 用于将文档上传消息路由到文档上传队列
     */
    String ROUTING_KEY_DOCUMENT_UPLOADED = "document.uploaded";
    
    /**
     * 解析完成路由键
     * 用于将解析完成消息路由到解析完成队列
     */
    String ROUTING_KEY_PARSE_FINISHED = "parse.finished";
    
    /**
     * 分片完成路由键
     * 用于将分片完成消息路由到分片完成队列
     */
    String ROUTING_KEY_CHUNK_FINISHED = "chunk.finished";
    
    /**
     * 向量化完成路由键
     */
    String ROUTING_KEY_EMBEDDING_FINISHED = "embedding.finished";
    
    /**
     * 索引完成路由键
     * 用于将索引完成消息路由到索引完成队列
     */
    String ROUTING_KEY_INDEX_FINISHED = "index.finished";
}
