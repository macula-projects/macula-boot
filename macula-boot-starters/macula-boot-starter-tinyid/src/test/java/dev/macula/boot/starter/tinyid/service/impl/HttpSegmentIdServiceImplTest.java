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
package dev.macula.boot.starter.tinyid.service.impl;

import dev.macula.boot.starter.tinyid.remote.TinyIdFeignClient;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证号段响应解析及远程失败传播。
 * @author Rain
 * @since 6.1.0
 */
class HttpSegmentIdServiceImplTest {

    private final TinyIdFeignClient client = mock(TinyIdFeignClient.class);
    private final HttpSegmentIdServiceImpl service = new HttpSegmentIdServiceImpl(client);

    @Test
    void parsesSegmentWithoutChangingShardParameters() {
        when(client.nextSegmentId("order")).thenReturn("200,250,300,2,1");
        var segment = service.getNextSegmentId("order");
        assertThat(segment.getCurrentId().get()).isEqualTo(200);
        assertThat(segment.getLoadingId()).isEqualTo(250);
        assertThat(segment.getMaxId()).isEqualTo(300);
        assertThat(segment.getDelta()).isEqualTo(2);
        assertThat(segment.getRemainder()).isEqualTo(1);
    }

    @Test
    void preservesEmptyResponseAndRejectsMalformedResponse() {
        when(client.nextSegmentId("order")).thenReturn(null, "", " ", "1,2", "a,2,3,1,0");
        assertThat(service.getNextSegmentId("order")).isNull();
        assertThat(service.getNextSegmentId("order")).isNull();
        assertThat(service.getNextSegmentId("order")).isNull();
        assertThatIllegalStateException().isThrownBy(() -> service.getNextSegmentId("order"));
        assertThatThrownBy(() -> service.getNextSegmentId("order")).isInstanceOf(NumberFormatException.class);
    }

    @Test
    void propagatesFailureWithoutRetryAndRejectsBlankBusiness() {
        RuntimeException failure = new IllegalStateException("unavailable");
        when(client.nextSegmentId("order")).thenThrow(failure);
        assertThatThrownBy(() -> service.getNextSegmentId("order")).isSameAs(failure);
        verify(client).nextSegmentId("order");
        assertThatIllegalArgumentException().isThrownBy(() -> service.getNextSegmentId(" "));
        verifyNoMoreInteractions(client);
    }
}
