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

package dev.macula.boot.starter.binlog;

import com.github.shyiko.mysql.binlog.event.EventType;

public class BinlogUtils {

    public static boolean isUpdate(EventType eventType) {
        return eventType == EventType.PRE_GA_UPDATE_ROWS || eventType == EventType.UPDATE_ROWS || eventType == EventType.EXT_UPDATE_ROWS;
    }

    public static boolean isInsert(EventType eventType) {
        return eventType == EventType.PRE_GA_WRITE_ROWS || eventType == EventType.WRITE_ROWS || eventType == EventType.EXT_WRITE_ROWS;
    }

    public static boolean isDelete(EventType eventType) {
        return eventType == EventType.PRE_GA_DELETE_ROWS || eventType == EventType.DELETE_ROWS || eventType == EventType.EXT_DELETE_ROWS;
    }
}
