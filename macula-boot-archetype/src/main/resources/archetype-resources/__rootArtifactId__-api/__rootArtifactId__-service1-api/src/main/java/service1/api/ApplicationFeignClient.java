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
package ${package}.service1.api;

import ${package}.service1.form.ApplicationForm;
import ${package}.service1.api.fallback.AbstracApplicationFeignFallbackFactory;
import ${package}.service1.query.ApplicationPageQuery;
import ${package}.service1.vo.app.ApplicationVO;
import dev.macula.boot.result.PageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * {@code ApplicationFeignClient} is 应用管理接口
 *
 * @author rain
 * @since 2023/9/11 17:38
 */
@FeignClient(value = "${rootArtifactId}-service1", contextId = "applicationFeignClient",
    fallbackFactory = AbstracApplicationFeignFallbackFactory.class)
public interface ApplicationFeignClient {
    @GetMapping("/api/v1/app")
    PageVO<ApplicationVO> listApplications(@SpringQueryMap ApplicationPageQuery queryParams);

    @PostMapping("/api/v1/app")
    boolean saveApplication(@Valid @RequestBody ApplicationForm formData);

    @PutMapping(value = "/api/v1/app/{appId}")
    boolean updateApplication(@PathVariable Long appId, @Valid @RequestBody ApplicationForm formData);

    @DeleteMapping("/api/v1/app/{ids}")
    boolean deleteApplications(@PathVariable("ids") String ids);

    @PutMapping("/api/v1/app/addMaintainer/{appId}")
    boolean addMaintainer(@PathVariable Long appId, @RequestBody ApplicationForm formData);
}
