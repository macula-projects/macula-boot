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
package dev.macula.boot.starter.jpa.jpa;

import dev.macula.boot.starter.jpa.jpa.templatequery.TemplateQueryLookupStrategy;
import dev.macula.boot.starter.jpa.jpa.templatequery.TemplateQueryMethodFactory;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.query.JpaQueryMethodFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.core.support.RepositoryProxyPostProcessor;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.lang.Nullable;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * MaculaJpaRepositoryFactory
 * </p>
 * 扩展自{@link JpaRepositoryFactory}
 *
 * @author Rain
 * @version $Id: MaculaJpaRepositoryFactory.java 5896 2015-10-14 03:25:10Z wzp $
 * @since 2011-2-15
 */
@Slf4j
public class MaculaJpaRepositoryFactory extends JpaRepositoryFactory {

    private final PersistenceProvider extractor;
    private final JpaQueryMethodFactory queryMethodFactory;
    private EntityManager entityManager;
    private List<RepositoryProxyPostProcessor> postProcessors;

    /**
     * @param entityManager
     */
    public MaculaJpaRepositoryFactory(EntityManager entityManager) {
        super(entityManager);
        this.entityManager = entityManager;
        this.extractor = PersistenceProvider.fromEntityManager(entityManager);
        this.queryMethodFactory = new TemplateQueryMethodFactory(extractor);

        // add advise for cat
        addRepositoryProxyPostProcessor(CatRepositoryProxyPostProcessor.INSTANCE);
    }

    @Override
    public void addRepositoryProxyPostProcessor(RepositoryProxyPostProcessor processor) {
        super.addRepositoryProxyPostProcessor(processor);
        if (postProcessors == null) {
            postProcessors = new ArrayList<>();
        }
        postProcessors.add(processor);
    }

    @Override
    public <T> T getRepository(Class<T> repositoryInterface, RepositoryComposition.RepositoryFragments fragments) {
        Iterator<RepositoryFragment<?>> it = fragments.iterator();

        while (it.hasNext()) {
            RepositoryFragment<?> rfr = it.next();
            Optional<?> op = rfr.getImplementation();
            if (op.isPresent()) {
                Object customImplementation = op.get();
                if (customImplementation instanceof JpaEntityManagerAware) {
                    // 注入EntityManager
                    ((JpaEntityManagerAware)customImplementation).setEntityManager(entityManager);
                }
            }
        }

        return super.getRepository(repositoryInterface, fragments);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return MaculaSimpleJpaRepository.class;
    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable QueryLookupStrategy.Key key,
        QueryMethodEvaluationContextProvider evaluationContextProvider) {
        return Optional.of(
            TemplateQueryLookupStrategy.create(entityManager, queryMethodFactory, key, evaluationContextProvider));
    }

    /**
     * 获取当前repository的接口和方法名称，放入threadlocal中给cat使用，标识对应SQL的名称
     */
    enum CatRepositoryProxyPostProcessor implements RepositoryProxyPostProcessor {
        /**  */
        INSTANCE;

        @Override
        public void postProcess(ProxyFactory factory, RepositoryInformation repositoryInformation) {
            factory.addAdvice(new CatMethodInterceptor(repositoryInformation));
        }

        class CatMethodInterceptor implements MethodInterceptor {

            private RepositoryInformation repositoryInformation;

            public CatMethodInterceptor(RepositoryInformation repositoryInformation) {
                this.repositoryInformation = repositoryInformation;
            }

            @Override
            public Object invoke(MethodInvocation invocation) throws Throwable {
                Object result = null;
                try {
                    // 获取当前repository的接口和方法名称，放入threadlocal中给cat使用，标识对应SQL的名称
                    String sqlName =
                        repositoryInformation.getRepositoryInterface().getSimpleName() + "." + invocation.getMethod()
                            .getName();

                    if (log.isDebugEnabled()) {
                        log.debug("====sql name is {}", sqlName);
                    }

                    RepositoryMethodNameHolder.set(sqlName);
                    result = invocation.proceed();
                } finally {
                    RepositoryMethodNameHolder.remove();
                }
                return result;
            }
        }
    }
    // end enum
}
