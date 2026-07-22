package com.travlejava.ai.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseEnum {

    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(404, "参数检验失败"),
    UNAUTHORIZED(401, "暂未登录或token已经过期"),
    FORBIDDEN(403, "没有相关权限"),

    // Common Errors 1000-1999
    UNKNOWN_ERROR(1000, "未知错误"),
    INVALID_PARAM(1001, "无效参数"),
    DATA_NOT_FOUND(1002, "数据不存在"),

    // Auth Errors 2000-2999
    USERNAME_OR_PASSWORD_ERROR(2000, "用户名或密码错误"),
    USER_NOT_EXIST(2001, "用户不存在"),
    USER_ALREADY_EXISTS(2002, "用户已存在"),
    TOKEN_MISSING(2003, "Token缺失"),
    TOKEN_INVALID(2004, "Token无效或已过期"),
    TOKEN_EXPIRED(2005, "Token已过期"),
    REFRESH_TOKEN_INVALID(2006, "Refresh Token无效"),

    // Document Errors 3000-3999
    DOCUMENT_UPLOAD_FAILED(3000, "文档上传失败"),
    DOCUMENT_PARSE_FAILED(3001, "文档解析失败"),
    DOCUMENT_NOT_FOUND(3002, "文档不存在"),

    // Knowledge Base Errors 4000-4999
    KNOWLEDGE_BASE_NOT_FOUND(4000, "知识库不存在"),
    KNOWLEDGE_BASE_ALREADY_EXISTS(4001, "知识库已存在"),
    DOCUMENT_ALREADY_IN_KB(4002, "文档已在知识库中"),

    // Scheduler Errors 5000-5999
    JOB_CREATE_FAILED(5000, "任务创建失败"),
    JOB_NOT_FOUND(5001, "任务不存在"),
    JOB_TRIGGER_FAILED(5002, "任务触发失败"),
    EXECUTOR_NOT_FOUND(5003, "执行器不存在");

    private final Integer code;
    private final String message;
}
