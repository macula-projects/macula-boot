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

package dev.macula.boot.starter.oss.support;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 带进度通知的 InputStream 包装类
 */
public class ProgressInputStream extends FilterInputStream {

    private final long allSize;
    private final ProgressListener listener;
    private boolean readFlag;
    private long progressSize;

    public ProgressInputStream(InputStream in, ProgressListener listener, long allSize) {
        super(in);
        this.listener = listener;
        this.allSize = allSize;
    }

    @Override
    public long skip(long n) throws IOException {
        long skip = super.skip(n);
        progress(skip);
        return skip;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        progress(b == -1 ? -1 : 1);
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!this.readFlag) {
            this.readFlag = true;
            this.listener.start();
        }
        int bytes = super.read(b, off, len);
        progress(bytes);
        return bytes;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    protected void progress(long size) {
        if (size > 0) {
            this.listener.progress(progressSize += size, allSize);
        } else if (size < 0) {
            this.listener.finish();
        }
    }

}
