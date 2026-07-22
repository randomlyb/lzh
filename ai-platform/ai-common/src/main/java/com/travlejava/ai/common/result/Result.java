package com.travlejava.ai.common.result;

import lombok.Data;

/**
 * 统一响应结果类
 * 用于封装API接口的返回结果，包含状态码、消息和数据
 *
 * @param <T> 响应数据的类类型
 */
@Data
public class Result<T> {
    
    /**
     * 响应状态码，200表示成功，其他表示失败
     */
    private Integer code;

    /**
     * 响应消息，描述操作结果
     */
    private String message;
    
    /**
     * 响应数据，具体的业务数据
     */
    private T data;

    /**
     * 私有构造方法，禁止直接实例化
     */
    private Result() {}

    /**
     * 私有构造方法，用于创建响应对象
     *
     * @param code 状态码
     * @param message 消息
     * @param data 数据
     */
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    /**
     * 创建成功响应（带数据）
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 创建成功响应（带自定义消息和数据）
     *
     * @param message 自定义消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 创建失败响应（默认500错误码）
     *
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 创建失败响应（自定义错误码）
     *
     * @param code 自定义错误码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
