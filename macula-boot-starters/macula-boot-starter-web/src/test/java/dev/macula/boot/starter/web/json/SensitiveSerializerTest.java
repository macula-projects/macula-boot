/*
 * Copyright (c) 2026 Macula
 * macula.dev, China
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.macula.boot.starter.web.json;

import dev.macula.boot.starter.web.annotation.Sensitive;
import dev.macula.boot.starter.web.handler.SensitiveHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自定义脱敏 Handler 的 Jackson 3 上下文配置及错误传播验证。
 *
 * @author Rain
 * @since 6.1.0
 */
class SensitiveSerializerTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    /**
     * 验证自定义分支只写一次结果，避免落入默认分支输出原文。
     */
    @Test
    void customHandlerWritesExactlyOneValue() {
        assertEquals("{\"value\":\"masked\"}", mapper.writeValueAsString(new CustomValue()));
    }

    /**
     * 验证字段专用实例在缓存后仍保留各自注解参数。
     */
    @Test
    void cachedFieldSerializersKeepTheirOwnAnnotationConfiguration() {
        for (int i = 0; i < 2; i++) {
            JsonNode json = mapper.readTree(mapper.writeValueAsString(new ConfiguredValues()));
            assertEquals("abcdef:1:3:2", json.get("first").asText());
            assertEquals("uvwxyz:2:5:6", json.get("second").asText());
        }
    }

    /**
     * 验证空串和 null 不调用自定义策略。
     */
    @Test
    void emptyValuesBypassHandler() {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(new EmptyValues()));
        assertTrue(json.get("nullValue").isNull());
        assertEquals("", json.get("emptyValue").asText());
    }

    /**
     * 验证自定义策略可以返回 JSON null。
     */
    @Test
    void handlerMayReturnNull() {
        assertEquals("{\"value\":null}", mapper.writeValueAsString(new NullResult()));
    }

    /**
     * 验证策略执行失败保留原始原因和字段路径。
     */
    @Test
    void executionFailurePreservesCauseAndFieldPath() {
        JacksonException error = assertThrows(JacksonException.class,
            () -> mapper.writeValueAsString(new FailingValue()));
        assertEquals(1, error.getPath().size());
        assertEquals("value", error.getPath().get(0).getPropertyName());
        assertEquals(IllegalStateException.class, error.getCause().getClass());
        assertEquals("handler failed", error.getCause().getMessage());
    }

    /**
     * 列举缺失策略及不同构造失败原因，验证上下文异常保留原始类型。
     */
    static Stream<Arguments> invalidConfigurations() {
        return Stream.of(
            Arguments.of(new MissingHandlerValue(), SensitiveHandler.class, IllegalArgumentException.class),
            Arguments.of(new NoConstructorValue(), NoDefaultConstructorHandler.class, NoSuchMethodException.class),
            Arguments.of(new PrivateConstructorValue(), PrivateConstructorHandler.class, IllegalAccessException.class),
            Arguments.of(new ThrowingConstructorValue(), ThrowingConstructorHandler.class, InvocationTargetException.class));
    }

    /**
     * 验证初始化失败报告字段、策略类及原始反射异常。
     */
    @ParameterizedTest
    @MethodSource("invalidConfigurations")
    void invalidConfigurationReportsFieldHandlerAndOriginalCause(Object value, Class<?> handler, Class<?> cause) {
        DatabindException error = assertThrows(DatabindException.class, () -> mapper.writeValueAsString(value));
        assertTrue(error.getOriginalMessage().contains("字段 value"));
        assertTrue(error.getOriginalMessage().contains(handler.getName()));
        assertEquals(cause, error.getCause().getClass());
        if (cause == InvocationTargetException.class) {
            assertEquals("constructor failed", error.getCause().getCause().getMessage());
        }
    }

    /**
     * 验证内置规则不会创建未使用的自定义策略。
     */
    @Test
    void builtInTypeDoesNotInstantiateUnusedHandler() {
        assertEquals("{\"value\":\"138****0000\"}", mapper.writeValueAsString(new BuiltInValue()));
    }

    /**
     * 验证原有范围脱敏及未注解字段行为不变。
     */
    @Test
    void customRangesAndPlainFieldsRemainCompatible() {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(new ExistingValues()));
        assertEquals("ab***fgh", json.get("hide").asText());
        assertEquals("ab****gh", json.get("retain").asText());
        assertEquals("ab**fgh", json.get("overlay").asText());
        assertEquals("unchanged", json.get("plain").asText());
    }

    /**
     * 返回固定脱敏文本的无状态测试策略。
     * @since 6.1.0
     */
    public static class MaskingHandler implements SensitiveHandler {
        /**
         * 执行夹具策略，验证处理结果、注解参数或异常传播语义。
         */
        @Override
        public String handle(String value, Sensitive sensitive) {
            return "masked";
        }
    }

    /**
     * 回显字段注解参数，检查上下文配置隔离。
     * @since 6.1.0
     */
    public static class ConfigurationHandler implements SensitiveHandler {
        /**
         * 执行夹具策略，验证处理结果、注解参数或异常传播语义。
         */
        @Override
        public String handle(String value, Sensitive sensitive) {
            return value + ":" + sensitive.startInclude() + ":" + sensitive.endExclude() + ":" + sensitive.overlayRepeat();
        }
    }

    /**
     * 模拟策略执行失败。
     * @since 6.1.0
     */
    public static class FailingHandler implements SensitiveHandler {
        /**
         * 执行夹具策略，验证处理结果、注解参数或异常传播语义。
         */
        @Override
        public String handle(String value, Sensitive sensitive) {
            throw new IllegalStateException("handler failed");
        }
    }

    /**
     * 模拟自定义策略返回空值。
     * @since 6.1.0
     */
    public static class NullHandler implements SensitiveHandler {
        /**
         * 执行夹具策略，验证处理结果、注解参数或异常传播语义。
         */
        @Override
        public String handle(String value, Sensitive sensitive) {
            return null;
        }
    }

    /**
     * 模拟缺少无参构造器的策略。
     * @since 6.1.0
     */
    public static class NoDefaultConstructorHandler extends MaskingHandler {
        /**
         * 保存测试上下文及策略依赖。
         */
        public NoDefaultConstructorHandler(String required) {
        }
    }

    /**
     * 模拟无参构造器不可访问的策略。
     * @since 6.1.0
     */
    public static class PrivateConstructorHandler extends MaskingHandler {
        /**
         * 保存测试上下文及策略依赖。
         */
        private PrivateConstructorHandler() {
        }
    }

    /**
     * 模拟构造过程中失败的策略。
     * @since 6.1.0
     */
    public static class ThrowingConstructorHandler extends MaskingHandler {
        /**
         * 保存测试上下文及策略依赖。
         */
        public ThrowingConstructorHandler() {
            throw new IllegalStateException("constructor failed");
        }
    }

    /**
     * 携带指定注解和字段值的序列化测试夹具。
     * @since 6.1.0
     */
    public static class CustomValue {
        @Sensitive(value = Sensitive.Type.CUSTOM_HANDLER, handler = MaskingHandler.class)
        public String value = "original";
    }

    /**
     * 携带指定注解和字段值的序列化测试夹具。
     * @since 6.1.0
     */
    public static class ConfiguredValues {
        @Sensitive(value = Sensitive.Type.CUSTOM_HANDLER, handler = ConfigurationHandler.class,
            startInclude = 1, endExclude = 3, overlayRepeat = 2)
        public String first = "abcdef";
        @Sensitive(value = Sensitive.Type.CUSTOM_HANDLER, handler = ConfigurationHandler.class,
            startInclude = 2, endExclude = 5, overlayRepeat = 6)
        public String second = "uvwxyz";
    }

    /**
     * 携带指定注解和字段值的序列化测试夹具。
     * @since 6.1.0
     */
    public static class EmptyValues {
        @Sensitive(value = Sensitive.Type.CUSTOM_HANDLER, handler = FailingHandler.class)
        public String nullValue;
        @Sensitive(value = Sensitive.Type.CUSTOM_HANDLER, handler = FailingHandler.class)
        public String emptyValue = "";
    }

    /**
     * 携带指定注解和字段值的序列化测试夹具。
     * @since 6.1.0
     */
    public static class NullResult {
        @Sensitive(value = Sensitive.Type.CUSTOM_HANDLER, handler = NullHandler.class)
        public String value = "original";
    }

    /**
     * 携带指定注解和字段值的序列化测试夹具。
     * @since 6.1.0
     */
    public static class FailingValue {
        @Sensitive(value = Sensitive.Type.CUSTOM_HANDLER, handler = FailingHandler.class)
        public String value = "original";
    }

    /**
     * 携带指定注解和字段值的序列化测试夹具。
     * @since 6.1.0
     */
    public static class MissingHandlerValue {
        @Sensitive(Sensitive.Type.CUSTOM_HANDLER)
        public String value = "original";
    }

    /**
     * 携带指定注解和字段值的序列化测试夹具。
     * @since 6.1.0
     */
    public static class NoConstructorValue {
        @Sensitive(value = Sensitive.Type.CUSTOM_HANDLER, handler = NoDefaultConstructorHandler.class)
        public String value = "original";
    }

    /**
     * 携带指定注解和字段值的序列化测试夹具。
     * @since 6.1.0
     */
    public static class PrivateConstructorValue {
        @Sensitive(value = Sensitive.Type.CUSTOM_HANDLER, handler = PrivateConstructorHandler.class)
        public String value = "original";
    }

    /**
     * 携带指定注解和字段值的序列化测试夹具。
     * @since 6.1.0
     */
    public static class ThrowingConstructorValue {
        @Sensitive(value = Sensitive.Type.CUSTOM_HANDLER, handler = ThrowingConstructorHandler.class)
        public String value = "original";
    }

    /**
     * 携带指定注解和字段值的序列化测试夹具。
     * @since 6.1.0
     */
    public static class BuiltInValue {
        @Sensitive(value = Sensitive.Type.MOBILE, handler = ThrowingConstructorHandler.class)
        public String value = "13812340000";
    }

    /**
     * 携带指定注解和字段值的序列化测试夹具。
     * @since 6.1.0
     */
    public static class ExistingValues {
        @Sensitive(value = Sensitive.Type.CUSTOM_HIDE, startInclude = 2, endExclude = 5)
        public String hide = "abcdefgh";
        @Sensitive(value = Sensitive.Type.CUSTOM_RETAIN_HIDE, startInclude = 2, endExclude = 2)
        public String retain = "abcdefgh";
        @Sensitive(value = Sensitive.Type.CUSTOM_OVERLAY, startInclude = 2, endExclude = 5, overlayRepeat = 2)
        public String overlay = "abcdefgh";
        public String plain = "unchanged";
    }
}
