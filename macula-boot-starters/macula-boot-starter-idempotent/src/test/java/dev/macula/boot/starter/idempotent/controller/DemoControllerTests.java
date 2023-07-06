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

package dev.macula.boot.starter.idempotent.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author lengleng
 * @since 2021/2/1
 */
@SpringBootApplication
@SpringBootTest
class DemoControllerTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * 单线程测试
     *
     * @throws Exception
     */
    @Test
    void getOneThreadResutTest() throws Exception {
        mockMvc.perform(get("/get?key=1")).andExpect(status().isOk()).andReturn();
    }

    /**
     * 多线程测试
     */
    @RepeatedTest(10)
    @Execution(CONCURRENT)
    void getMultiThreadResutTest() {
        try {
            mockMvc.perform(get("/get?key=10")).andExpect(status().isOk());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RepeatedTest(10)
    @Execution(CONCURRENT)
    void getMultiThreadNoKeyResutTest() {
        try {
            mockMvc.perform(get("/noKey")).andExpect(status().isOk());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
