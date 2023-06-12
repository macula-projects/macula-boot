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

package dev.macula.boot.starter.jpa.jpa.templatequery.template;

import cn.hutool.core.util.StrUtil;
import dev.macula.boot.starter.jpa.jpa.TemplateQuery;
import dev.macula.boot.starter.jpa.jpa.templatequery.TemplateQueryMethod;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.util.ClassUtils;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于FreeMarker的SQL语句解析
 *
 * @author <a href="mailto:stormning@163.com">stormning</a>
 * @version V1.0, 2015/8/10.
 */
public class FreemarkerSqlTemplates implements ResourceLoaderAware, InitializingBean {

    private static Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

    private static StringTemplateLoader sqlTemplateLoader = new StringTemplateLoader();

    static {
        cfg.setTemplateLoader(sqlTemplateLoader);
    }

    protected final Log logger = LogFactory.getLog(getClass());

    private String encoding = "UTF-8";

    private Map<String, Boolean> modifiedCache = new ConcurrentHashMap<String, Boolean>();

    private Map<String, String> fixSqlCache = new ConcurrentHashMap<String, String>();

    private PathMatchingResourcePatternResolver resourcePatternResolver;

    private ResourceLoader resourceLoader;

    private List<NamedTemplateResolver> templateResolvers = new ArrayList<NamedTemplateResolver>();

    /**
     * 处理查询方法的TemplateQuery注解，寻找SQL模板，按照Freemarker语法解析
     *
     * @param queryMethod JPA Method
     * @param model       参数集
     * @return SQL语句
     */
    public String process(JpaQueryMethod queryMethod, Map<String, Object> model) {

        TemplateQueryMethod tqMethod = (TemplateQueryMethod)queryMethod;
        String repositoryName = tqMethod.getMethod().getDeclaringClass().getName();
        String methodName = tqMethod.getName();

        try {
            // 如果需要，加载SQL模板
            loadSqltoTemplateIfNeed(tqMethod);

            // 提取Freemarker模板，解析SQL
            StringWriter writer = new StringWriter();
            cfg.getTemplate(getTemplateKey(repositoryName, methodName), encoding).process(model, writer);

            return writer.toString();
        } catch (Exception e) {
            logger.error("process template error. Repository name: " + repositoryName + " methodName:" + methodName, e);
            return StrUtil.EMPTY;
        }
    }

    // 如果需要加载SQL模板
    private void loadSqltoTemplateIfNeed(TemplateQueryMethod tqMethod) throws Exception {
        String repositoryName = tqMethod.getMethod().getDeclaringClass().getName();
        String entityName = tqMethod.getEntityInformation().getJavaType().getName();
        String methodName = tqMethod.getName();

        Boolean modified = modifiedCache.get(getTemplateKey(repositoryName, methodName));
        if (modified == null || modified) {
            // 配置中心的优先级最高，注解中的次之，文件中的优先级最低，通过Map层层覆盖替换
            // 1、====先加载文件中的SQL配置
            // 先按照repository路径寻找SQL模板文件（和包含TemplateQuery的Repository类在一起并同名，后缀是xml或者sftl）
            String pattern = "classpath:/" + ClassUtils.convertClassNameToResourcePath(repositoryName);
            loadResource(repositoryName, pattern);
            if (sqlTemplateLoader.findTemplateSource(getTemplateKey(repositoryName, methodName)) == null) {
                // 找不到，则寻找旧版路劲，在src/resources/sqls/**下面，使用的是EntityName
                pattern = "classpath:/sqls/**/" + entityName;
                loadResource(repositoryName, pattern);
            }

            // 2、====再加载Annotaion中的配置
            TemplateQuery templateQueryAnnontation =
                AnnotationUtils.findAnnotation(tqMethod.getMethod(), TemplateQuery.class);
            if (null != templateQueryAnnontation) {
                String sqlT = templateQueryAnnontation.value();
                if (StrUtil.isNotEmpty(sqlT)) {
                    sqlTemplateLoader.putTemplate(getTemplateKey(repositoryName, methodName), sqlT);
                    modifiedCache.put(getTemplateKey(repositoryName, methodName), Boolean.FALSE);
                }
            }

            // TODO 3、====加载配置中心的SQL，用于快速修复线上SQL问题
        }
    }

    // 根据Pattern寻找SQL模板文件
    private void loadResource(final String repositoryName, String pattern) {
        try {
            for (NamedTemplateResolver resolver : templateResolvers) {
                Resource[] resources = resourcePatternResolver.getResources(pattern + "." + resolver.getSuffix());
                if (resources != null && resources.length > 0) {
                    for (Resource resource : resources) {
                        if (resource.exists()) {
                            // 找到对应SQL模板文件后，把里面的所有SQL都一次性加载
                            resolver.doInTemplateResource(resource, (methodName, content) -> {
                                sqlTemplateLoader.putTemplate(getTemplateKey(repositoryName, methodName), content);
                                modifiedCache.put(getTemplateKey(repositoryName, methodName), Boolean.FALSE);
                            });
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Load sql template file error:", ex);
        }
    }

    // 模板KEY
    private String getTemplateKey(String repositoryName, String methodName) {
        return repositoryName + "." + methodName;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        resourcePatternResolver = new PathMatchingResourcePatternResolver(resourceLoader);
        templateResolvers.add(new XmlNamedTemplateResolver(encoding, resourceLoader));
        templateResolvers.add(new SftlNamedTemplateResolver(encoding));
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
