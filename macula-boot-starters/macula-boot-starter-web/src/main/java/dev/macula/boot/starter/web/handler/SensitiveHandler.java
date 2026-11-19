package dev.macula.boot.starter.web.handler;

import dev.macula.boot.starter.web.annotation.Sensitive;

/**
 * {@code SensitiveHandler} is 脱敏自定义处理
 *
 * @author Rain
 * @since 2026/11/19 16:27
 */
public interface SensitiveHandler {
    /**
     * 自定义脱敏处理方法
     * @param value 原文
     * @param sensitive 注解内容
     * @return 脱敏后内容
     */
    String handle(String value, Sensitive sensitive);
}
