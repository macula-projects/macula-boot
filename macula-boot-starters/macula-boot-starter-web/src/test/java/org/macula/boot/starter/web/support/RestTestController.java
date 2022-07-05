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

package org.macula.boot.starter.web.support;

import org.macula.boot.starter.web.annotation.NotControllerResponseAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * {@code WebTest} is
 *
 * @author rain
 * @since 2022/6/29 14:32
 */
@RestController
public class RestTestController {
    @PostMapping("/test")
    public DemoDto test(@Valid @RequestBody DemoDto dto) {
        System.out.println("tests....................");
        System.out.println(dto);
        dto.setDateTime(new Date());
        dto.setLocalDateTime(LocalDateTime.now());
        dto.setZonedDateTime(ZonedDateTime.now());
        return dto;
    }

    @GetMapping("/health")
    @NotControllerResponseAdvice
    public String health() {
        return "sucess";
    }
}
