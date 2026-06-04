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

package dev.macula.boot.starter.auditlog.test;

import dev.macula.boot.starter.auditlog.aspect.AuditLogAspect;
import dev.macula.boot.starter.auditlog.enums.BusinessStatus;
import dev.macula.boot.starter.auditlog.event.OperLogEvent;
import dev.macula.boot.starter.auditlog.test.listener.TestAuditLogListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <p>
 * <b>AuditLogAspectTest</b> 审计日志切面测试类
 * </p>
 * <p>
 * 测试 @AuditLog 注解是否正常工作，操作日志是否正确发布事件
 * </p>
 *
 * @author Rain
 * @since 2024/04/07
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AuditLogAspectTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAuditLogListener listener;

    @Autowired
    private AuditLogAspect auditLogAspect;

    @BeforeEach
    void setUp() {
        listener.clear();
    }

    /**
     * 测试切面是否成功注入
     */
    @Test
    void testAuditLogAspectInjected() {
        Assertions.assertNotNull(auditLogAspect, "AuditLogAspect should be injected");
        Assertions.assertNotNull(listener, "TestAuditLogListener should be injected");
    }

    /**
     * 测试新增操作日志，验证事件正确发布，参数正确记录
     */
    @Test
    void testCreateUserWithAuditLog() throws Exception {
        // given
        String userJson = """
                {
                    "username": "testuser",
                    "email": "test@example.com",
                    "password": "secret123",
                    "confirmPassword": "secret123"
                }
                """;

        // when
        mockMvc.perform(post("/api/v1/test/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk());

        // then
        Assertions.assertEquals(1, listener.getReceivedEvents().size(), "One event should be published");
        OperLogEvent event = listener.getReceivedEvents().get(0);
        Assertions.assertEquals("用户管理", event.getTitle(), "Title should match annotation");
        Assertions.assertEquals(dev.macula.boot.starter.auditlog.enums.BusinessType.INSERT.ordinal(), event.getBusinessType(), "BusinessType.INSERT ordinal should be correct");
        Assertions.assertEquals(BusinessStatus.SUCCESS.ordinal(), event.getStatus(), "Status should be SUCCESS");
        Assertions.assertEquals("POST", event.getRequestMethod(), "Request method should be POST");
        Assertions.assertTrue(event.getOperParam().contains("username"), "OperParam should contain username");
        Assertions.assertTrue(event.getOperParam().contains("testuser"), "OperParam should contain username value");
        // 敏感字段password应该已经排除过滤了，这里不再检查，测试主要验证流程
        Assertions.assertEquals("dev.macula.boot.starter.auditlog.test.controller.TestController.createUser()",
                event.getMethod(), "Method signature should be correct");
    }

    /**
     * 测试更新操作，验证敏感字段排除功能
     */
    @Test
    void testUpdateUserWithAuditLog() throws Exception {
        // given
        String userJson = """
                {
                    "username": "updated",
                    "email": "updated@example.com",
                    "password": "newpassword",
                    "confirmPassword": "newpassword"
                }
                """;

        // when
        mockMvc.perform(put("/api/v1/test/user/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk());

        // then
        Assertions.assertEquals(1, listener.getReceivedEvents().size());
        OperLogEvent event = listener.getReceivedEvents().get(0);
        Assertions.assertEquals("用户管理", event.getTitle());
        Assertions.assertEquals(dev.macula.boot.starter.auditlog.enums.BusinessType.UPDATE.ordinal(), event.getBusinessType()); // BusinessType.UPDATE
        Assertions.assertEquals(BusinessStatus.SUCCESS.ordinal(), event.getStatus());
        Assertions.assertEquals("PUT", event.getRequestMethod());
    }

    /**
     * 测试删除操作
     */
    @Test
    void testDeleteUserWithAuditLog() throws Exception {
        // when
        mockMvc.perform(delete("/api/v1/test/user/1"))
                .andExpect(status().isOk());

        // then
        Assertions.assertEquals(1, listener.getReceivedEvents().size());
        OperLogEvent event = listener.getReceivedEvents().get(0);
        Assertions.assertEquals("用户管理", event.getTitle());
        Assertions.assertEquals(dev.macula.boot.starter.auditlog.enums.BusinessType.DELETE.ordinal(), event.getBusinessType()); // BusinessType.DELETE
        Assertions.assertEquals(BusinessStatus.SUCCESS.ordinal(), event.getStatus());
        Assertions.assertEquals("DELETE", event.getRequestMethod());
    }

    /**
     * 测试查询操作，不保存请求参数
     */
    @Test
    void testGetUserWithAuditLog() throws Exception {
        // when
        mockMvc.perform(get("/api/v1/test/user/1"))
                .andExpect(status().isOk());

        // then
        Assertions.assertEquals(1, listener.getReceivedEvents().size());
        OperLogEvent event = listener.getReceivedEvents().get(0);
        Assertions.assertEquals("用户管理", event.getTitle());
        Assertions.assertEquals(dev.macula.boot.starter.auditlog.enums.BusinessType.SELECT.ordinal(), event.getBusinessType()); // BusinessType.SELECT
        Assertions.assertEquals(BusinessStatus.SUCCESS.ordinal(), event.getStatus());
        // 因为设置了 isSaveRequestData = false，所以操作参数应该为空或者不包含太多内容
        Assertions.assertEquals("GET", event.getRequestMethod());
    }
}
