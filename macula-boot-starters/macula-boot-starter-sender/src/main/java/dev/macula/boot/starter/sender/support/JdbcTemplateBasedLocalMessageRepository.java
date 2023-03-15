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

package dev.macula.boot.starter.sender.support;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code JdbcTemplateBasedLocalMessageRepository} 本地消息持久化
 *
 * @author https://gitee.com/litao851025/lego
 * @since 2023/1/3 14:10
 */
public class JdbcTemplateBasedLocalMessageRepository implements LocalMessageRepository {

    private static final String SQL_INSERT =
        "insert into %s " + "(orderly, topic, sharding_key, tag, msg_key, msg_id, msg, retry_time, status, create_time, update_time)" + " values " + "(:orderly, :topic, :shardingKey, :tag, :msgKey, :msgId, :msg, :retryTime, :status, :createTime, :updateTime)";

    private static final String SQL_UPDATE =
        "update %s " + "set " + " msg_id = :msgId, " + " retry_time = :retryTime," + " status = :status," + " update_time = :updateTime " + "where " + " id = :id";

    private static final String SQL_LOAD_BY_UPDATE_TIME =
        "select " + "id, orderly, topic, sharding_key, tag, msg_key, msg_id, msg, retry_time, status, create_time, update_time " + "from %s " + "where update_time > :updateTime and status in (:errorStatus, :noneStatus)";

    private final TransactionTemplate transactionTemplate;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final String tableName;

    public JdbcTemplateBasedLocalMessageRepository(DataSource dataSource, String tableName) {
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager(dataSource);
        this.transactionTemplate = new TransactionTemplate(dataSourceTransactionManager);
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.tableName = tableName;
    }

    @Override
    public void save(LocalMessage message) {
        String sql = buildInsertSql();
        SqlParameterSource ps = new BeanPropertySqlParameterSource(message);
        KeyHolder keyHolder = new GeneratedKeyHolder();

        this.transactionTemplate.execute(transactionStatus -> {
            this.jdbcTemplate.update(sql, ps, keyHolder);
            return null;
        });

        Number id = keyHolder.getKey();
        if (id != null) {
            message.setId(id.longValue());
        }
    }

    private String buildInsertSql() {
        return String.format(SQL_INSERT, tableName);
    }

    @Override
    public void update(LocalMessage message) {
        String sql = buildUpdateSql();
        SqlParameterSource ps = new BeanPropertySqlParameterSource(message);

        this.transactionTemplate.execute(transactionStatus -> {
            this.jdbcTemplate.update(sql, ps);
            return null;
        });

    }

    private String buildUpdateSql() {
        return String.format(SQL_UPDATE, tableName);
    }

    @Override
    public List<LocalMessage> loadNotSuccessByUpdateGt(Date latestUpdateTime, int size) {
        String sql = buildLoadNotSuccess();
        SqlParameterSource ps = buildParameterSource(latestUpdateTime);
        RowMapper<LocalMessage> rowMapper = new BeanPropertyRowMapper<>(LocalMessage.class);

        return this.jdbcTemplate.query(sql, ps, rowMapper);
    }

    private SqlParameterSource buildParameterSource(Date latestUpdateTime) {
        Map<String, Object> param = new HashMap<>();
        param.put("updateTime", latestUpdateTime);
        param.put("errorStatus", LocalMessage.STATUS_ERROR);
        param.put("noneStatus", LocalMessage.STATUS_NONE);
        return new MapSqlParameterSource(param);
    }

    private String buildLoadNotSuccess() {
        return String.format(SQL_LOAD_BY_UPDATE_TIME, tableName);
    }
}
