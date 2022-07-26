package dev.macula.example.consumer.feign;

import dev.macula.example.consumer.feign.configuration.GapiConfiguration;
import dev.macula.example.consumer.vo.PoBaseDto;
import dev.macula.example.consumer.vo.PoBaseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "gapi-service", url = "https://gapi-dev.infinitus.com.cn", configuration = GapiConfiguration.class)
public interface GapiService {

    @PostMapping("/ecp/po/trade/updateEvaluationStatus")
    PoBaseResult updateEvaluationStatus(@RequestBody PoBaseDto poBaseDto);

}
