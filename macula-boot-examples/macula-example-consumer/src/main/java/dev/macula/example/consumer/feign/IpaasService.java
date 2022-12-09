package dev.macula.example.consumer.feign;

import dev.macula.example.consumer.feign.configuration.IpaasConfiguration;
import dev.macula.example.consumer.vo.CompanyDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ipaas-service", url = "https://ipaas-dev.infinitus.com.cn", configuration = IpaasConfiguration.class)
public interface IpaasService {

    @PostMapping("/openapi/product-api/companies")
    String companies(@RequestBody CompanyDto companyDto);

    @GetMapping("/openapi/product-api/skus")
    String listSkus(@RequestParam("sku") String sku);

}
