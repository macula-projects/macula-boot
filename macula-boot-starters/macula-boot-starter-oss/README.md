# 接入Macula Cloud的Oss模块

对象存储访问，目前支持 Minio ，阿里云 OSS ，腾讯 COS ，亚马逊 AWSS3 ，本地存储，基于aizuda

```java
public class Main {
    public void upload() {
        OSS.fileStorage(platform).bucket(bucketName)
            // 使用默认 yml 配置媒体类型
            .allowMediaType(bis)
            // 只允许gif图片上传,所有图片可以是 image/ 部分匹配
            .allowMediaType(fis, t -> t.startsWith("image/gif")).upload(bis, filename);
    }
}
```

## SpringBoot 使用

- application.yml 配置

```yaml
# 配置存储平台 ，第一位 test-minio 为默认存储平台
macula:
  cloud:
    oss:
      test-minio:
        platform: minio
        endpoint: http://xxxxxx
        accessKey: xxx
        secretKey: xxxxxxx
        bucketName: test1
      aliyun-oss:
        platform: aliyun
        endpoint: http://xxxxxx
        accessKey: xxx
        secretKey: xxxxxxx
        bucketName: test
```

- Bean 方式注入，以下注入 minio3 为平台别名

```java
public class Config {
    @Bean
    public IFileStorage minio3() {
        // 注入一个自定义存储平台
        OssProperty ossProperty = new OssProperty();
        ossProperty.setPlatform(StoragePlatform.minio);
        ossProperty.setBucketName("test3");
        ossProperty.setEndpoint("http://xxxxx");
        ossProperty.setAccessKey("q7RNi6elbvQ0j1ry");
        ossProperty.setSecretKey("HMoKkeu0zGSvSdDGWlMDuytaRON12St9");
        return new Minio(ossProperty);
    }
}
```

- 测试上传 platform 存储平台（不设置使用默认）bucketName 存储桶（不设置使用默认）

```java
// 静态方法方式调用
OSS.fileStorage(platform).bucket(bucketName).upload(fis,filename);

    // 依赖注入方式调用
    fileStorage.bucket(bucketName).upload(fis,filename);
```

## IFileStorage 方法说明

| 属性       | 说明      |
|----------|---------|
| upload   | 上传      |
| delete   | 删除      |
| download | 下载      |
| getUrl   | 文件可预览地址 |

## 配置属性说明

| 属性                | 说明                                          |
|-------------------|---------------------------------------------|
| platform          | 存储平台，目前支持 Minio，阿里云OSS，腾讯COS，亚马逊AWSS3 ，本地存储 |
| endpoint          | 域名                                          |
| accessKey         | 访问 KEY                                      |
| secretKey         | 密钥                                          |
| bucketName        | 存储空间桶名                                      |
| connectionTimeout | 连接超时，阿里云 OSS 有效                             |

