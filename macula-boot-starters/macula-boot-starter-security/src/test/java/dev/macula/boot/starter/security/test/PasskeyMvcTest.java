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

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.macula.boot.security.PasskeyIssuer;
import dev.macula.boot.security.PasskeyResource;
import dev.macula.boot.starter.security.annotation.CheckOwner;
import dev.macula.boot.starter.security.authorization.CheckOwnerAdvisor;
import dev.macula.boot.starter.security.passkey.HmacPasskeyService;
import dev.macula.boot.starter.security.passkey.PasskeyService;
import dev.macula.boot.starter.web.advice.ControllerExceptionAdvice;
import dev.macula.boot.starter.web.advice.ControllerResponseAdvice;
import dev.macula.boot.starter.web.config.JacksonProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises real Web advice and Spring Security 5.7 interception before controller side effects.
 *
 * @author Rain
 * @since 5.0.21
 */
class PasskeyMvcTest {

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(MvcConfiguration.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        CheckOwnerTest.authenticate("alice");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void listCredentialWorksForDifferentDetailDtoAndWriteRequests() throws Exception {
        String response = mvc.perform(get("/items")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].resourceId").doesNotExist())
            .andExpect(jsonPath("$.data[0].passkey").isString()).andReturn().getResponse().getContentAsString();
        String token = context.getBean(ObjectMapper.class).readTree(response).get("data").get(0)
            .get("passkey").asText();
        mvc.perform(get("/items/123").header("X-Resource-Passkey", token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.message").value("details"));
        mvc.perform(post("/items/123/cancel").header("X-Resource-Passkey", token))
            .andExpect(status().isOk());
        mvc.perform(post("/items/update?passkey=" + token).contentType("application/json")
            .content("{\"orderId\":\"123\"}")).andExpect(status().isOk());
        assertThat(context.getBean(State.class).writes).isEqualTo(2);
    }

    @Test
    void invalidMissingAndConflictingCredentialsReturn403WithoutExecution() throws Exception {
        String token = context.getBean(PasskeyService.class).issue("alice", "123");
        mvc.perform(post("/items/123/cancel")).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("A0312"));
        mvc.perform(post("/items/456/cancel").header("X-Resource-Passkey", token))
            .andExpect(status().isForbidden());
        mvc.perform(post("/items/123/cancel?passkey=conflict").header("X-Resource-Passkey", token))
            .andExpect(status().isForbidden());
        assertThat(context.getBean(State.class).writes).isZero();
    }

    @Test
    void ownerRejectionRunsReadButDoesNotReturnDetails() throws Exception {
        mvc.perform(get("/owned")).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("A0312"))
            .andExpect(jsonPath("$.data.secret").doesNotExist());
        assertThat(context.getBean(State.class).reads).isEqualTo(1);
    }

    @Test
    void ordinaryExceptionsKeepExistingResponseAndDisabledModeDenies() throws Exception {
        mvc.perform(get("/ordinary-error")).andExpect(status().isInternalServerError());
        context.getBean(State.class).enabled = false;
        String token = context.getBean(PasskeyService.class).issue("alice", "123");
        mvc.perform(post("/items/123/cancel").header("X-Resource-Passkey", token))
            .andExpect(status().isForbidden());
        assertThat(context.getBean(State.class).writes).isZero();
    }

    /** Minimal MVC infrastructure with production service, advisor and advice. @since 5.0.21 */
    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true,
        order = Ordered.LOWEST_PRECEDENCE - 1)
    static class MvcConfiguration {
        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        static CheckOwnerAdvisor advisor(ObjectProvider<PasskeyService> services, ObjectProvider<State> state) {
            return new CheckOwnerAdvisor(services::getObject, () -> state.getObject().enabled);
        }

        @Bean
        State state() {
            return new State();
        }

        @Bean
        PasskeyService passkeyService() {
            return new HmacPasskeyService(PasskeyServiceTest.settings(), PasskeyServiceTest.CLOCK);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ControllerResponseAdvice responseAdvice(ObjectMapper mapper, ObjectProvider<PasskeyIssuer> issuers) {
            return new ControllerResponseAdvice(mapper, new JacksonProperties(), issuers, true);
        }

        @Bean
        ControllerExceptionAdvice exceptionAdvice() {
            return new ControllerExceptionAdvice();
        }

        @Bean
        ItemController itemController(State state) {
            return new ItemController(state);
        }

        @Bean
        PermissionGate pms() {
            return new PermissionGate();
        }
    }

    /** Counts side effects independently of controller proxies. @since 5.0.21 */
    static class State {
        private int writes;
        private int reads;
        private boolean enabled = true;
    }

    /** Supplies operations with standard MVC parameter binding. @since 5.0.21 */
    @RestController
    static class ItemController {
        private final State state;

        ItemController(State state) {
            this.state = state;
        }

        @GetMapping("/items")
        public List<Item> list() {
            return Collections.singletonList(new Item("123"));
        }

        @GetMapping("/items/{id}")
        @CheckOwner("#p0")
        public Detail detail(@PathVariable("id") String id) {
            return new Detail(id, "details");
        }

        @PostMapping("/items/{id}/cancel")
        @CheckOwner("#p0")
        @PreAuthorize("@pms.hasPermission('order:cancel')")
        public void cancel(@PathVariable("id") String id) {
            state.writes++;
        }

        @PostMapping("/items/update")
        @CheckOwner("#p0.orderId")
        public void update(@RequestBody Input input) {
            state.writes++;
        }

        @GetMapping("/owned")
        @CheckOwner("returnObject.owner")
        public Owned owned() {
            state.reads++;
            return new Owned("bob", "secret");
        }

        @GetMapping("/ordinary-error")
        public void error() {
            throw new IllegalStateException("ordinary");
        }
    }

    /** Authorized list projection carrying a signed access credential. @since 5.0.21 */
    public static class Item implements PasskeyResource {
        private final String id;
        private String passkey;

        public Item(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public String getResourceId() {
            return id;
        }

        @Override
        public String getPasskey() {
            return passkey;
        }

        @Override
        public void setPasskey(String passkey) {
            this.passkey = passkey;
        }
    }

    /** Detail representation intentionally differs from the list projection. @since 5.0.21 */
    public static class Detail {
        private final String id;
        private final String message;

        public Detail(String id, String message) {
            this.id = id;
            this.message = message;
        }

        public String getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }
    }

    /** Standard JSON write request. @since 5.0.21 */
    public static class Input {
        private String orderId;

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }
    }

    /** Direct owner-based query result without a passkey contract. @since 5.0.21 */
    public static class Owned {
        private final String owner;
        private final String secret;

        public Owned(String owner, String secret) {
            this.owner = owner;
            this.secret = secret;
        }

        public String getOwner() {
            return owner;
        }

        public String getSecret() {
            return secret;
        }
    }

    /** Application permission service stub. @since 5.0.21 */
    public static class PermissionGate {
        public boolean hasPermission(String permission) {
            return true;
        }
    }
}
