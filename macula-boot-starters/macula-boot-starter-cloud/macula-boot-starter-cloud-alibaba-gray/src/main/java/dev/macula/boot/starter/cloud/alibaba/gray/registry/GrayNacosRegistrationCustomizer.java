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

package dev.macula.boot.starter.cloud.alibaba.gray.registry;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.nacos.registry.NacosRegistration;
import com.alibaba.cloud.nacos.registry.NacosRegistrationCustomizer;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.context.GrayVersionMetaHolder;

/**
 * {@code GrayNacosRegistrationCustomizer} 读取灰度metadata标签值放入上下文
 *
 * @author rain
 * @since 2023/9/26 13:40
 */
public class GrayNacosRegistrationCustomizer implements NacosRegistrationCustomizer {
    @Override
    public void customize(NacosRegistration registration) {
        // 实例向注册中心注册时读取metadata中的gray标识设置上下文
        GrayVersionMetaHolder.clear();
        String grayVersion = registration.getMetadata().get(GlobalConstants.GRAY_VERSION_TAG);
        if (StrUtil.isNotEmpty(grayVersion)) {
            GrayVersionMetaHolder.setGrayVersion(grayVersion);
        }
    }
}
