/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.mysql;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;


public class Config {

    public String mysqlAddr;
    public Integer mysqlPort;
    public String mysqlUsername;
    public String mysqlPassword;

    public String mqNamesrvAddr;
    public String mqNamespace;
    public String mqProducerGroup;
    public String mqConsumerGroup;
    public String mqSecretKey;
    public String mqAccessKey;

    public String mqTopic;

    public String startType = "DEFAULT";
    public String binlogFilename;
    public Long nextPosition;
    public Integer maxTransactionRows = 100;
    public long serverId = 1001;

    public void setMysqlAddr(String mysqlAddr) {
        this.mysqlAddr = mysqlAddr;
    }

    public void setMysqlPort(Integer mysqlPort) {
        this.mysqlPort = mysqlPort;
    }

    public void setMysqlUsername(String mysqlUsername) {
        this.mysqlUsername = mysqlUsername;
    }

    public void setMysqlPassword(String mysqlPassword) {
        this.mysqlPassword = mysqlPassword;
    }

    public void setBinlogFilename(String binlogFilename) {
        this.binlogFilename = binlogFilename;
    }

    public void setNextPosition(Long nextPosition) {
        this.nextPosition = nextPosition;
    }

    public void setMaxTransactionRows(Integer maxTransactionRows) {
        this.maxTransactionRows = maxTransactionRows;
    }

    public void setMqNamesrvAddr(String mqNamesrvAddr) {
        this.mqNamesrvAddr = mqNamesrvAddr;
    }

    public void setMqAccessKey(String mqAccessKey) {
        this.mqAccessKey = mqAccessKey;
    }

    public void setMqSecretKey(String mqSecretKey) {
        this.mqSecretKey = mqSecretKey;
    }

    public void setMqTopic(String mqTopic) {
        this.mqTopic = mqTopic;
    }

    public void setStartType(String startType) {
        this.startType = startType;
    }
}