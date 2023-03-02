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

package dev.macula.boot.starter.web.support;

import dev.macula.boot.starter.web.annotation.Sensitive;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
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
public class DemoDto {

    @NotEmpty(message = "{name.notempty}")
    private String name;

    @NotEmpty
    private String password;

    @Length(min = 5, max = 25, message = "{key.length}")
    private String key;

    @Pattern(regexp = "[012]", message = "无效的状态标志")
    private String state;

    private Date dateTime;

    private LocalDateTime localDateTime;

    private LocalDate localDate;

    private ZonedDateTime zonedDateTime;

    @Sensitive(value = Sensitive.Type.MOBILE)
    private String mobile;

}
