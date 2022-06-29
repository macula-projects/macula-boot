package org.macula.boot.starter.tinyid.service.impl;

import org.macula.boot.starter.tinyid.base.entity.SegmentId;
import org.macula.boot.starter.tinyid.base.service.SegmentIdService;
import org.macula.boot.starter.tinyid.config.TinyIdProperties;
import org.macula.boot.starter.tinyid.utils.TinyIdHttpUtils;
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
    private static String serverUrl = "http://{0}/tinyid/id/nextSegmentIdSimple?token={1}&bizType=";
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
