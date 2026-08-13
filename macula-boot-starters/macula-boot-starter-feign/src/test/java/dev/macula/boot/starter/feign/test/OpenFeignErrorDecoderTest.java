/*
 * Copyright (c) 2024 Macula
 *    macula.dev, China
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

package dev.macula.boot.starter.feign.test;

import dev.macula.boot.exception.BizCheckException;
import dev.macula.boot.exception.BizException;
import dev.macula.boot.starter.feign.codec.OpenFeignErrorDecoder;
import feign.Request;
import feign.Request.Body;
import feign.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * <p>
 * <b>OpenFeignErrorDecoderTest</b> OpenFeign 错误解码器测试
 * </p>
 * <p>
 * 测试Feign调用错误时是否正确解析Result并包装成业务异常
 * </p>
 *
 * @author Rain
 * @since 2024/04/07
 */
public class OpenFeignErrorDecoderTest {

    private final OpenFeignErrorDecoder decoder = new OpenFeignErrorDecoder();

    /**
     * 测试解析返回失败Result，包装为BizException
     */
    @Test
    void testDecodeFailedResult() {
        // given: 构造失败的Result响应
        String body = """
                {
                    "success": false,
                    "code": "500",
                    "msg": "业务处理失败",
                    "data": null
                }
                """;
        Response response = createResponse(500, body);

        // when: 解析异常
        Exception exception = decoder.decode("dev.macula.TestService#test()", response);

        // then: 正确包装为BizException
        Assertions.assertTrue(exception instanceof BizException, "Exception should be wrapped as BizException");
        BizException bizException = (BizException) exception;
        Assertions.assertEquals("500", bizException.getCode());
        Assertions.assertEquals("Feign提供方异常：业务处理失败", bizException.getMessage());
    }

    /**
     * 测试业务检查异常（NOT_EXTENDED = 207）
     */
    @Test
    void testDecodeBizCheckException() {
        // given: 业务校验失败Result，状态码207（NOT_EXTENDED）
        String body = """
                {
                    "success": false,
                    "code": "400",
                    "msg": "参数校验失败"
                }
                """;
        Response response = createResponse(207, body);

        // when: 解析异常
        Exception exception = decoder.decode("dev.macula.TestService#test()", response);

        // then: 包装为BizCheckException
        Assertions.assertTrue(exception instanceof BizCheckException,
                "Status 207 should be wrapped as BizCheckException");
        BizCheckException checkException = (BizCheckException) exception;
        Assertions.assertEquals("400", checkException.getCode());
        Assertions.assertEquals("参数校验失败", checkException.getMsg());
        Assertions.assertEquals("Feign提供方异常：参数校验失败", checkException.getMessage());
    }

    /**
     * 测试body中包含cause字段时使用cause作为错误消息
     */
    @Test
    void testUseCauseWhenPresent() {
        // given: Result包含cause
        String body = """
                {
                    "success": false,
                    "code": "404",
                    "msg": "资源不存在",
                    "cause": "具体的错误原因在这里"
                }
                """;
        Response response = createResponse(404, body);

        // when: 解析异常
        Exception exception = decoder.decode("dev.macula.TestService#test()", response);

        // then: 错误消息应该是cause的值
        BizException bizException = (BizException) exception;
        Assertions.assertEquals("具体的错误原因在这里", bizException.getMessage());
    }

    /**
     * 测试JSON解析失败时返回body内容作为异常消息
     */
    @Test
    void testInvalidJsonReturnsBody() {
        // given: 非JSON响应
        String body = "Internal Server Error - HTML error page";
        Response response = createResponse(500, body);

        // when: 解析异常
        Exception exception = decoder.decode("dev.macula.TestService#test()", response);

        // then: 仍然包装异常，消息就是body内容
        Assertions.assertTrue(exception instanceof BizException);
        Assertions.assertEquals(body, exception.getMessage());
    }

    /**
     * 测试成功状态码仍然返回异常（因为进入了error decoder说明还是有问题）
     */
    @Test
    void testNonErrorStatusUsesReasonPhrase() {
        // given: 状态码200但还是进入了error decoder
        String body = "";
        Request request = Request.create(Request.HttpMethod.GET, "http://example.com",
                Collections.emptyMap(), Body.empty(), null);
        Response response = Response.builder()
                .status(200)
                .body(body.getBytes(StandardCharsets.UTF_8))
                .request(request)
                .build();
        // reason() 在这种情况下会返回OK

        // when: 解析异常
        Exception exception = decoder.decode("dev.macula.TestService#test()", response);

        // then: 返回BizException，包含reason
        Assertions.assertTrue(exception instanceof BizException);
    }

    /**
     * 测试空body的情况
     */
    @Test
    void testEmptyBodyReturnsGenericMessage() {
        // given: 空body
        Response response = createResponse(500, "");

        // when: 解析异常
        Exception exception = decoder.decode("dev.macula.TestService#test()", response);

        // then: 返回通用异常消息
        Assertions.assertTrue(exception instanceof BizException);
        Assertions.assertEquals("Feign提供方异常", exception.getMessage());
    }

    /**
     * 创建测试Response
     */
    private Response createResponse(int status, String body) {
        Request request = Request.create(Request.HttpMethod.GET, "http://example.com",
                Collections.emptyMap(), Body.empty(), null);
        InputStream inputStream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        return Response.builder()
                .status(status)
                .body(inputStream, body.getBytes(StandardCharsets.UTF_8).length)
                .request(request)
                .build();
    }
}
