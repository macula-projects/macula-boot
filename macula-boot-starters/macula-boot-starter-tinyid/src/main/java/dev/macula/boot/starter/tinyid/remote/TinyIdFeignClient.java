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
package dev.macula.boot.starter.tinyid.remote;

import dev.macula.boot.starter.tinyid.config.TinyIdFeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 经统一网关申请号段的客户端，应用凭据由专属配置签名。
 *
 * @author Rain
 * @since 6.1.0
 */
@FeignClient(name = "macula-cloud-tinyid", contextId = "tinyIdFeignClient",
    url = "${macula.cloud.endpoint}/tinyid", configuration = TinyIdFeignClientConfiguration.class)
public interface TinyIdFeignClient {

    /**
     * 申请下一个号段。
     * @param bizType 已在服务端配置的业务类型
     * @return currentId,loadingId,maxId,delta,remainder 格式的文本
     */
    @PostMapping(value = "/api/v1/id/nextSegmentIdSimple", produces = {"text/plain", "application/json"})
    String nextSegmentId(@RequestParam("bizType") String bizType);
}
