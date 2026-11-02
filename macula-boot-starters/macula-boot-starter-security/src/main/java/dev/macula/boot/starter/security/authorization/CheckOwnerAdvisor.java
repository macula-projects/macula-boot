/*
 * Copyright (c) 2026 Macula
 * macula.dev, China
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

package dev.macula.boot.starter.security.authorization;

import dev.macula.boot.starter.security.annotation.CheckOwner;
import dev.macula.boot.starter.security.passkey.PasskeyRequestResolver;
import dev.macula.boot.starter.security.passkey.PasskeyService;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;
import org.springframework.expression.spel.ast.VariableReference;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Enforces one ownership annotation using expression context: input IDs before invocation,
 * or result ownership after a synchronous query. Credential presence never selects a mode.
 *
 * @author Rain
 * @since 5.0.21
 */
public class CheckOwnerAdvisor extends StaticMethodMatcherPointcutAdvisor implements MethodInterceptor {

    private static final long serialVersionUID = 1L;

    private final Supplier<PasskeyService> service;
    private final BooleanSupplier enabled;
    private final PasskeyRequestResolver requestResolver = new PasskeyRequestResolver();
    private final DefaultParameterNameDiscoverer parameterNames = new DefaultParameterNameDiscoverer();
    private final SpelExpressionParser parser = new SpelExpressionParser();

    /**
     * Creates an advisor with lazy configuration dependencies for method-security infrastructure.
     * @param service current passkey service
     * @param enabled whether passkey access is enabled
     */
    public CheckOwnerAdvisor(Supplier<PasskeyService> service, BooleanSupplier enabled) {
        this.service = service;
        this.enabled = enabled;
        setAdvice(this);
        // Share the legacy method-security precedence; registration order keeps its pre checks outside this advisor.
        setOrder(Ordered.LOWEST_PRECEDENCE);
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return annotation(method, targetClass).isPresent();
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Class<?> targetClass = invocation.getThis() == null ? invocation.getMethod().getDeclaringClass()
            : AopUtils.getTargetClass(invocation.getThis());
        MergedAnnotation<CheckOwner> check = annotation(invocation.getMethod(), targetClass);
        Object source = check.isPresent() ? check.getSource() : null;
        if (!(source instanceof Method)) {
            throw denied();
        }
        Method method = (Method)source;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AuthenticationCredentialsNotFoundException("Resource access requires authentication");
        }
        AuthenticationTrustResolverImpl trustResolver = new AuthenticationTrustResolverImpl();
        if (!authentication.isAuthenticated() || trustResolver.isAnonymous(authentication)
            || !StringUtils.hasText(authentication.getName())) {
            throw denied();
        }
        SpelExpression expression;
        References references = new References();
        Set<String> inputs = inputNames(method);
        try {
            expression = parser.parseRaw(check.getString("value"));
            inspect(expression.getAST(), true, inputs, references);
        } catch (RuntimeException ex) {
            throw denied();
        }
        if (references.result == references.input) {
            throw denied();
        }
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
            new ResultRoot(null), method, invocation.getArguments(), parameterNames);
        if (references.input) {
            if (!enabled.getAsBoolean()) {
                throw denied();
            }
            String id = resourceId(evaluate(expression, context));
            ServletRequestAttributes attributes = null;
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
                attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
            }
            String token = requestResolver.resolve(attributes == null ? null : attributes.getRequest());
            if (!service.get().verify(authentication.getName(), id, token)) {
                throw denied();
            }
            return invocation.proceed();
        }
        Object result = invocation.proceed();
        if (result == null) {
            throw denied();
        }
        context.setRootObject(new ResultRoot(result));
        Object owner = evaluate(expression, context);
        if (owner == null || !authentication.getName().equals(owner)) {
            throw denied();
        }
        return result;
    }

    private MergedAnnotation<CheckOwner> annotation(Method method, Class<?> targetClass) {
        Method specific = AopUtils.getMostSpecificMethod(method, targetClass);
        MergedAnnotation<CheckOwner> result = MergedAnnotations.from(specific,
            MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(CheckOwner.class);
        return result.isPresent() ? result : MergedAnnotations.from(method,
            MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(CheckOwner.class);
    }

    private Set<String> inputNames(Method declaration) {
        Set<String> names = new HashSet<>();
        for (int i = 0; i < declaration.getParameterTypes().length; i++) {
            names.add("p" + i);
            names.add("a" + i);
        }
        String[] discovered = parameterNames.getParameterNames(declaration);
        if (discovered != null) {
            java.util.Collections.addAll(names, discovered);
        }
        return names;
    }

    private void inspect(SpelNode node, boolean root, Set<String> inputs, References references) {
        if (node instanceof CompoundExpression) {
            for (int i = 0; i < node.getChildCount(); i++) {
                inspect(node.getChild(i), i == 0, inputs, references);
            }
            return;
        }
        if (node instanceof VariableReference) {
            String name = node.toStringAST().substring(1);
            if (!inputs.contains(name)) {
                throw denied();
            }
            references.input = true;
        } else if (node instanceof PropertyOrFieldReference && root) {
            PropertyOrFieldReference property = (PropertyOrFieldReference)node;
            if (!"returnObject".equals(property.getName())) {
                throw denied();
            }
            references.result = true;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            inspect(node.getChild(i), true, inputs, references);
        }
    }

    private Object evaluate(SpelExpression expression, MethodBasedEvaluationContext context) {
        try {
            return expression.getValue(context);
        } catch (RuntimeException ex) {
            throw denied();
        }
    }

    private String resourceId(Object value) {
        if (value instanceof String && StringUtils.hasText((String)value)) {
            return (String)value;
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer
            || value instanceof Long || value instanceof BigInteger) {
            return value.toString();
        }
        throw denied();
    }

    private CheckOwnerAccessDeniedException denied() {
        return new CheckOwnerAccessDeniedException("Resource access denied");
    }

    /**
     * Records contexts referenced by a parsed expression, excluding string literals.
     * @since 5.0.21
     */
    private static class References {
        private boolean result;
        private boolean input;
    }

    /**
     * Provides the original returnObject expression root for existing callers.
     * @since 5.0.21
     */
    public static class ResultRoot {
        private final Object returnObject;

        /**
         * Creates an expression root.
         * @param returnObject actual method result
         */
        public ResultRoot(Object returnObject) {
            this.returnObject = returnObject;
        }

        /**
         * Returns the actual method result.
         * @return method result
         */
        public Object getReturnObject() {
            return returnObject;
        }
    }
}
