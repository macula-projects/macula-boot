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

package dev.macula.boot.starter.crypto.config;

import cn.hutool.core.util.StrUtil;
import dev.macula.boot.starter.crypto.utils.ConfigCryptoUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 为 Spring Environment 中以 {@code mpw:} 开头的配置值提供读取时解密。
 *
 * <p>处理分为三个时机：环境后处理器包装已有的本地配置，供日志等早期组件读取；
 * {@link LifecycleListener} 在 ApplicationPreparedEvent 中补充包装 bootstrap 初始化器
 * 随后加入的 Nacos 等配置源；Spring Cloud 配置变更事件到达时，再包装刷新后替换的源。
 * 仅保留后期监听，会错过日志初始化；仅保留环境后处理器，则可能错过后加载的远程配置。</p>
 *
 * <p>包装保留原名称、顺序和必要的具体类型，不向最高优先级源复制解密结果。
 * 原始数据仍保存密文，读取时解密且不缓存明文，避免刷新后旧值继续覆盖新配置。
 * 本类及嵌套监听器均通过 META-INF/spring.factories 注册，无需组件扫描。</p>
 *
 * @author rain
 * @since 2023/3/21 17:49
 */
public class CryptoPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** 与 ConfigCryptoUtil、独立 CLI 共用的配置密文标记。 */
    private static final String PREFIX = "mpw:";
    /** 启动密钥，按 Environment 优先级读取；密钥本身不参与解密。 */
    private static final String PASSWORD_KEY = "macula.crypto.key";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 在日志初始化等早期读取发生前，先处理此时已经加载到 Environment 的配置源。
        wrap(environment);
    }

    /**
     * 原位安装解密包装器，供启动和刷新入口共用。
     *
     * <p>replace 保持源的名称和优先级，允许高优先级明文覆盖低优先级密文。
     * 每次调用重新遍历当前环境，以发现新加入或被替换的配置源；没有密钥时沿用原行为，
     * 不修改配置。该方法不提供配置源替换与并发读取之间的原子性保证。</p>
     */
    private static void wrap(ConfigurableEnvironment environment) {
        String key = environment.getProperty(PASSWORD_KEY);
        if (StrUtil.isBlank(key)) {
            return;
        }
        for (PropertySource<?> source : environment.getPropertySources()) {
            PropertySource<?> wrapped = wrapSource(source, key);
            if (wrapped != source) {
                environment.getPropertySources().replace(source.getName(), wrapped);
            }
        }
    }

    /**
     * 根据源的具体类型选择包装方式，同时保持重复安装的幂等性。
     * 具体类型分支必须先于 Map、Enumerable 等父类型分支。
     */
    private static PropertySource<?> wrapSource(PropertySource<?> source, String key) {
        // 启动后期和刷新时都会再次进入。相同密钥复用原包装，避免形成多层解密代理；
        // 密钥不同时从原始 delegate 重建，不将已解密值再次作为密文处理。
        if (source instanceof OriginTrackedMapPropertySource && source.getSource() instanceof DecryptingOriginMap) {
            DecryptingOriginMap wrapped = (DecryptingOriginMap) source.getSource();
            return key.equals(wrapped.key) ? source : new OriginTrackedMapPropertySource(source.getName(),
                new DecryptingOriginMap(wrapped.delegate, key));
        }
        if (source instanceof DecryptingMapPropertySource) {
            DecryptingMapPropertySource wrapped = (DecryptingMapPropertySource) source;
            return key.equals(wrapped.key) ? wrapped : new DecryptingMapPropertySource(wrapped.delegate, key);
        }
        if (source instanceof DecryptingPropertySource) {
            DecryptingPropertySource wrapped = (DecryptingPropertySource) source;
            return key.equals(wrapped.key) ? wrapped : new DecryptingPropertySource(wrapped.getSource(), key);
        }
        if (source instanceof CompositePropertySource) {
            // Nacos 等远程配置可能藏在组合源内部，需递归包装，并保持子源原有顺序。
            CompositePropertySource composite = new CompositePropertySource(source.getName());
            boolean changed = false;
            for (PropertySource<?> child : ((CompositePropertySource) source).getPropertySources()) {
                PropertySource<?> wrapped = wrapSource(child, key);
                composite.addPropertySource(wrapped);
                changed |= wrapped != child;
            }
            // 子源均未变化时保留组合源身份，避免无意义替换和配置适配器缓存失效。
            return changed ? composite : source;
        }
        // 保留系统环境变量的专用类型，避免破坏 Spring Boot 的宽松绑定。
        // 启动密钥仍可从系统环境变量读取。
        if (source instanceof SystemEnvironmentPropertySource) {
            return source;
        }
        // Spring Cloud 的 ExtendedDefaultPropertySource.add 只接收这种具体类型。
        // 降级为普通 Map 会使 bootstrap.yml 中的 spring.application.name 等属性丢失。
        // 该类型为 final，因此保留外层类型，通过内部 Map 视图提供解密能力。
        if (source instanceof OriginTrackedMapPropertySource) {
            return new OriginTrackedMapPropertySource(source.getName(),
                new DecryptingOriginMap((OriginTrackedMapPropertySource) source, key));
        }
        // 部分 Spring Boot 组件会按 MapPropertySource 访问已有源，保留该类型及原 Map。
        if (source instanceof MapPropertySource) {
            return new DecryptingMapPropertySource((MapPropertySource) source, key);
        }
        if (source instanceof EnumerablePropertySource) {
            // 处理 BootstrapPropertySource 等可枚举、但不属于 Map 的配置源。
            return new DecryptingPropertySource((EnumerablePropertySource<?>) source, key);
        }
        // 无法枚举的特殊源保持原样，避免改变框架内部配置聚合器等对象的语义。
        return source;
    }

    @Override
    public int getOrder() {
        // 在环境后处理器中靠后执行，尽量等待配置加载完成；这并不代表晚于所有初始化器。
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 从原始源获取最新值，仅解密带协议前缀的字符串；普通值、空值和启动密钥原样返回。
     * 复用 ConfigCryptoUtil 的协议校验，解密失败时报告源名和属性名，不回退为原始密文。
     */
    private static Object decrypt(PropertySource<?> source, String name, String key) {
        Object value = source.getProperty(name);
        if (!PASSWORD_KEY.equals(name) && value instanceof String && ((String) value).startsWith(PREFIX)) {
            try {
                return ConfigCryptoUtil.decrypt((String) value, key);
            } catch (RuntimeException ex) {
                // 异常消息及 cause 都可能被日志打印，因此不传播包含敏感输入的底层异常。
                throw new IllegalStateException("配置解密失败，propertySource=" + source.getName() + ", property=" + name);
            }
        }
        return value;
    }

    /**
     * 在配置加载和刷新边界补充安装包装器。
     *
     * <p>ApplicationPreparedEvent 位于初始化器执行之后、普通 Bean 创建之前，
     * 可以处理早期扫描时还未加入主环境的 bootstrap/Nacos 配置源。
     * 刷新时依赖 Spring Cloud 在替换源后发布标准同步 EnvironmentChangeEvent，
     * 并优先于后续配置重新绑定监听器执行；不负责主动刷新普通单例中的 @Value。</p>
     *
     * <p>通过事件完整类名识别可选的 Cloud 事件，避免 Crypto Starter 增加 Cloud 运行时依赖。
     * 只处理 source 为 ConfigurableApplicationContext 的标准刷新事件，
     * 无法从旧式仅携带属性名集合的事件中确定待处理的环境。</p>
     */
    public static final class LifecycleListener implements ApplicationListener<ApplicationEvent>, Ordered {
        private static final String ENVIRONMENT_CHANGE_EVENT =
            "org.springframework.cloud.context.environment.EnvironmentChangeEvent";

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            if (event instanceof ApplicationPreparedEvent) {
                wrap(((ApplicationPreparedEvent) event).getApplicationContext().getEnvironment());
            } else if (ENVIRONMENT_CHANGE_EVENT.equals(event.getClass().getName())
                && event.getSource() instanceof ConfigurableApplicationContext) {
                wrap(((ConfigurableApplicationContext) event.getSource()).getEnvironment());
            }
        }

        @Override
        public int getOrder() {
            // 同步刷新事件中，先包装新源，再让配置重新绑定监听器读取新值。
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }

    /**
     * 通用可枚举源的解密代理，保留名称枚举及 OriginLookup，供 Spring 配置绑定和诊断使用。
     * containsProperty 只判断是否存在，不为存在性检查触发解密。
     */
    private static final class DecryptingPropertySource
        extends EnumerablePropertySource<EnumerablePropertySource<?>> implements OriginLookup<String> {

        private final String key;

        private DecryptingPropertySource(EnumerablePropertySource<?> source, String key) {
            super(source.getName(), source);
            this.key = key;
        }

        @Override
        public String[] getPropertyNames() {
            return source.getPropertyNames();
        }

        @Override
        public boolean containsProperty(String name) {
            return source.containsProperty(name);
        }

        @Override
        public Object getProperty(String name) {
            return decrypt(source, name, key);
        }

        @Override
        public Origin getOrigin(String name) {
            return OriginLookup.getOrigin(source, name);
        }
    }

    /**
     * 普通 Map 源的专用代理，保持 MapPropertySource 类型和 getSource 返回的原 Map。
     * 部分 Spring Boot 组件会强转为 MapPropertySource 后访问或更新 Map，不能统一换成通用代理。
     */
    private static final class DecryptingMapPropertySource extends MapPropertySource implements OriginLookup<String> {
        private final MapPropertySource delegate;
        private final String key;

        private DecryptingMapPropertySource(MapPropertySource delegate, String key) {
            super(delegate.getName(), delegate.getSource());
            this.delegate = delegate;
            this.key = key;
        }

        @Override
        public String[] getPropertyNames() {
            return delegate.getPropertyNames();
        }

        @Override
        public boolean containsProperty(String name) {
            return delegate.containsProperty(name);
        }

        @Override
        public Object getProperty(String name) {
            return decrypt(delegate, name, key);
        }

        @Override
        public Origin getOrigin(String name) {
            return OriginLookup.getOrigin(delegate, name);
        }
    }

    /**
     * OriginTrackedMapPropertySource 的只读惰性 Map 视图。
     *
     * <p>该 PropertySource 是 final，不能继承，因此为同类型的新源提供这个 Map 作为底层数据。
     * 通过 delegate 读取原始值并解密，原 Map 保留密文，不保存明文快照。
     * Map#get 和条目读取返回转换后的值，属性名枚举和存在性检查不触发解密。</p>
     */
    private static final class DecryptingOriginMap extends AbstractMap<String, Object> {
        private final OriginTrackedMapPropertySource delegate;
        private final String key;

        private DecryptingOriginMap(OriginTrackedMapPropertySource delegate, String key) {
            this.delegate = delegate;
            this.key = key;
        }

        @Override
        public Object get(Object name) {
            if (!(name instanceof String)) {
                return null;
            }
            String propertyName = (String) name;
            Object value = decrypt(delegate, propertyName, key);
            Origin origin = delegate.getOrigin(propertyName);
            // 外层 OriginTrackedMapPropertySource 会解包此值；同时可继续定位原配置文件及行号。
            return origin == null ? value : OriginTrackedValue.of(value, origin);
        }

        @Override
        public boolean containsKey(Object name) {
            return name instanceof String && delegate.containsProperty((String) name);
        }

        @Override
        public Set<String> keySet() {
            // AbstractMap 默认通过 entrySet 枚举键，会顺带计算值；这里直接枚举名称，避免提前解密。
            return new LinkedHashSet<>(Arrays.asList(delegate.getPropertyNames()));
        }

        @Override
        public Set<Map.Entry<String, Object>> entrySet() {
            // 每次迭代才从原始源读取，配置变化不会被旧条目或旧明文缓存遮蔽；条目禁止修改。
            return new AbstractSet<Map.Entry<String, Object>>() {
                @Override
                public Iterator<Map.Entry<String, Object>> iterator() {
                    Iterator<String> names = Arrays.asList(delegate.getPropertyNames()).iterator();
                    return new Iterator<Map.Entry<String, Object>>() {
                        @Override
                        public boolean hasNext() {
                            return names.hasNext();
                        }

                        @Override
                        public Map.Entry<String, Object> next() {
                            String name = names.next();
                            return new SimpleImmutableEntry<>(name, get(name));
                        }
                    };
                }

                @Override
                public int size() {
                    return delegate.getPropertyNames().length;
                }
            };
        }

        @Override
        public String toString() {
            // 默认 AbstractMap.toString 会遍历并打印已解密条目，调试输出中也不暴露这些值。
            return "DecryptingOriginMap";
        }
    }
}
