package dev.macula.boot.starter.mp.test;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

public class TimeTypeTest {
    // 数据库连接参数（serverTimezone 动态修改）
    private static String urlTemplate = "jdbc:mysql://localhost:3306/test?serverTimezone=%s";
    private static String user = "root";
    private static String password = "";

    public static void main(String[] args) throws SQLException {

        testJava8Time(TimeZone.getTimeZone("Asia/Shanghai"), "Asia/Shanghai");

        testJava8Time(TimeZone.getTimeZone("Asia/Shanghai"), "UTC");

        testJava8Time(TimeZone.getTimeZone("UTC"), "Asia/Shanghai");

        testJava8Time(TimeZone.getTimeZone("UTC"), "UTC");
    }

    private static void testJava8Time(TimeZone jreTimezone, String serverTimezone) throws SQLException {
        TimeZone.setDefault(jreTimezone); // 设置JRE默认时区
        String url = String.format(urlTemplate, serverTimezone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        System.out.println("测试场景：");
        System.out.println("JRE时区：" + jreTimezone.getID() + "（UTC+8）");
        System.out.println("serverTimezone：" + serverTimezone + "（UTC）");
        System.out.println("当前物理时间：UTC 2025-11-10 14:40:00（北京时间 22:40:00）");
        System.out.println("====================================");

        // 1. 定义Java 8时间对象（基于JRE时区）
        Instant instant = Instant.ofEpochMilli(1762785600000L); // UTC时间戳（核心参考）
        ZonedDateTime shanghaiTime = instant.atZone(jreTimezone.toZoneId());
        LocalDateTime localDateTime = shanghaiTime.toLocalDateTime();
        LocalDate localDate = shanghaiTime.toLocalDate();
        LocalTime localTime = shanghaiTime.toLocalTime();
        Date date = new Date(instant.toEpochMilli());

        System.out.println("Java 8 时间对象（JRE时区）：" + jreTimezone.getID());
        System.out.println("Instant（UTC）：" + instant);
        System.out.println("ZonedDateTime：" + shanghaiTime.format(formatter));
        System.out.println("LocalDateTime：" + localDateTime.format(formatter));
        System.out.println("LocalDate：" + localDate);
        System.out.println("LocalTime：" + localTime);
        System.out.println("------------------------------------");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(url, user, password);

            // 2. 插入数据（Java 8时间类型通过JDBC转换）
            ps = conn.prepareStatement("INSERT INTO time_type_test " +
                    "(dt_datetime, dt_datetime2, dt_timestamp, dt_timestamp2, dt_date, dt_time) VALUES (?, ?, ?, ?, ?, ?)");
            // 注意：MySQL JDBC驱动对LocalDateTime等类型的支持需要版本8.0+
            ps.setObject(1, localDateTime);      // DATETIME ← LocalDateTime
            ps.setObject(2, shanghaiTime);       // DATETIME <- ZonedDateTime
            ps.setObject(3, instant);            // TIMESTAMP ← Instant（推荐，直接对应UTC）
            ps.setObject(4, localDateTime);      // TIMESTAMP <- LocalDateTime
            ps.setObject(5, localDate);          // DATE ← LocalDate
            ps.setObject(6, localTime);          // TIME ← LocalTime
            ps.executeUpdate();
            System.out.println("数据插入完成");

            // 3. 查询数据（用Java 8类型接收）
            ps = conn.prepareStatement("SELECT * FROM time_type_test ORDER BY id DESC LIMIT 1");
            rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("\nJava查询结果（转换为JRE时区显示）：");

                // 2. 获取TIMESTAMP → 兼容转换为Instant（核心修正）
                // 方案：先获取ZonedDateTime，再转换为Instant
                ZonedDateTime dbZoned = rs.getObject("dt_timestamp", ZonedDateTime.class);
                Instant dbInstant = dbZoned.toInstant(); // 转为UTC时间戳


                LocalDateTime dbDateTime = rs.getObject("dt_datetime", LocalDateTime.class);
                ZonedDateTime dbZonedDateTime = rs.getObject("dt_datetime2", ZonedDateTime.class);
                LocalDateTime dt_timestamp2 = rs.getObject("dt_timestamp2", LocalDateTime.class);
                LocalDate dbDate = rs.getObject("dt_date", LocalDate.class);
                LocalTime dbTime = rs.getObject("dt_time", LocalTime.class);
                System.out.println("DATETIME → LocalDateTime：" + dbDateTime.format(formatter));
                System.out.println("TIMESTAMP → Instant（UTC）：" + dbInstant);
                System.out.println("TIMESTAMP → JRE时间：" + dbInstant.atZone(jreTimezone.toZoneId()).format(formatter));
                System.out.println("DATE → LocalDate：" + dbDate);
                System.out.println("TIME → LocalTime：" + dbTime);
                System.out.println("DATETIME2 -> ZonedDateTime:" + dbZonedDateTime);
                System.out.println("TIMESTAMP2 -> LocalDateTime:" + dt_timestamp2);
            }

            // 4. 数据库原始存储（通过数据库函数查看）
            ps = conn.prepareStatement("SELECT " +
                    "dt_datetime, dt_timestamp, dt_date, dt_time, " +
                    "UNIX_TIMESTAMP(dt_timestamp) AS ts_utc " +
                    "FROM time_type_test ORDER BY id DESC LIMIT 1");
            rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("\n数据库原始存储（数据库时区=UTC）：");
                System.out.println("DATETIME: " + rs.getString("dt_datetime"));
                System.out.println("TIMESTAMP: " + rs.getString("dt_timestamp"));
                System.out.println("DATE: " + rs.getString("dt_date"));
                System.out.println("TIME: " + rs.getString("dt_time"));
                System.out.println("TIMESTAMP对应的UTC时间戳：" + rs.getLong("ts_utc") * 1000);
            }

        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            if (conn != null) conn.close();
        }
    }
}