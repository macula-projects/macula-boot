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

package dev.macula.boot.starter.web.test.support;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.macula.boot.starter.web.annotation.Sensitive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.springframework.boot.autoconfigure.security.SecurityProperties;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * {@code DemoDto} is
 *
 * @author rain
 * @since 2022/6/29 14:34
 */
@Data
public class DemoDto implements Serializable {

    @NotEmpty(message = "{name.notempty}")
    private String name;

    @NotEmpty
    private String password;

    @Length(min = 5, max = 25, message = "{key.length}")
    private String key;

    @Pattern(regexp = "[012]", message = "无效的状态标志")
    private String state;

    private Date dateTime = new Date();

    //@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime localDateTime;

    private LocalDate localDate;

    private ZonedDateTime zonedDateTime;

    @Sensitive(value = Sensitive.Type.MOBILE)
    private String mobile;

    private int intNum;

    private Integer integerNum;

    private Long longNum = 1022L;

    private BigInteger bigInteger = new BigInteger("12333");

    private BigDecimal bigDecimal = new BigDecimal("1234.344");

    private SecurityProperties.User user;

}
