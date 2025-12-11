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

package dev.macula.boot.starter.tinyid.base.factory;

import dev.macula.boot.starter.tinyid.base.generator.IdGenerator;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 抽象 ID生成器工厂
 * 
 * @author du_imba
 */
public abstract class AbstractIdGeneratorFactory implements IdGeneratorFactory {

    private static ConcurrentHashMap<String, IdGenerator> generators = new ConcurrentHashMap<>();

    @Override
    public IdGenerator getIdGenerator(String bizType) {
        if (generators.containsKey(bizType)) {
            return generators.get(bizType);
        }
        synchronized (this) {
            if (generators.containsKey(bizType)) {
                return generators.get(bizType);
            }
            IdGenerator idGenerator = createIdGenerator(bizType);
            generators.put(bizType, idGenerator);
            return idGenerator;
        }
    }

    /**
     * 根据 bizType创建id生成器
     *
     * @param bizType 业务类型
     * @return IdGenerator
     */
    protected abstract IdGenerator createIdGenerator(String bizType);
}
