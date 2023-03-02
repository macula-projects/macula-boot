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
import dev.macula.boot.starter.tinyid.config.TinyIdProperties;
import dev.macula.boot.starter.tinyid.utils.TinyIdHttpUtils;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * @author du_imba
 */
public class HttpSegmentIdServiceImpl implements SegmentIdService {
    private static final Logger logger = Logger.getLogger(HttpSegmentIdServiceImpl.class.getName());
    private static String serverUrl = "http://{0}/tinyid/api/v1/id/nextSegmentIdSimple?token={1}&bizType=";
    private TinyIdProperties properties;
    private List<String> serverList;

    public HttpSegmentIdServiceImpl(TinyIdProperties properties) {
        this.properties = properties;
    }

    @Override
    public SegmentId getNextSegmentId(String bizType) {
        String url = chooseService(bizType);
        String response = TinyIdHttpUtils.post(url, properties.getReadTimeout(), properties.getConnectTimeout());
        logger.info("tinyId client getNextSegmentId end, response:" + response);
        if (response == null || "".equals(response.trim())) {
            return null;
        }
        SegmentId segmentId = new SegmentId();
        String[] arr = response.split(",");
        segmentId.setCurrentId(new AtomicLong(Long.parseLong(arr[0])));
        segmentId.setLoadingId(Long.parseLong(arr[1]));
        segmentId.setMaxId(Long.parseLong(arr[2]));
        segmentId.setDelta(Integer.parseInt(arr[3]));
        segmentId.setRemainder(Integer.parseInt(arr[4]));
        return segmentId;
    }

    private String chooseService(String bizType) {
        // 将tinyIdServer转为需要的访问URL
        if (serverList == null) {
            if (StringUtils.hasLength(properties.getServer()) && StringUtils.hasLength(properties.getToken())) {
                String[] tinyIdServers = properties.getServer().split(",");
                serverList = new ArrayList<>(tinyIdServers.length);
                for (String server : tinyIdServers) {
                    String url = MessageFormat.format(serverUrl, server, properties.getToken());
                    serverList.add(url);
                }
            }
        }

        String url = "";
        if (serverList != null && serverList.size() == 1) {
            url = serverList.get(0);
        } else if (serverList != null && serverList.size() > 1) {
            Random r = new Random();
            url = serverList.get(r.nextInt(serverList.size()));
        }
        url += bizType;
        return url;
    }
}
