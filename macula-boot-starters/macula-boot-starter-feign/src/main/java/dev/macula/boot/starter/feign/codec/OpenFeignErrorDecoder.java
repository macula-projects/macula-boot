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

package dev.macula.boot.starter.feign.codec;

import cn.hutool.json.JSONUtil;
import dev.macula.boot.exception.BizCheckException;
import dev.macula.boot.exception.BizException;
import dev.macula.boot.result.Result;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * {@code OpenFeignErrorDecoder} 解决Feign的异常包装，统一返回结果
 *
 * @author rain
 * @since 2022/7/23 12:35
 */
@Slf4j
public class OpenFeignErrorDecoder implements ErrorDecoder {

    /**
     * Feign异常解析
     *
     * @param methodKey 方法名
     * @param response  响应体
     * @return ApiException
     */
    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("feign client error,response is {}:", response);
        try {
            // 获取数据
            String body = Util.toString(response.body().asReader(Charset.defaultCharset()));

            try {
                HttpStatus status = HttpStatus.valueOf(response.status());
                if (status.is4xxClientError() || status.is5xxServerError()) {
                    Result<?> resultData = JSONUtil.toBean(body, Result.class);
                    if (!resultData.isSuccess()) {
                        String errMsg = "Feign提供方异常：";
                        if (resultData.getCause() != null && !"null".equals(resultData.getCause())) {
                            errMsg = resultData.getCause();
                        } else {
                            errMsg += resultData.getMsg();
                        }
                        // 业务检查异常
                        if (status == HttpStatus.NOT_EXTENDED) {
                            return new BizCheckException(resultData.getCode(), resultData.getMsg(), errMsg);
                        }
                        // 业务类异常
                        return new BizException(resultData.getCode(), resultData.getMsg(), errMsg);
                    }
                } else {
                    return new BizException("Feign提供方异常：" + response.reason());
                }
            } catch (Exception ex) {
                return new BizException(body);
            }

        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return new BizException("Feign提供方异常");
    }
}
