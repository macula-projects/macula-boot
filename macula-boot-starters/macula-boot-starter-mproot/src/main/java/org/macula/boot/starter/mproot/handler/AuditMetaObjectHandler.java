package org.macula.boot.starter.mproot.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.macula.boot.starter.mproot.config.MyBatisPlusProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
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

    private String createTimeName;
    private String createByName;
    private String lastUpdateTimeName;
    private String lastUpdateByName;

    public AuditMetaObjectHandler(MyBatisPlusProperties.Audit audit) {
        this.createByName = audit.getCreateByName();
        this.createTimeName = audit.getCreateTimeName();
        this.lastUpdateByName = audit.getLastUpdateByName();
        this.lastUpdateTimeName = audit.getLastUpdateTimeName();
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("start insert fill ....");
        this.strictInsertFill(metaObject, createTimeName, () -> LocalDateTime.now(), LocalDateTime.class);
        this.strictInsertFill(metaObject, createByName, String.class, getUserName());
        this.strictInsertFill(metaObject, lastUpdateTimeName, () -> LocalDateTime.now(), LocalDateTime.class);
        this.strictInsertFill(metaObject, lastUpdateByName, String.class, getUserName());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("start update fill ....");

        this.strictUpdateFill(metaObject, lastUpdateTimeName, () -> LocalDateTime.now(), LocalDateTime.class);
        this.strictUpdateFill(metaObject, lastUpdateByName, String.class, getUserName());
    }

    @Override
    public MetaObjectHandler strictFillStrategy(MetaObject metaObject, String fieldName, Supplier<?> fieldVal) {
        // 审计字段不管是否原来存在都要替换
        if ( createTimeName.equals(fieldName) || createByName.equals(fieldName) || lastUpdateTimeName.equals(fieldName) || lastUpdateByName.equals(fieldName) ) {
            Object obj = fieldVal.get();
            if (Objects.nonNull(obj)) {
                metaObject.setValue(fieldName, obj);
            }
            return this;
        }
        return MetaObjectHandler.super.strictFillStrategy(metaObject, fieldName, fieldVal);
    }

    /**
     * 获取 spring security 当前的用户名
     * @return 当前用户名
     */
    private String getUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Optional.ofNullable(authentication).isPresent()) {
            return authentication.getName();
        }
        return "*SYSADM";
    }
}
