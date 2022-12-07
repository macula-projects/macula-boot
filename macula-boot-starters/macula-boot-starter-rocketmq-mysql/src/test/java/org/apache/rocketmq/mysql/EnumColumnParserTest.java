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

package org.apache.rocketmq.mysql;

import org.apache.rocketmq.mysql.schema.column.EnumColumnParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class EnumColumnParserTest {

    @Test
    public void testEnum() {
        String colType = "enum('a','b','c','d')";

        EnumColumnParser parser = new EnumColumnParser(colType);
        String v = (String) parser.getValue(3);
        assertEquals(v, "c");
    }

}
