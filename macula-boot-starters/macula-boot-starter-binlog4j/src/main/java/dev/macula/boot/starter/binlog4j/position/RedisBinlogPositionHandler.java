package dev.macula.boot.starter.binlog4j.position;

import com.alibaba.fastjson2.JSON;
import dev.macula.boot.starter.binlog4j.utils.CacheConstants;
import org.redisson.api.RedissonClient;

public class RedisBinlogPositionHandler implements BinlogPositionHandler {

    private final RedissonClient redissonClient;

    public RedisBinlogPositionHandler(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public BinlogPosition loadPosition(Long serverId) {
        Object value = redissonClient.getBucket(getKey(serverId)).get();
        if (value != null) {
            return JSON.parseObject(value.toString(), BinlogPosition.class);
        }
        return null;
    }

    @Override
    public void savePosition(BinlogPosition position) {
        redissonClient.getBucket(getKey(position.getServerId())).set(JSON.toJSONString(position));
    }

    private String getKey(Long serverId) {
        return CacheConstants.CACHE_BINLOG_PREFIX + serverId.toString();
    }
}
