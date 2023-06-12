/*
 * Copyright 2004-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.macula.boot.starter.jpa.hibernate.audit;

import org.springframework.data.domain.Persistable;

import java.io.Serializable;

/**
 * <p>
 * <b>ChangedRecord</b> 数据变化实体
 * </p>
 *
 * @author Rain
 * @version $Id: AuditChanged.java 4375 2013-08-19 05:30:23Z wilson $
 * @since 2011-1-7
 */
public class AuditChanged implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 变化的表名 */
    private String tableName;

    /** 变化的字段名 */
    private String columnName;

    /** 变化的数据ID */
    private String dataId;

    /** 变化前的数据 */
    private Object oldValue;

    /** 变化后的数据 */
    private Object newValue;

    /** 变化批次 */
    private String batchNo;

    /** 变更人 */
    private String modifiedBy;

    /** 变化的实体 */
    private Persistable<?> entity;

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Persistable<?> getEntity() {
        return entity;
    }

    public void setEntity(Persistable<?> entity) {
        this.entity = entity;
    }
}
