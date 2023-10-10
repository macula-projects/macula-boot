/*
 * Copyright (c) 2023 Macula
 *   macula.dev, China
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

#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.admin.bff.controller;

import dev.macula.boot.result.PageVO;
import ${package}.admin.bff.service.ApplicationService;
import ${package}.service1.form.ApplicationForm;
import ${package}.service1.query.ApplicationPageQuery;
import ${package}.service1.vo.app.ApplicationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * {@code ApplicationController} 应用管理服务
 *
 * @author rain
 * @since 2023/10/9 18:42
 */
@Tag(name = "应用管理服务", description = "应用管理服务")
@RestController
@RequestMapping("/api/v1/app")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;

    @Operation(summary = "获取应用列表分页")
    @Parameter(name = "查询参数")
    @GetMapping
    public PageVO<ApplicationVO> listApplications(ApplicationPageQuery queryParams) {
        return applicationService.listApplicationPages(queryParams);
    }

    @Operation(summary = "新增应用")
    @Parameter(name = "应用表单数据")
    @PostMapping
    public boolean saveApplication(@Valid @RequestBody ApplicationForm formData) {
        return applicationService.saveApplication(formData);
    }

    @Operation(summary = "修改应用")
    @Parameter(name = "应用ID")
    @Parameter(name = "应用表单数据")
    @PutMapping(value = "/{appId}")
    public boolean updateApplication(@PathVariable Long appId, @Valid @RequestBody ApplicationForm formData) {
        return applicationService.updateApplication(appId, formData);
    }

    @Operation(summary = "删除应用")
    @Parameter(name = "应用ID，多个以英文逗号(,)分割")
    @DeleteMapping("/{ids}")
    public boolean deleteApplications(@PathVariable("ids") String ids) {
        return applicationService.deleteApplications(ids);
    }
}
