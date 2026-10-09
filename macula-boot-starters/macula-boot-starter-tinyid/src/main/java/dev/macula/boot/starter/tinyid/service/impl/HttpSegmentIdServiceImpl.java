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

package dev.macula.boot.starter.tinyid.service.impl;

import dev.macula.boot.starter.tinyid.base.entity.SegmentId;
import dev.macula.boot.starter.tinyid.base.service.SegmentIdService;
import dev.macula.boot.starter.tinyid.remote.TinyIdFeignClient;
import org.springframework.util.Assert;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于HTTP 的段ID服务实现
 * 
 * @author du_imba
 * @since 5.0.0
 */
public class HttpSegmentIdServiceImpl implements SegmentIdService {

    private final TinyIdFeignClient client;

    public HttpSegmentIdServiceImpl(TinyIdFeignClient client) {
        this.client = client;
    }

    @Override
    public SegmentId getNextSegmentId(String bizType) {
        Assert.hasText(bizType, "bizType must not be blank");
        String response = client.nextSegmentId(bizType);
        if (response == null || response.trim().isEmpty()) {
            return null;
        }
        SegmentId segmentId = new SegmentId();
        String[] arr = response.split(",", -1);
        if (arr.length != 5) {
            throw new IllegalStateException("Invalid TinyID segment response");
        }
        segmentId.setCurrentId(new AtomicLong(Long.parseLong(arr[0])));
        segmentId.setLoadingId(Long.parseLong(arr[1]));
        segmentId.setMaxId(Long.parseLong(arr[2]));
        segmentId.setDelta(Integer.parseInt(arr[3]));
        segmentId.setRemainder(Integer.parseInt(arr[4]));
        return segmentId;
    }

}
