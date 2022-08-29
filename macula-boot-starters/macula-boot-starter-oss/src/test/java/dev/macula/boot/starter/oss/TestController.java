/*
 * Copyright (c) 2022 Macula
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

package dev.macula.boot.starter.oss;

import cn.hutool.core.io.IoUtil;
import dev.macula.boot.starter.oss.model.OssResult;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

@RestController
public class TestController {
    @Resource
    private IFileStorage fileStorage;

    /**
     * http://localhost:8080/upload
     * http://localhost:8080/upload?platform=test-minio
     * http://localhost:8080/upload?platform=minio3
     * http://localhost:8080/upload?platform=aliyunOss
     */
    @GetMapping("upload")
    public OssResult upload(String platform, String bucketName) {
        OssResult ossResult = null;
        try {
            // 未设置 bucketName 会使用默认捅存储
            String filename = "D:\\IdeaProjects\\aizuda\\aizuda-components-examples\\aizuda-oss-example\\pom.xml";
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            IoUtil.copy(new FileInputStream(filename), os);
            ByteArrayInputStream bis = new ByteArrayInputStream(os.toByteArray());
            if (StringUtils.hasLength(platform)) {
                // 指定 platform 平台存储
                ossResult = OSS.fileStorage(platform).bucket(bucketName)
                        // 使用默认 yml 配置媒体类型
                        .allowMediaType(bis)
                        // 只允许gif图片上传,所有图片可以是 image/ 部分匹配
                        //.allowMediaType(fis, t -> t.startsWith("image/gif"))
                        .upload(bis, filename);
            } else {
                // 调用默认注入实现存储平台
                ossResult = fileStorage.bucket(bucketName).upload(bis, filename);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ossResult;
    }

    /**
     * http://localhost:8080/download?objectName=202206/9d68578d-1e61-4eaa-acc4-c900f0bed0c9.gif
     * http://localhost:8080/download?platform=平台&bucketName=桶名称&objectName=文件名
     */
    @GetMapping("download")
    public void download(HttpServletResponse response, String platform, String bucketName, String objectName) {
        try {
            if (StringUtils.hasLength(platform)) {
                OSS.fileStorage(platform).bucket(bucketName).download(response, objectName);
            } else {
                fileStorage.bucket(bucketName).download(response, objectName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * http://localhost:8080/delete?objectName=202206/9d68578d-1e61-4eaa-acc4-c900f0bed0c9.gif
     * http://localhost:8080/test?platform=test-minio&objectName=文件名
     */
    @GetMapping("delete")
    public boolean delete(String platform, String bucketName, String objectName) {
        boolean result = false;
        try {
            // 未设置 bucketName 会使用默认捅存储
            if (StringUtils.hasLength(platform)) {
                // 指定 platform 平台存储
                result = OSS.fileStorage(platform).bucket(bucketName).delete(objectName);
            } else {
                // 调用默认注入实现存储平台
                result = fileStorage.bucket(bucketName).delete(objectName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
