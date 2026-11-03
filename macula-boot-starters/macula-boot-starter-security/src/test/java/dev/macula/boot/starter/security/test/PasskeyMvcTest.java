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

package dev.macula.boot.starter.security.test;

import dev.macula.boot.security.PasskeyIssuer;
import dev.macula.boot.security.PasskeyResource;
import dev.macula.boot.starter.security.annotation.Passkey;
import dev.macula.boot.starter.security.passkey.HmacPasskeyService;
import dev.macula.boot.starter.security.passkey.PasskeyService;
import dev.macula.boot.starter.security.web.PasskeyArgumentResolverPostProcessor;
import dev.macula.boot.starter.web.advice.ControllerExceptionAdvice;
import dev.macula.boot.starter.web.advice.ControllerResponseAdvice;
import dev.macula.boot.starter.web.config.JacksonProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 通过真实 MVC 参数绑定、Web Advice 和标准方法鉴权验证参数级资源凭证。
 *
 * @author Rain
 * @since 6.1.0
 */
class PasskeyMvcTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Input.creations.set(0);
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(MvcConfiguration.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        PasskeyArgumentResolverTest.authenticate("alice");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    private String token() {
        return context.getBean(PasskeyService.class).issue("alice", "123");
    }

    @Test
    void listCredentialProtectsPathQueryAndBodyArguments() throws Exception {
        String response = mvc.perform(get("/items")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].resourceId").doesNotExist())
            .andExpect(jsonPath("$.data[0].passkey").isString()).andReturn().getResponse().getContentAsString();
        String credential = context.getBean(ObjectMapper.class).readTree(response).get("data").get(0)
            .get("passkey").asString();
        mvc.perform(get("/items/123").header("X-Resource-Passkey", credential))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.message").value("details"));
        mvc.perform(get("/query?id=123&passkey=" + credential)).andExpect(status().isOk());
        mvc.perform(post("/items/123/cancel").header("X-Resource-Passkey", credential)).andExpect(status().isOk());
        mvc.perform(post("/items/update?passkey=" + credential).contentType("application/json")
            .content("{\"orderId\":\"123\"}")).andExpect(status().isOk());
        assertThat(context.getBean(State.class).writes).isEqualTo(2);
        assertThat(Input.creations.get()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidMissingAndConflictingCredentialsBeforeControllerExecution() throws Exception {
        mvc.perform(post("/items/123/cancel")).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("A0312"));
        mvc.perform(post("/items/456/cancel").header("X-Resource-Passkey", token()))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(post("/items/123/cancel?passkey=conflict").header("X-Resource-Passkey", token()))
            .andExpect(status().isForbidden());
        mvc.perform(post("/items/update").contentType("application/json")
            .content("{\"orderId\":\"123\",\"passkey\":\"" + token() + "\"}")).andExpect(status().isForbidden());
        assertThat(context.getBean(State.class).writes).isZero();
    }

    @Test
    void preservesJsonValidationAndTypeConversionFailures() throws Exception {
        mvc.perform(post("/items/update").header("X-Resource-Passkey", token()).contentType("application/json")
            .content("{\"orderId\":\"\"}")).andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("A0400"))
            .andExpect(result -> assertThat(result.getResolvedException())
                .isInstanceOf(org.springframework.web.bind.MethodArgumentNotValidException.class));
        int conversionStatus = mvc.perform(get("/public/not-a-number")).andReturn().getResponse().getStatus();
        assertThat(conversionStatus).isGreaterThanOrEqualTo(400);
        mvc.perform(get("/query?id=not-a-number").header("X-Resource-Passkey", token()))
            .andExpect(status().is(conversionStatus));
        assertThat(context.getBean(State.class).writes).isZero();
        assertThat(context.getBean(State.class).reads).isZero();
    }

    @Test
    void supportsNestedPropertiesAndApplicationArgumentResolvers() throws Exception {
        mvc.perform(post("/nested").header("X-Resource-Passkey", token()).contentType("application/json")
            .content("{\"order\":{\"id\":\"123\"}}")).andExpect(status().isOk());
        mvc.perform(post("/nested").header("X-Resource-Passkey", token()).contentType("application/json")
            .content("{\"order\":null}")).andExpect(status().isForbidden());
        mvc.perform(get("/custom?id=123").header("X-Resource-Passkey", token())).andExpect(status().isOk());
        mvc.perform(get("/custom?id=456").header("X-Resource-Passkey", token())).andExpect(status().isForbidden());
        assertThat(context.getBean(State.class).writes).isEqualTo(1);
    }

    @Test
    void preAndPostAuthorizationStillApplyWithParameterCredentials() throws Exception {
        PermissionGate gate = context.getBean(PermissionGate.class);
        gate.allowed = false;
        mvc.perform(get("/checked/123?owner=alice").header("X-Resource-Passkey", token()))
            .andExpect(status().isInternalServerError());
        assertThat(context.getBean(State.class).reads).isZero();
        gate.allowed = true;
        mvc.perform(get("/checked/456?owner=alice").header("X-Resource-Passkey", token()))
            .andExpect(status().isForbidden());
        assertThat(context.getBean(State.class).reads).isZero();
        mvc.perform(get("/checked/123?owner=alice").header("X-Resource-Passkey", token())).andExpect(status().isOk());
        mvc.perform(get("/checked/123?owner=bob").header("X-Resource-Passkey", token()))
            .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.data").isEmpty());
        assertThat(context.getBean(State.class).reads).isEqualTo(2);
    }

    @Test
    void standardPostAuthorizeWorksWithoutPasskey() throws Exception {
        mvc.perform(get("/owned")).andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.data").isEmpty());
        assertThat(context.getBean(State.class).reads).isEqualTo(1);
    }

    @Test
    void unannotatedArgumentsKeepTheirBindingWithoutCredentials() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get("/public/123")).andExpect(status().isOk()).andExpect(jsonPath("$.data").value(123));
        mvc.perform(post("/public-body").contentType("application/json").content("{\"orderId\":\"plain\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").value("plain"));
    }

    @Test
    void disabledModeRejectsProtectedParametersAndOrdinaryErrorsKeepTheirResponse() throws Exception {
        mvc.perform(get("/ordinary-error")).andExpect(status().isInternalServerError());
        context.getBean(State.class).enabled = false;
        mvc.perform(post("/items/123/cancel").header("X-Resource-Passkey", token()))
            .andExpect(status().isForbidden());
        assertThat(context.getBean(State.class).writes).isZero();
    }

    /** Supplies real MVC resolvers, credentials, method security and production response advice. @since 6.1.0 */
    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableMethodSecurity(proxyTargetClass = true)
    static class MvcConfiguration implements WebMvcConfigurer {
        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        static PasskeyArgumentResolverPostProcessor passkeyProcessor(ObjectProvider<PasskeyService> services,
            ObjectProvider<State> state) {
            return new PasskeyArgumentResolverPostProcessor(services::getIfAvailable, () -> state.getObject().enabled);
        }
        @Bean
        State state() { return new State(); }
        @Bean
        PasskeyService passkeyService() {
            return new HmacPasskeyService(PasskeyServiceTest.settings(), PasskeyServiceTest.CLOCK);
        }
        @Bean
        ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean
        ControllerResponseAdvice responseAdvice(ObjectMapper mapper, ObjectProvider<PasskeyIssuer> issuers) {
            return new ControllerResponseAdvice(mapper, new JacksonProperties(), issuers, true);
        }
        @Bean
        ControllerExceptionAdvice exceptionAdvice() { return new ControllerExceptionAdvice(); }
        @Bean
        ItemController itemController(State state) { return new ItemController(state); }
        @Bean
        PermissionGate pms() { return new PermissionGate(); }
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return parameter.getParameterType() == CustomId.class;
                }
                @Override
                public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                    NativeWebRequest request, WebDataBinderFactory binderFactory) {
                    return new CustomId(request.getParameter("id"));
                }
            });
        }
    }

    /** Tracks controller execution independently of method-security proxies. @since 6.1.0 */
    static class State {
        private int writes;
        private int reads;
        private boolean enabled = true;
    }

    /** Uses only standard binding annotations plus the resource parameter marker. @since 6.1.0 */
    @RestController
    static class ItemController {
        private final State state;
        ItemController(State state) { this.state = state; }

        @GetMapping("/items")
        public List<Item> list() { return Collections.singletonList(new Item("123")); }

        @GetMapping("/items/{id}")
        public Detail detail(@PathVariable("id") @Passkey String id) {
            state.reads++;
            return new Detail(id, "details");
        }

        @GetMapping("/query")
        public long query(@RequestParam("id") @Passkey Long id) {
            state.reads++;
            return id;
        }

        @PostMapping("/items/{id}/cancel")
        @PreAuthorize("@pms.hasPermission('order:cancel')")
        public void cancel(@PathVariable("id") @Passkey String id) { state.writes++; }

        @PostMapping("/items/update")
        public void update(@RequestBody @Valid @Passkey("orderId") Input input) { state.writes++; }

        @PostMapping("/nested")
        public void nested(@RequestBody @Passkey("order.id") NestedInput input) { state.writes++; }

        @GetMapping("/custom")
        public String custom(@Passkey("id") CustomId id) { return id.getId(); }

        @GetMapping("/checked/{id}")
        @PreAuthorize("@pms.hasPermission('order:read')")
        @PostAuthorize("returnObject.owner == authentication.name")
        public Owned checked(@PathVariable("id") @Passkey String id, @RequestParam("owner") String owner) {
            state.reads++;
            return new Owned(owner, "details");
        }

        @GetMapping("/owned")
        @PreAuthorize("isAuthenticated()")
        @PostAuthorize("returnObject.owner == authentication.name")
        public Owned owned() {
            state.reads++;
            return new Owned("bob", "secret");
        }

        @GetMapping("/public/{id}")
        public Long unprotected(@PathVariable("id") Long id) { return id; }

        @PostMapping("/public-body")
        public String unprotectedBody(@RequestBody Input input) { return input.getOrderId(); }

        @GetMapping("/ordinary-error")
        public void error() { throw new IllegalStateException("ordinary"); }
    }

    /** Authorized list projection with a credential. @since 6.1.0 */
    public static class Item implements PasskeyResource {
        private final String id;
        private String passkey;
        public Item(String id) { this.id = id; }
        public String getId() { return id; }
        @Override
        public String getResourceId() { return id; }
        @Override
        public String getPasskey() { return passkey; }
        @Override
        public void setPasskey(String passkey) { this.passkey = passkey; }
    }

    /** Detail DTO intentionally different from the list projection. @since 6.1.0 */
    public static class Detail {
        private final String id;
        private final String message;
        public Detail(String id, String message) {
            this.id = id;
            this.message = message;
        }
        public String getId() { return id; }
        public String getMessage() { return message; }
    }

    /** Mutable JSON input with a JavaBean getter and a construction counter. @since 6.1.0 */
    public static class Input {
        private static final AtomicInteger creations = new AtomicInteger();
        @NotBlank
        private String orderId;
        private String passkey;
        public Input() { creations.incrementAndGet(); }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getPasskey() { return passkey; }
        public void setPasskey(String passkey) { this.passkey = passkey; }
    }

    /** Nested JSON input. @since 6.1.0 */
    public static class NestedInput {
        private ResourceKey order;
        public ResourceKey getOrder() { return order; }
        public void setOrder(ResourceKey order) { this.order = order; }
    }

    /** Nested resource ID. @since 6.1.0 */
    public static class ResourceKey {
        private String id;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    /** Argument created by an application-supplied resolver. @since 6.1.0 */
    public static class CustomId {
        private final String id;
        public CustomId(String id) { this.id = id; }
        public String getId() { return id; }
    }

    /** Returned data checked by standard PostAuthorize. @since 6.1.0 */
    public static class Owned {
        private final String owner;
        private final String secret;
        public Owned(String owner, String secret) {
            this.owner = owner;
            this.secret = secret;
        }
        public String getOwner() { return owner; }
        public String getSecret() { return secret; }
    }

    /** Minimal operation-permission fixture. @since 6.1.0 */
    public static class PermissionGate {
        private boolean allowed = true;
        public boolean hasPermission(String permission) { return allowed; }
    }
}
