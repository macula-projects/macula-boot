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

package dev.macula.boot.starter.tinyid.factory.impl;

import dev.macula.boot.starter.tinyid.base.factory.AbstractIdGeneratorFactory;
import dev.macula.boot.starter.tinyid.base.generator.IdGenerator;
import dev.macula.boot.starter.tinyid.base.generator.impl.CachedIdGenerator;
import dev.macula.boot.starter.tinyid.base.service.SegmentIdService;

import java.util.logging.Logger;

/**
 * @author du_imba
 */
public class CachedIdGeneratorFactory extends AbstractIdGeneratorFactory {

    private static final Logger logger = Logger.getLogger(CachedIdGeneratorFactory.class.getName());

    private SegmentIdService segmentIdService;

    public CachedIdGeneratorFactory(SegmentIdService segmentIdService) {
        this.segmentIdService = segmentIdService;
    }

    @Override
    protected IdGenerator createIdGenerator(String bizType) {
        return new CachedIdGenerator(bizType, segmentIdService);
    }

}
