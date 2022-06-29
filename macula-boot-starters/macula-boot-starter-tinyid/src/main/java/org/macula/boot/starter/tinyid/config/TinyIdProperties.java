package org.macula.boot.starter.tinyid.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author du_imba
 */
@ConfigurationProperties(prefix = "macula.tinyid")
@Data
public class TinyIdProperties {

    private String token;
    private String server;
    private Integer readTimeout = 5000;
    private Integer connectTimeout = 5000;
}
