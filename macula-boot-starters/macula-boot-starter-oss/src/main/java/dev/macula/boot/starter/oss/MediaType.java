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

/*
 * 爱组搭 http://aizuda.com 低代码组件化开发平台
 * ------------------------------------------
 * 受知识产权保护，请勿删除版权申明
 */
package dev.macula.boot.starter.oss;

import org.apache.tika.Tika;

import java.io.IOException;
import java.io.InputStream;

/**
 * oss 媒体类型辅助类
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品
 *
 * @author 青苗
 * @since 2022-06-30
 */
public class MediaType {
    private static Tika TIKA;

    public static Tika getTika() {
        return TIKA;
    }

    /**
     * 提取媒体类型
     *
     * @param is 文件流 {@link InputStream}
     * @return
     * @throws IOException
     */
    public static String detect(InputStream is) throws IOException {
        if (null == TIKA) {
            synchronized (MediaType.class) {
                TIKA = new Tika();
            }
        }
        return TIKA.detect(is);
    }
}
