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

package dev.macula.boot.starter.security.utils;

import cn.hutool.json.JSONUtil;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * <p>
 * <b>ResponseUtils</b> 响应助手
 * </p>
 *
 * @author Rain
 * @since 2022-02-20
 */
public class ResponseUtils {

    @SneakyThrows
    public static void writeErrorInfo(HttpServletResponse response, ApiResultCode resultCode) {
        switch (resultCode) {
            case ACCESS_UNAUTHORIZED:
            case TOKEN_INVALID_OR_EXPIRED:
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                break;
            case TOKEN_ACCESS_FORBIDDEN:
                response.setStatus(HttpStatus.FORBIDDEN.value());
                break;
            default:
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                break;
        }

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        String body = JSONUtil.toJsonStr(Result.failed(resultCode));
        out.write(body);
        out.flush();
        out.close();
    }
}