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

package dev.macula.boot.starter.web.advice;

import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.result.PageVO;
import dev.macula.boot.result.Result;
import dev.macula.boot.security.PasskeyIssuer;
import dev.macula.boot.security.PasskeyResource;
import dev.macula.boot.starter.web.annotation.NotControllerResponseAdvice;
import dev.macula.boot.starter.web.config.JacksonProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that credential issuance is independent of response wrapping and its opt-outs.
 *
 * @author Macula
 * @since 6.1.0
 */
class ControllerResponseAdviceTest {

    private MockMvc mvc(boolean issuerPresent) {
        return mvc(issuerPresent, true);
    }

    private MockMvc mvc(boolean issuerPresent, boolean enabled) {
        var mapper = JsonMapper.builder().build();
        ControllerResponseAdvice advice;
        if (issuerPresent) {
            var factory = new StaticListableBeanFactory();
            factory.addBean("issuer", (PasskeyIssuer) resource -> resource.setPasskey("signed-" + resource.getResourceId()));
            advice = new ControllerResponseAdvice(mapper, new JacksonProperties(), factory.getBeanProvider(PasskeyIssuer.class), enabled);
        } else {
            advice = new ControllerResponseAdvice(mapper, new JacksonProperties(), null, enabled);
        }
        return MockMvcBuilders.standaloneSetup(new ListController())
            .setControllerAdvice(advice, new ControllerExceptionAdvice()).build();
    }

    @Test
    void signsPlainResultAndPageWithoutDoubleWrapping() throws Exception {
        MockMvc mvc = mvc(true);
        mvc.perform(get("/list")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].passkey").value("signed-123"))
            .andExpect(jsonPath("$.data[0].resourceId").doesNotExist());
        mvc.perform(get("/result")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].passkey").value("signed-123"))
            .andExpect(jsonPath("$.data[1]").value("ordinary"));
        mvc.perform(get("/page")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.records[0].passkey").value("signed-123"));
    }

    @Test
    void responseEntityAndOptOutStillIssueButDoNotWrap() throws Exception {
        MockMvc mvc = mvc(true);
        mvc.perform(get("/entity")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].passkey").value("signed-123"));
        mvc.perform(get("/raw")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].passkey").value("signed-123"));
        mvc.perform(get("/list").header(GlobalConstants.FEIGN_REQ_ID, "test"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].passkey").value("signed-123"));
    }

    @Test
    void missingIssuerRejectsListAndLeavesOrdinaryAndStandaloneResponsesAlone() throws Exception {
        MockMvc mvc = mvc(false);
        mvc.perform(get("/list")).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("A0312"));
        mvc.perform(get("/ordinary")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0]").value("ordinary"));
        mvc.perform(get("/single")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.passkey").value("old"));
    }

    @Test
    void disabledModeReturnsResourceListsWithoutIssuingEvenWithCustomIssuer() throws Exception {
        for (boolean issuerPresent : List.of(false, true)) {
            MockMvc mvc = mvc(issuerPresent, false);
            mvc.perform(get("/list")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].passkey").value("old"));
            mvc.perform(get("/result")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].passkey").value("old"));
            mvc.perform(get("/raw")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passkey").value("old"));
        }
    }

    /**
     * Controller returning the supported containers and legacy wrapping opt-outs.
     * @since 6.1.0
     */
    @RestController
    static class ListController {
        @GetMapping("/list")
        public List<Item> list() { return List.of(new Item()); }
        @GetMapping("/result")
        public Result<List<Object>> result() { return Result.success(List.of(new Item(), "ordinary")); }
        @GetMapping("/entity")
        public ResponseEntity<List<Item>> entity() { return ResponseEntity.ok(list()); }
        @GetMapping("/raw")
        @NotControllerResponseAdvice
        public List<Item> raw() { return list(); }
        @GetMapping("/single")
        public Item single() { return new Item(); }
        @GetMapping("/ordinary")
        public List<String> ordinary() { return List.of("ordinary"); }
        @GetMapping("/page")
        public PageVO<Item> page() {
            @SuppressWarnings("unchecked")
            PageVO<Item> page = JsonMapper.builder().build().convertValue(Map.of("records", List.of()), PageVO.class);
            page.setRecords(list());
            return page;
        }
    }

    /**
     * Resource record whose stale credential must be replaced per response.
     * @since 6.1.0
     */
    public static class Item implements PasskeyResource {
        private String passkey = "old";
        public String getId() { return "123"; }
        @Override
        public String getResourceId() { return "123"; }
        @Override
        public String getPasskey() { return passkey; }
        @Override
        public void setPasskey(String passkey) { this.passkey = passkey; }
    }
}
