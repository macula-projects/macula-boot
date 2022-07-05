package org.macula.boot.starter.database.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * <b>MyBatisPlusProperties</b> MyBatis Plus模块的可配置属性
 * </p>
 *
 * @author Rain
 * @since 2022-01-22
 */
@ConfigurationProperties(prefix = "macula.mybatis-plus")
@Getter
@Setter
public class MyBatisPlusProperties {
    private Audit audit = new Audit();

    @Getter
    @Setter
    public static class Audit {
        private String createTimeName = "createTime";
        private String createByName = "createBy";
        private String lastUpdateTimeName = "lastUpdateTime";
        private String lastUpdateByName = "lastUpdateBy";
    }
}
