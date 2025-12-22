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

package dev.macula.boot.starter.mp.handler;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.starter.mp.config.MyBatisPlusProperties;
import dev.macula.boot.starter.mp.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * <p>
 * <b>MaculaMetaObjectHandler</b> 自动填充的实现
 * </p>
 *
 * @author Rain
 * @since 2022-01-23
 */

@Slf4j
public class AuditMetaObjectHandler implements MetaObjectHandler {

    private final String createTimeName;
    private final String createByName;
    private final String lastUpdateTimeName;
    private final String lastUpdateByName;

    public AuditMetaObjectHandler(MyBatisPlusProperties.Audit audit) {
        this.createByName = audit.getCreateByName();
        this.createTimeName = audit.getCreateTimeName();
        this.lastUpdateByName = audit.getLastUpdateByName();
        this.lastUpdateTimeName = audit.getLastUpdateTimeName();
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("start insert fill ....");
        this.strictInsertFill(metaObject, createTimeName, LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, createByName, String.class, getCurrentUser());
        this.strictInsertFill(metaObject, lastUpdateTimeName, LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, lastUpdateByName, String.class, getCurrentUser());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("start update fill ....");

        this.strictUpdateFill(metaObject, lastUpdateTimeName, LocalDateTime::now, LocalDateTime.class);
        this.strictUpdateFill(metaObject, lastUpdateByName, String.class, getCurrentUser());
    }

    @Override
    public MetaObjectHandler strictFillStrategy(MetaObject metaObject, String fieldName, Supplier<?> fieldVal) {
        // 审计字段不管是否原来存在都要替换
        if (createTimeName.equals(fieldName) || createByName.equals(fieldName) || lastUpdateTimeName.equals(
            fieldName) || lastUpdateByName.equals(fieldName)) {
            Object obj = fieldVal.get();
            if (Objects.nonNull(obj)) {
                metaObject.setValue(fieldName, obj);
            }
            return this;
        }
        return MetaObjectHandler.super.strictFillStrategy(metaObject, fieldName, fieldVal);
    }

    private String getCurrentUser() {
        String name = SecurityUtils.getCurrentUser();
        if (StrUtil.isEmpty(name)) {
            name = SecurityConstants.BACKGROUND_USER;
        }
        return name;
    }
}
