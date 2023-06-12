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

import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JpaQueryLookupStrategy;
import org.springframework.data.jpa.repository.query.JpaQueryMethodFactory;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;

import javax.persistence.EntityManager;
import java.lang.reflect.Method;

/**
 * .
 * <p>
 *
 * @author <a href="mailto:stormning@163.com">stormning</a>
 * @version V1.0, 2015/8/9.
 */
public class TemplateQueryLookupStrategy implements QueryLookupStrategy {

    private final EntityManager entityManager;

    private QueryLookupStrategy jpaQueryLookupStrategy;

    private JpaQueryMethodFactory queryMethodFactory;

    private EscapeCharacter escapeCharacter = EscapeCharacter.of('\\');

    public TemplateQueryLookupStrategy(EntityManager entityManager, JpaQueryMethodFactory queryMethodFactory, Key key,
        QueryMethodEvaluationContextProvider evaluationContextProvider) {
        this.jpaQueryLookupStrategy =
            JpaQueryLookupStrategy.create(entityManager, queryMethodFactory, key, evaluationContextProvider,
                escapeCharacter);
        ;
        this.queryMethodFactory = queryMethodFactory;
        this.entityManager = entityManager;
    }

    public static QueryLookupStrategy create(EntityManager entityManager, JpaQueryMethodFactory queryMethodFactory,
        Key key, QueryMethodEvaluationContextProvider evaluationContextProvider) {
        return new TemplateQueryLookupStrategy(entityManager, queryMethodFactory, key, evaluationContextProvider);
    }

    @Override
    public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
        NamedQueries namedQueries) {
        if (method.getAnnotation(dev.macula.boot.starter.jpa.jpa.TemplateQuery.class) == null) {
            return jpaQueryLookupStrategy.resolveQuery(method, metadata, factory, namedQueries);
        } else {
            return new TemplateQuery(queryMethodFactory.build(method, metadata, factory), entityManager);
        }
    }

}
