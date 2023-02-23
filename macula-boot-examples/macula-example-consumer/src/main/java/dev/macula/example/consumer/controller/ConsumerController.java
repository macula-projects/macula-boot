/*
 * Copyright (c) 2022 Macula
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

package dev.macula.example.consumer.controller;

import dev.macula.example.consumer.feign.GapiService;
import dev.macula.example.consumer.feign.IpaasService;
import dev.macula.example.consumer.feign.Provider1Service;
import dev.macula.example.consumer.vo.CompanyDto;
import dev.macula.example.consumer.vo.PoBaseDto;
import dev.macula.example.consumer.vo.PoBaseResult;
import dev.macula.example.consumer.vo.UserResult;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * {@code ConsumerController} 消费者演示
 *
 * @author rain
 * @since 2022/7/22 22:48
 */

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/consumer")
public class ConsumerController {

    @Autowired
    private Provider1Service provider1Service;

    @Autowired
    private GapiService gapiService;

    @Autowired
    private IpaasService ipaasService;

    @GetMapping("/echo")
    public String echo() {
        return provider1Service.echo("consumer");
    }

    @PostMapping("/user")
    public UserResult getUser() {
        return provider1Service.getUser();
    }

    @PostMapping("/updateEvaluationStatus")
    public PoBaseResult updateEvaluationStatus(@RequestBody PoBaseDto poBaseDto) {
        return gapiService.updateEvaluationStatus(poBaseDto);
    }

    @GetMapping("/getOrderDetail")
    public PoBaseResult getOrderDetail(@RequestParam("poNo") String poNo) {
        return gapiService.getOrderDetail2Result(poNo);
    }

    @PostMapping("/companies")
    public String companies(@RequestBody CompanyDto companyDto) {
        return ipaasService.companies(companyDto);
    }

    @GetMapping("/listSku")
    public String listSku(@RequestParam("sku") String sku) {
        return ipaasService.listSkus(sku);
    }

}
