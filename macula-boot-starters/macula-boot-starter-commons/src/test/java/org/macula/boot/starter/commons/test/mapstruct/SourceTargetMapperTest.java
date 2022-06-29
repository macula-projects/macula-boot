package org.macula.boot.starter.commons.test.mapstruct;

import org.junit.jupiter.api.Test;

import java.util.Date;

public class SourceTargetMapperTest {

    @Test
    public void testMapper() {
        SourceDto s = new SourceDto("rain", "123456", new Date(), 90);
        TargetDto t = SourceTargetMapper.MAPPER.toTarget(s);
        System.out.println(t);
    }
}
