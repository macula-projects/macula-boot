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

package dev.macula.boot.commons.utils;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import org.junit.jupiter.api.Test;
import org.springframework.format.datetime.DateFormatter;

import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * {@code DateTimeTest} is
 *
 * @author rain
 * @since 2022/7/1 10:28
 */

public class DateTimeTest {

    @Test
    public void testDate() throws ParseException {
        System.out.println(OffsetDateTime.now(ZoneId.of("+07:00")).withOffsetSameInstant(ZoneOffset.ofHours(8)));
        System.out.println(OffsetDateTime.now(ZoneId.of("+07:00")).withOffsetSameLocal(ZoneOffset.ofHours(8)));
        System.out.println(LocalDateTime.now().atOffset(ZoneOffset.ofHours(7)));

        System.out.println(
            "带时区日期字符串转UTC时间：" + OffsetDateTime.parse("2016-02-14T10:32:04.150+07:00").toInstant());
        System.out.println("带时区日期字符串转其他时区时间：" + OffsetDateTime.ofInstant(
            OffsetDateTime.parse("2016-02-14T10:32:04.150+07:00").toInstant(), ZoneId.of("+08:00")));
        System.out.println(
            "带时区日期字符串转本地时间：" + LocalDateTime.ofInstant(Instant.parse("2016-02-14T10:32:04.150Z"),
                ZoneId.of("+08:00")));
        System.out.println("本地时间转UTC时间：" + LocalDateTime.now().atZone(ZoneOffset.of("+08:00")).toInstant());
        System.out.println("UTC当前时间：" + Instant.now());
        System.out.println("指定时区的当前时间：" + OffsetDateTime.now(ZoneId.of("+07:00")));
        System.out.println("时区当前时间：" + ZonedDateTime.now());
        System.out.println(
            "Parse测试:" + LocalDate.parse("2019-12-12T23:44:32.000+09:00", DateTimeFormatter.ISO_DATE_TIME));

        DateFormatter formatter = new DateFormatter();
        //formatter.setIso(DateTimeFormat.ISO.DATE_TIME);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        System.out.println("ISO.DATE_TIME:" + formatter.print(new Date(), Locale.getDefault()));
        System.out.println("ISO.DATE_TIME:" + formatter.parse("2019-02-26", Locale.getDefault()));

        DateTimeFormatter formatter1 = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
        System.out.println("DATETIME:" + formatter1.format(OffsetDateTime.now()));
        TemporalAccessor xx = formatter1.parse("19-2-27 上午11:26");
        System.out.println("DATETIME:" + xx);

        System.out.println(LocalDateTime.now().atZone(ZoneId.of("+07:00")).toOffsetDateTime());
        System.out.println("OffsetDateTime to LocaleDateTime:" + OffsetDateTime.now().toLocalDateTime());

        System.out.println("ISO_LOCAL_DATE_TIME:" + DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.of("+09:00"))
            .format(LocalDateTime.now()));
        System.out.println("ISO_LOCAL_DATE_TIME:" + DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("+09:00"))
            .format(OffsetDateTime.now()));
        System.out.println("OffsetDateTime:" + OffsetDateTime.parse("2019-02-27T15:28:14.965+07:00",
            DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("+09:00"))));

        String iso8601 = "2016-02-14T18:32:04.150+07:00";
        ZonedDateTime zdt = ZonedDateTime.parse(iso8601);
        // 转为LocalDateTime需要先把ZondeDateTime转为需要的时区，直接toLocalDateTime是没有换算时区的
        LocalDateTime ldt = zdt.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.of("+08:00"));
        System.out.println(ldt);

        DateTime hutoolDate = DateUtil.parse("2022-05-12T14:01:02+07:00");
        System.out.println(hutoolDate.toJdkDate());
    }
}
