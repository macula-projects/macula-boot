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

package dev.macula.boot.starter.jpa.jpa.templatequery;

import cn.hutool.core.bean.BeanUtil;
import dev.macula.boot.starter.jpa.jpa.templatequery.transformer.BeanTransformerAdapter;
import dev.macula.boot.starter.jpa.jpa.templatequery.transformer.SmartTransformer;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.QueryParameter;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;
import org.springframework.data.jpa.repository.query.JpaParameters;
import org.springframework.data.repository.query.Parameter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QueryBuilder
 *
 * @author <a href="mailto:stormning@163.com">stormning</a>
 * @version V1.0, 2015/8/11.
 */
public class QueryBuilder {

    private static final Pattern ORDERBY_PATTERN_1 =
        Pattern.compile("order\\s+by.+?$", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static Map<Class<?>, ResultTransformer> transformerCache = new ConcurrentHashMap<>();

    @SuppressWarnings("deprecation")
    public static void transform(NativeQuery<?> query, Class<?> clazz) {
        ResultTransformer transformer;
        if (Map.class.isAssignableFrom(clazz)) {
            transformer = Transformers.ALIAS_TO_ENTITY_MAP;
        } else if (Number.class.isAssignableFrom(clazz) || clazz.isPrimitive() || String.class.isAssignableFrom(
            clazz) || Date.class.isAssignableFrom(clazz)) {
            transformer = transformerCache.computeIfAbsent(clazz, SmartTransformer::new);
        } else {
            transformer = transformerCache.computeIfAbsent(clazz, BeanTransformerAdapter::new);
        }
        query.setResultTransformer(transformer);
    }

    private static String wrapCountQuery(String query) {
        return "select count(*) from (" + query + ") as ctmp";
    }

    private static String cleanOrderBy(String query) {
        Matcher matcher = ORDERBY_PATTERN_1.matcher(query);
        StringBuffer sb = new StringBuffer();
        int i = 0;
        while (matcher.find()) {
            String part = matcher.group(i);
            if (canClean(part)) {
                matcher.appendReplacement(sb, "");
            } else {
                matcher.appendReplacement(sb, part);
            }
            i++;
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static boolean canClean(String orderByPart) {
        return orderByPart != null && (!orderByPart.contains(")") || StringUtils.countOccurrencesOf(orderByPart,
            ")") == StringUtils.countOccurrencesOf(orderByPart, "("));
    }

    public static String toCountQuery(String query) {
        return wrapCountQuery(cleanOrderBy(query));
    }

    /**
     * 给Hibernate查询参数设置值
     *
     * @param query  NativeQuery
     * @param params 参数
     */
    @SuppressWarnings("rawtypes")
    public static void setParams(NativeQuery<?> query, Map<String, Object> params) {

        Collection<QueryParameter> nps = query.getParameterMetadata().getNamedParameters();
        if (nps != null) {
            for (QueryParameter<?> param : nps) {
                String key = param.getName();
                Object arg = params.get(key);
                if (arg == null) {
                    query.setParameter(key, null);
                } else if (arg.getClass().isArray()) {
                    query.setParameterList(key, (Object[])arg);
                } else if (arg instanceof Collection) {
                    query.setParameterList(key, ((Collection)arg));
                } else if (arg.getClass().isEnum()) {
                    query.setParameter(key, ((Enum)arg).ordinal());
                } else {
                    query.setParameter(key, arg);
                }
            }
        }
    }

    /**
     * 将接口中定义的查询方法参数转换为Map，直接使用参数名称作为KEY
     *
     * @param parameters JPA参数
     * @param values 对应的值
     * @return DataModel
     */
    public static Map<String, Object> transValuesToDataModel(JpaParameters parameters, Object[] values) {
        // gen model
        Map<String, Object> dataModel = new HashMap<>();
        for (int i = 0; i < parameters.getNumberOfParameters(); i++) {
            Object value = values[i];
            Parameter parameter = parameters.getParameter(i);
            String name = parameter.getName().orElse(null);
            if (name != null) {
                dataModel.put(name, value);
            }
        }
        return dataModel;
    }

    /**
     * 将接口中定义的查询方法参数转换为Map，原子类型直接放入map，Bean或者Map转换为Map后放入， 为了防止不同的bean或者map属性重名，最终的MAP的KEY是参数名称.属性名称
     *
     * @param parameters 参数定义
     * @param values     参数值
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> transValuesToMap(JpaParameters parameters, Object[] values) {
        // gen model
        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < parameters.getNumberOfParameters(); i++) {
            Object value = values[i];
            Parameter parameter = parameters.getParameter(i);
            if (value != null && parameter.isBindable()) {
                if (!QueryBuilder.isValidValue(value)) {
                    continue;
                }
                Class<?> clz = value.getClass();
                if (clz.isPrimitive() || clz.isEnum() || String.class.isAssignableFrom(
                    clz) || Number.class.isAssignableFrom(clz) || Date.class.isAssignableFrom(clz)) {
                    // 原子类型
                    params.put(parameter.getName().get(), value);
                } else if (clz.isArray() || value instanceof Collection<?>) {
                    // 集合
                    params.put(parameter.getName().get(), value);
                } else {
                    // 如果方法中的参数为Map或者Bean类型，则当成Map放入参数的Map中
                    // 为了防止不同的bean或者map属性重名，最终的MAP的KEY是参数名称.属性名称
                    Map<String, Object> map = BeanUtil.beanToMap(value);
                    for (String key : map.keySet()) {
                        Object mapValue = map.get(key);
                        if (isValidValue(mapValue)) {
                            params.put(parameter.getName().get() + "." + key, mapValue);
                        }
                    }
                }
            }
        }
        return params;
    }

    public static boolean isValidValue(Object object) {
        if (object == null) {
            return false;
        }
        /*if (object instanceof Number && ((Number) object).longValue() == 0) {
            return false;
		}*/
        return !(object instanceof Collection && CollectionUtils.isEmpty((Collection<?>)object));
    }

    public static void main(String[] args) {
        String t1 = "select * from user order by id";
        String t2 = "select * from abc order by xxx(convert( resName using gbk )) collate gbk_chinese_ci asc";
        String t3 =
            "select count * from ((select * from aaa group by a order by a) union all (select * from aaa group by a order by a))";
        String t4 =
            "SELECT\n" + "  t1.*,t2.name AS dictionaryName,t3.name AS classifyName\n" + "FROM res_data_element t1 LEFT JOIN sys_business_dictionary t2 ON  t1.dictionary_id = t2.id\n" + "  LEFT JOIN sys_business_dictionary t3 ON  t1.classify = t3.id\n" + "WHERE  1=1\n" + "       AND  t1.is_history_version = 1\n" + "       AND t1.status = 1\n" + "       AND (t1.name LIKE '%${nameOrCodeOrENameOrCompany}%'\n" + "         OR\n" + "         t1.code LIKE '%${nameOrCodeOrENameOrCompany}%'\n" + "         OR\n" + "         t1.englishname LIKE '%${nameOrCodeOrENameOrCompany}%'\n" + "         OR\n" + "         t1.submit_company LIKE '%${nameOrCodeOrENameOrCompany}%')";
        System.out.println(QueryBuilder.toCountQuery(t1));
        System.out.println(QueryBuilder.toCountQuery(t2));
        System.out.println(QueryBuilder.toCountQuery(t3));
        //        System.out.println(QueryBuilder.toCountQuery(t4));
    }
}
