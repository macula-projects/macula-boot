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

package org.apache.rocketmq.redis.replicator;

import com.moilioncircle.redis.replicator.AbstractReplicator;
import com.moilioncircle.redis.replicator.CloseListener;
import com.moilioncircle.redis.replicator.Configuration;
import com.moilioncircle.redis.replicator.ExceptionListener;
import com.moilioncircle.redis.replicator.RedisReplicator;
import com.moilioncircle.redis.replicator.RedisSocketReplicator;
import com.moilioncircle.redis.replicator.RedisURI;
import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.cmd.Command;
import com.moilioncircle.redis.replicator.cmd.CommandName;
import com.moilioncircle.redis.replicator.cmd.CommandParser;
import com.moilioncircle.redis.replicator.event.EventListener;
import com.moilioncircle.redis.replicator.rdb.RdbVisitor;
import com.moilioncircle.redis.replicator.rdb.datatype.Module;
import com.moilioncircle.redis.replicator.rdb.module.ModuleParser;
import dev.macula.boot.starter.leaderelection.ElectionListener;
import dev.macula.boot.starter.leaderelection.LeaderElection;
import lombok.SneakyThrows;
import org.apache.rocketmq.redis.replicator.conf.Configure;
import org.apache.rocketmq.redis.replicator.mq.RocketMQRedisProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;

import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.DEPLOY_MODEL;
import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.DEPLOY_MODEL_SINGLE;
import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.REDIS_URI;

public class RocketMQRedisReplicator extends AbstractReplicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocketMQRedisReplicator.class);

    protected final RedisURI uri;
    protected final Configure configure;
    protected final Replicator replicator;
    protected final LeaderElection leaderElection;

    public RocketMQRedisReplicator(Configure configure, LeaderElection leaderElection) throws IOException, URISyntaxException {
        Objects.requireNonNull(configure);
        String uri = configure.getString(REDIS_URI);
        this.configure = configure;
        this.uri = new RedisURI(uri);
        this.replicator = new RedisReplicator(this.uri);
        this.leaderElection = leaderElection;
    }

    public static void main(String[] args) throws Exception {
        Configure configure = new Configure();
        // TODO 需要从Spring获取
        LeaderElection leaderElection = new LeaderElection();
        Replicator replicator = new RocketMQRedisReplicator(configure, leaderElection);
        final RocketMQRedisProducer producer = new RocketMQRedisProducer(configure);
        producer.open();
        replicator.addEventListener((r, e) -> {
            try {
                if (!producer.send(e)) {
                    LOGGER.error("Failed to send event[{}]", e);
                }
            } catch (Exception ex) {
                LOGGER.error("Failed to send event[{}]", e, ex);
            }
        });

        replicator.addCloseListener(r -> producer.close());
        replicator.open();
    }

    @Override
    public void builtInCommandParserRegister() {
        replicator.builtInCommandParserRegister();
    }

    @Override
    public CommandParser<? extends Command> getCommandParser(CommandName command) {
        return replicator.getCommandParser(command);
    }

    @Override
    public <T extends Command> void addCommandParser(CommandName command, CommandParser<T> parser) {
        replicator.addCommandParser(command, parser);
    }

    @Override
    public CommandParser<? extends Command> removeCommandParser(CommandName command) {
        return replicator.removeCommandParser(command);
    }

    @Override
    public ModuleParser<? extends Module> getModuleParser(String moduleName, int moduleVersion) {
        return replicator.getModuleParser(moduleName, moduleVersion);
    }

    @Override
    public <T extends Module> void addModuleParser(String moduleName, int moduleVersion, ModuleParser<T> parser) {
        replicator.addModuleParser(moduleName, moduleVersion, parser);
    }

    @Override
    public ModuleParser<? extends Module> removeModuleParser(String moduleName, int moduleVersion) {
        return replicator.removeModuleParser(moduleName, moduleVersion);
    }

    @Override
    public RdbVisitor getRdbVisitor() {
        return replicator.getRdbVisitor();
    }

    @Override
    public void setRdbVisitor(RdbVisitor rdbVisitor) {
        replicator.setRdbVisitor(rdbVisitor);
    }

    @Override
    public boolean addEventListener(EventListener listener) {
        return replicator.addEventListener(listener);
    }

    @Override
    public boolean removeEventListener(EventListener listener) {
        return replicator.removeEventListener(listener);
    }

    @Override
    public boolean addCloseListener(CloseListener listener) {
        return replicator.addCloseListener(listener);
    }

    @Override
    public boolean removeCloseListener(CloseListener listener) {
        return replicator.removeCloseListener(listener);
    }

    @Override
    public boolean addExceptionListener(ExceptionListener listener) {
        return replicator.addExceptionListener(listener);
    }

    @Override
    public boolean removeExceptionListener(ExceptionListener listener) {
        return replicator.removeExceptionListener(listener);
    }

    @Override
    public boolean verbose() {
        return replicator.verbose();
    }

    @Override
    public Configuration getConfiguration() {
        return replicator.getConfiguration();
    }

    @Override
    public void open() throws IOException {
        if (this.replicator instanceof RedisSocketReplicator) {
            boolean single = configure.getString(DEPLOY_MODEL).equals(DEPLOY_MODEL_SINGLE);
            if (single) {
                this.replicator.open();
            } else {
                leaderElection.addElectionListener(new ElectionListener() {
                    @SneakyThrows
                    @Override
                    public void onElected() {
                        replicator.open();
                    }
                });
            }
        } else {
            this.replicator.open();
        }
    }

    @Override
    public void close() throws IOException {
        replicator.close();
    }
}
