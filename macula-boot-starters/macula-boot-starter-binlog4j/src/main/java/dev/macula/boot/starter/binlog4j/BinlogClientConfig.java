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

package dev.macula.boot.starter.binlog4j;

import cn.hutool.crypto.digest.MD5;
import dev.macula.boot.starter.binlog4j.enums.BinlogClientMode;
import dev.macula.boot.starter.binlog4j.utils.CacheConstants;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class BinlogClientConfig {

    /** 账户 */
    private String username;

    /** 密码 */
    private String password;

    /** 地址 */
    private String host;

    /** 端口 */
    private int port = 3306;

    /** 时间偏移量 */
    private long timeOffset = 0;

    /** 客户端编号 (不同的集群) */
    private long serverId;

    /** 是否保持连接 */
    private boolean keepAlive = true;

    /** 是否是首次启动 */
    private boolean inaugural = false;

    /** 保持连接时间 */
    private long KeepAliveInterval = TimeUnit.MINUTES.toMillis(1L);

    /** 链接超时 */
    private long connectTimeout = TimeUnit.SECONDS.toMillis(3L);

    /** 发送心跳包时间间隔 */
    private long heartbeatInterval = TimeUnit.SECONDS.toMillis(5L);

    /** 读取记忆 */
    private boolean persistence = false;

    /** 是否开启GTID复制 */
    private boolean gtidMode = false;

    /** 当GtidSet设为""时，是否通过gtid_purged获取新的gtid */
    private boolean gtidPurged = true;

    /** 当Gtid起始位置没有缓存时，默认的GTID */
    private String gtidSetDefault;

    /** 部署模式 */
    private BinlogClientMode mode = BinlogClientMode.standalone;

    public String getKey() {
        return CacheConstants.CACHE_BINLOG_PREFIX + MD5.create()
            .digestHex(this.host + ":" + this.port + ":" + this.serverId);
    }
}
