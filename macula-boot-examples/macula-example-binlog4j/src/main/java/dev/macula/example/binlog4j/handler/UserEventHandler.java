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

package dev.macula.example.binlog4j.handler;

import com.alibaba.fastjson2.JSON;
import dev.macula.boot.starter.binlog4j.IBinlogEventHandler;
import dev.macula.boot.starter.binlog4j.config.annotation.BinlogSubscriber;
import dev.macula.example.binlog4j.entity.User;

/**
 * {@code UserEventHandler} 用户表订阅Handler
 *
 * @author rain
 * @since 2023/9/5 11:27
 */
@BinlogSubscriber(clientName = "master", database = "macula-system", table = "sys_user")
public class UserEventHandler implements IBinlogEventHandler<User> {

    @Override
    public void onInsert(User target) {
        System.out.println("插入数据：" + JSON.toJSONString(target));
    }

    @Override
    public void onUpdate(User source, User target) {
        System.out.println("修改数据:" + JSON.toJSONString(target));
    }

    @Override
    public void onDelete(User target) {
        System.out.println("删除数据");
    }
}
