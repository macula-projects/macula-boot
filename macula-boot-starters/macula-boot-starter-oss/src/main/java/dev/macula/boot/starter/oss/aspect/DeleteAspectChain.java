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

package dev.macula.boot.starter.oss.aspect;

import dev.macula.boot.starter.oss.platform.FileStorage;
import dev.macula.boot.starter.oss.recorder.FileRecorder;
import dev.macula.boot.starter.oss.support.FileInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.Iterator;

/**
 * 删除的切面调用链
 */
@Getter
@Setter
public class DeleteAspectChain {

    private DeleteAspectChainCallback callback;
    private Iterator<FileStorageAspect> aspectIterator;

    public DeleteAspectChain(Iterable<FileStorageAspect> aspects, DeleteAspectChainCallback callback) {
        this.aspectIterator = aspects.iterator();
        this.callback = callback;
    }

    /**
     * 调用下一个切面
     */
    public boolean next(FileInfo fileInfo, FileStorage fileStorage, FileRecorder fileRecorder) {
        if (aspectIterator.hasNext()) {//还有下一个
            return aspectIterator.next().deleteAround(this, fileInfo, fileStorage, fileRecorder);
        } else {
            return callback.run(fileInfo, fileStorage, fileRecorder);
        }
    }
}
