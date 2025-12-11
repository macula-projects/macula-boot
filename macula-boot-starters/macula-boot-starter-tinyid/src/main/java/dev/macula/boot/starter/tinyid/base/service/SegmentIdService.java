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

package dev.macula.boot.starter.tinyid.base.service;

import dev.macula.boot.starter.tinyid.base.entity.SegmentId;

/**
 * 段 ID服务接口
 * 
 * @author du_imba
 */
public interface SegmentIdService {

    /**
     * 根据bizType 获取下一个SegmentId对象
     *
     * @param bizType 业务类型
     * @return SegmentId
     */
    SegmentId getNextSegmentId(String bizType);

}
