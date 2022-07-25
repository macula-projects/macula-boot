/*
 * Copyright (c) 2022 Macula
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

package dev.macula.boot.starter.database.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * {@code DateTimeTest} is
 *
 * @author rain
 * @since 2022/7/1 08:24
 */
public class DateTimeTest {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/test?serverTimezone=GMT%2B8", "root", "");
        PreparedStatement st = conn.prepareStatement("insert into tb_java8date (t_date, t_time, t_datetime, t_timestamp) values (?, ?, ?, ?)");
        st.setObject(1, LocalDate.now());
        st.setObject(2, LocalTime.now());
        st.setObject(3, LocalDateTime.now());
        st.setObject(4, ZonedDateTime.now());
        st.execute();
        st.close();


        st = conn.prepareStatement("select t_date, t_time, t_datetime, t_timestamp from tb_java8date");
        ResultSet data = st.executeQuery();
        while (data.next()) {
            LocalDate date = data.getObject(1, LocalDate.class);
            LocalTime time = data.getObject(2, LocalTime.class);
            LocalDateTime datetime = data.getObject(3, LocalDateTime.class);
            ZonedDateTime timestamp = data.getObject(4, ZonedDateTime.class);
            System.out.println(date + ", " + time + ", " + datetime + ", " + timestamp);
        }
        st.close();

        conn.close();
    }
}
