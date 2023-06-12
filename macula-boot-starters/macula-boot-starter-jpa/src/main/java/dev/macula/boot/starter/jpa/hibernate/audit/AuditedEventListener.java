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

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import dev.macula.boot.starter.jpa.domain.support.AuditorAwareStub;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.Persistable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p> <b>UpdateEventListener</b> 是实体变更事件监听器 </p>
 *
 * @author Rain
 * @author Wilson Luo
 * @version $Id: AuditedEventListener.java 5663 2015-06-19 09:49:03Z wzp $
 * @since 2011-4-7
 */
public class AuditedEventListener implements PostUpdateEventListener, PostDeleteEventListener, ApplicationContextAware {

	private static final long serialVersionUID = 1L;

	private static Map<String, String> tableNameMap = new ConcurrentHashMap<String, String>();

	private static Map<String, String> columnNameMap = new ConcurrentHashMap<String, String>();

	private static Map<String, Boolean> entityAuditedMap = new ConcurrentHashMap<String, Boolean>();

	private static Map<String, Boolean> propertyAuditedMap = new ConcurrentHashMap<String, Boolean>();
	private final Logger log = LoggerFactory.getLogger(getClass());
	private ApplicationContext applicationContext;

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		// 判断实体是否需要记录日志
		if (isEntityAudited(event.getEntity())) {
			if (event.getEntity() instanceof Persistable) {
				Persistable<?> entity = (Persistable<?>)event.getEntity();
				Object id = entity.getId();
				if (null != id) {
					// 表名
					String tableName = getTableName(entity);
					// 实体ID
					String dataId = id.toString();
					// 本次变化批号
					String batchNo = IdUtil.randomUUID();

					String[] propertyNames = event.getPersister().getPropertyNames();
					for (int i = 0; i < propertyNames.length; i++) {
						String propertyName = propertyNames[i];
						Object oldValue = event.getOldState()[i];
						Object newValue = event.getState()[i];

						// 判断是否需要记录日志并且数据发生变化
						if (isPropertyAudited(event.getEntity(), propertyName) && isChanged(oldValue, newValue)) {
							// 获取列名
							String columnName = getColumnName(entity, propertyName);

							// 构建变化实体
							AuditChanged chg = new AuditChanged();
							chg.setBatchNo(batchNo);
							chg.setTableName(tableName);
							chg.setColumnName(columnName);
							chg.setDataId(dataId);
							chg.setOldValue(oldValue);
							chg.setNewValue(newValue);
							chg.setEntity(entity);
							chg.setModifiedBy(AuditorAwareStub.getCurrentUser());

							// 发布事件
							if (log.isDebugEnabled()) {
								log.debug("Publish AuditChangedEvent: {}", JSONUtil.toJsonStr(chg));
							}
							applicationContext.publishEvent(new AuditChangedEvent(chg));
						}
					}
				}
			}
		}
	}

	@Override
	public void onPostDelete(PostDeleteEvent event) {
		// 判断实体是否需要记录日志
		if (isEntityAudited(event.getEntity())) {
			if (event.getEntity() instanceof Persistable) {
				Persistable<?> entity = (Persistable<?>)event.getEntity();
				Object id = entity.getId();
				if (null != id) {
					// 表名
					String tableName = getTableName(entity);
					// 实体ID
					String dataId = id.toString();
					// 本次变化批号
					String batchNo = IdUtil.randomUUID();
					AuditDeleted chg = new AuditDeleted();
					chg.setBatchNo(batchNo);
					chg.setTableName(tableName);
					chg.setDataId(dataId);
					chg.setEntity(entity);
					chg.setModifiedBy(AuditorAwareStub.getCurrentUser());
					// 发布事件
					if (log.isDebugEnabled()) {
						log.debug("Publish AuditDeletedEvent: {}", JSONUtil.toJsonStr(chg));
					}
					applicationContext.publishEvent(new AuditDeletedEvent(chg));
				}
			}
		}
	}

	/**
	 * 获取表名
	 */
	private String getTableName(Object entity) {
		String key = entity.getClass().getName();

		String tableName = tableNameMap.get(key);
		if (StringUtils.isEmpty(tableName)) {
			Table ann = entity.getClass().getAnnotation(Table.class);
			if (null != ann) {
				tableName = ann.name();
			} else {
				tableName = entity.getClass().getSimpleName();
			}
			tableNameMap.put(key, tableName);
		}

		return tableName;
	}

	/**
	 * 获取列名
	 */
	private String getColumnName(Object entity, String propertyName) {
		String key = entity.getClass().getName() + "." + propertyName;

		String columnName = columnNameMap.get(key);
		if (columnName == null) {
			Column ann = null;
			Field f = ReflectionUtils.findField(entity.getClass(), propertyName);
			if (f != null) {
				ann = f.getAnnotation(Column.class);
			}
			if (ann == null) {
				Method m = ReflectionUtils.findMethod(entity.getClass(), "get" + StringUtils.capitalize(propertyName));
				if (m == null) {
					m = ReflectionUtils.findMethod(entity.getClass(), "is" + StringUtils.capitalize(propertyName));
				}
				if (m != null) {
					ann = m.getAnnotation(Column.class);
				}
			}
			if (ann != null) {
				columnName = ann.name();
			} else {
				columnName = propertyName;
			}
			columnNameMap.put(key, columnName);
		}

		return columnName;
	}

	/**
	 * 判断实体是否需要记录日志
	 */
	private boolean isEntityAudited(Object entity) {
		String key = entity.getClass().getName();

		Boolean auditable = entityAuditedMap.get(key);
		if (auditable == null) {
			Auditable ann = entity.getClass().getAnnotation(Auditable.class);
			auditable = ann != null && ann.value();
			entityAuditedMap.put(key, auditable);
		}

		return auditable;
	}

	/**
	 * 判断字段属性是否需要记录日志
	 */
	private boolean isPropertyAudited(Object entity, String propertyName) {
		String key = entity.getClass().getName() + "." + propertyName;

		Boolean auditable = propertyAuditedMap.get(key);
		if (auditable == null) {
			Auditable ann = null;
			Field f = ReflectionUtils.findField(entity.getClass(), propertyName);
			if (f != null) {
				ann = f.getAnnotation(Auditable.class);
			}
			if (ann == null) {
				Method m = ReflectionUtils.findMethod(entity.getClass(), "get" + StringUtils.capitalize(propertyName));
				if (m == null) {
					m = ReflectionUtils.findMethod(entity.getClass(), "is" + StringUtils.capitalize(propertyName));
				}
				if (m != null) {
					ann = m.getAnnotation(Auditable.class);
				}
			}
			auditable = (ann != null && ann.value());
			propertyAuditedMap.put(key, auditable);
		}
		return auditable;
	}

	/**
	 * 比较数据前后是否发生变化
	 */
	private boolean isChanged(Object oldValue, Object newValue) {
		if (oldValue instanceof Persistable || newValue instanceof Persistable || oldValue instanceof Collection || newValue instanceof Collection || oldValue == newValue) {
			return false;
		}
		if (oldValue == null || newValue == null) {
			return true;
		}
		return !oldValue.equals(newValue);
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean requiresPostCommitHanding(EntityPersister arg0) {
		return false;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
