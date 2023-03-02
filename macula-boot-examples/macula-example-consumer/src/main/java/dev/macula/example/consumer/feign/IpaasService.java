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

package dev.macula.example.consumer.feign;

import dev.macula.example.consumer.feign.configuration.IpaasConfiguration;
import dev.macula.example.consumer.vo.CompanyDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ipaas-service", url = "https://ipaas-dev.infinitus.com.cn",
    configuration = IpaasConfiguration.class)
public interface IpaasService {

    @PostMapping("/openapi/product-api/companies")
    String companies(@RequestBody CompanyDto companyDto);

    @GetMapping("/openapi/product-api/skus")
    String listSkus(@RequestParam("sku") String sku);

}
