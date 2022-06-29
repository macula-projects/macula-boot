package org.macula.boot.starter.tinyid.factory.impl;

import org.macula.boot.starter.tinyid.base.factory.AbstractIdGeneratorFactory;
import org.macula.boot.starter.tinyid.base.generator.IdGenerator;
import org.macula.boot.starter.tinyid.base.generator.impl.CachedIdGenerator;
import org.macula.boot.starter.tinyid.base.service.SegmentIdService;

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
