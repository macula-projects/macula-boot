package dev.macula.boot.starter.binlog4j.utils;

import com.alibaba.druid.pool.DruidDataSource;
import dev.macula.boot.starter.binlog4j.BinlogClientConfig;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JDBC 工具类
 *
 * @author
 */
public class JDBCUtils {

    /**
     * 数据源列表 (线程安全)
     *
     * serverId 与 dataSource 配置
     */
    private static final ConcurrentHashMap<Long, DataSource> dataSourceMap = new ConcurrentHashMap<>();

    /**
     * 创建数据源
     */
    public static DataSource createDataSource(BinlogClientConfig dataSourceProperties) {

        DruidDataSource dataSource = new DruidDataSource();
        String serverAddress = dataSourceProperties.getHost() + ":" + dataSourceProperties.getPort();
        dataSource.setUrl(
            "jdbc:mysql://" + serverAddress + "/INFORMATION_SCHEMA?useUnicode=true&characterEncoding=UTF-8&useSSL=false");
        dataSource.setUsername(dataSourceProperties.getUsername());
        dataSource.setPassword(dataSourceProperties.getPassword());
        dataSource.setMaxActive(20);
        return dataSource;
    }

    /**
     * 获取数据源
     *
     * @param dataSourceProperties 数据源配置
     */
    public static DataSource getDataSource(BinlogClientConfig dataSourceProperties) {
        Long serverId = dataSourceProperties.getServerId();
        return dataSourceMap.computeIfAbsent(serverId, (key) -> createDataSource(dataSourceProperties));
    }

    /**
     * 获取数据库（链接）
     *
     * @param dataSourceProperties 数据源配置
     */
    public static Connection getConnection(BinlogClientConfig dataSourceProperties) throws SQLException {
        DataSource dataSource = getDataSource(dataSourceProperties);
        return dataSource.getConnection();
    }

    /**
     * 解析 ResultSet 查询结果集
     *
     * @param resultSet 结果集
     */
    @SneakyThrows
    public static String[] parseResultSet(ResultSet resultSet) {
        List<String> buffer = new ArrayList<>();
        while (resultSet.next()) {
            buffer.add(resultSet.getString(1));
        }
        return buffer.toArray(new String[0]);
    }

    /**
     * 获取列信息
     *
     * @param dataSourceProperties 数据源配置
     * @param database             数据库
     * @param table                数据表
     */
    public static String[] getColumnNames(BinlogClientConfig dataSourceProperties, String database, String table) {
        try (Connection connection = getConnection(dataSourceProperties);
            PreparedStatement statement = connection.prepareStatement(
                "select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA=? and TABLE_NAME=? order by ORDINAL_POSITION asc;")) {
            statement.setString(1, database);
            statement.setString(2, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                return parseResultSet(resultSet);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
