package org.macula.boot.starter.tinyid.base.generator.impl;

import org.macula.boot.starter.tinyid.base.entity.Result;
import org.macula.boot.starter.tinyid.base.entity.ResultCode;
import org.macula.boot.starter.tinyid.base.entity.SegmentId;
import org.macula.boot.starter.tinyid.base.exception.TinyIdSysException;
import org.macula.boot.starter.tinyid.base.generator.IdGenerator;
import org.macula.boot.starter.tinyid.base.service.SegmentIdService;
import org.macula.boot.starter.tinyid.base.util.NamedThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author du_imba
 */
public class CachedIdGenerator implements IdGenerator {
    protected String bizType;
    protected SegmentIdService segmentIdService;
    protected volatile SegmentId current;
    protected volatile SegmentId next;
    private volatile boolean isLoadingNext;
    private Object lock = new Object();
    private ExecutorService executorService = Executors.newSingleThreadExecutor(new NamedThreadFactory("tinyid-generator"));

    public CachedIdGenerator(String bizType, SegmentIdService segmentIdService) {
        this.bizType = bizType;
        this.segmentIdService = segmentIdService;
        loadCurrent();
    }

    public synchronized void loadCurrent() {
        if (current == null || !current.useful()) {
            if (next == null) {
                SegmentId segmentId = querySegmentId();
                this.current = segmentId;
            } else {
                current = next;
                next = null;
            }
        }
    }

    private SegmentId querySegmentId() {
        String message = null;
        try {
            SegmentId segmentId = segmentIdService.getNextSegmentId(bizType);
            if (segmentId != null) {
                return segmentId;
            }
        } catch (Exception e) {
            message = e.getMessage();
        }
        throw new TinyIdSysException("error query segmentId: " + message);
    }

    public void loadNext() {
        if (next == null && !isLoadingNext) {
            synchronized (lock) {
                if (next == null && !isLoadingNext) {
                    isLoadingNext = true;
                    executorService.submit(() -> {
                        try {
                            // 无论获取下个segmentId成功与否，都要将isLoadingNext赋值为false
                            next = querySegmentId();
                        } finally {
                            isLoadingNext = false;
                        }
                    });
                }
            }
        }
    }

    @Override
    public Long nextId() {
        // 透过死循环，一定要获取到segmentId，加入后台有多个数据库或者服务器，挂掉一个不影响使用，只是会慢点
        while (true) {
            if (current == null) {
                loadCurrent();
                continue;
            }
            Result result = current.nextId();
            if (result.getCode() == ResultCode.OVER) {
                loadCurrent();
            } else {
                if (result.getCode() == ResultCode.LOADING) {
                    loadNext();
                }
                return result.getId();
            }
        }
    }

    @Override
    public List<Long> nextId(Integer batchSize) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            Long id = nextId();
            ids.add(id);
        }
        return ids;
    }

}
