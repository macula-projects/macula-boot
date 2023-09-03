package dev.macula.boot.starter.binlog.position;

import dev.macula.boot.starter.binlog.config.RedisConfig;

public class RedisBinlogPositionHandler implements BinlogPositionHandler {
    public RedisBinlogPositionHandler(RedisConfig redisConfig) {

    }

    @Override
    public BinlogPosition loadPosition(Long serverId) {
        return null;
    }

    @Override
    public void savePosition(BinlogPosition position) {

    }
    /**
    private JedisPool jedisPool;

    public RedisBinlogPositionHandler(RedisConfig redisConfig) {
        this.jedisPool =
            new JedisPool(new GenericObjectPoolConfig(), redisConfig.getHost(), redisConfig.getPort(), 1000,
                redisConfig.getPassword());
    }

    @Override
    public BinlogPosition loadPosition(Long serverId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(serverId.toString());
            if (value != null) {
                return JSON.parseObject(value, BinlogPosition.class);
            }
        }
        return null;
    }

    @Override
    public void savePosition(BinlogPosition position) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(position.getServerId().toString(), JSON.toJSONString(position));
        }
    }
     */
}
