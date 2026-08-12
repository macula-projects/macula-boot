/*
 * Copyright (c) 2024-2026 Macula
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
package dev.macula.boot.starter.auditlog.test.controller;

import dev.macula.boot.starter.auditlog.annotation.AuditLog;
import dev.macula.boot.starter.auditlog.enums.BusinessType;
import dev.macula.boot.starter.auditlog.test.vo.UserForm;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <b>AuditLogTestController</b> 用于测试审计日志切面的测试控制器
 *
 * @author Rain
 * @since 2024/04/07
 */
@RestController
@RequestMapping("/api/v1/test")
public class AuditLogTestController {

    /** 测试新增操作的审计日志 */
    @AuditLog(title = "用户管理", businessType = BusinessType.INSERT, isSaveRequestData = true, isSaveResponseData = true)
    @PostMapping("/user")
    public Long createUser(@RequestBody UserForm userForm) {
        // 模拟创建用户，返回ID
        return 1L;
    }

    /** 测试更新操作的审计日志，排除敏感密码字段 */
    @AuditLog(title = "用户管理", businessType = BusinessType.UPDATE, excludeParamNames = {"password",
        "confirmPassword"}, maxParamLength = 1000)
    @PutMapping("/user/{id}")
    public Boolean updateUser(@PathVariable Long id, @RequestBody UserForm userForm) {
        return true;
    }

    /** 测试删除操作的审计日志 */
    @AuditLog(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/user/{id}")
    public Boolean deleteUser(@PathVariable Long id) {
        return true;
    }

    /** 测试不保存请求参数 */
    @AuditLog(title = "用户管理", businessType = BusinessType.SELECT, isSaveRequestData = false)
    @GetMapping("/user/{id}")
    public UserForm getUser(@PathVariable Long id) {
        UserForm userForm = new UserForm();
        userForm.setId(id);
        userForm.setUsername("test");
        userForm.setEmail("test@example.com");
        return userForm;
    }
}
