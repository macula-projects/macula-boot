/*
 * Copyright (c) 2026 Macula
 * macula.dev, China
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
package dev.macula.boot.starter.observability.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Macula 统一可观测性增量配置。
 *
 * @author Rain
 * @since 6.1.0
 */
@ConfigurationProperties("macula.observability")
public class ObservabilityProperties {

    private boolean enabled = true;

    private final Logging logging = new Logging();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Logging getLogging() {
        return logging;
    }

    /**
     * 日志导出的安全捕获配置。
     *
     * @since 6.1.0
     */
    public static class Logging {

        private List<String> captureMdcAttributes = new ArrayList<>();

        public List<String> getCaptureMdcAttributes() {
            return captureMdcAttributes;
        }

        public void setCaptureMdcAttributes(List<String> captureMdcAttributes) {
            this.captureMdcAttributes = captureMdcAttributes == null ? new ArrayList<>()
                : new ArrayList<>(captureMdcAttributes);
        }
    }
}
