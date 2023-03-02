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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * {@code WebTestController} is
 *
 * @author rain
 * @since 2022/6/30 10:24
 */

@Controller
public class WebTestController {
    @PostMapping("/getdate")
    public String testDate(@RequestParam(value = "date", required = false) Date date,
        @RequestParam(value = "local", required = false) LocalDateTime local,
        @RequestParam(value = "zoned", required = false) ZonedDateTime zoned) {
        System.out.println("date=" + date + ", local=" + local + ", zoned=" + zoned);
        return "";
    }
}
