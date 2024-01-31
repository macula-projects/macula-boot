## 概述

在 SpringBoot 中通过简单的方式将文件存储到 本地、FTP、SFTP、WebDAV、FastDFS、Minio、谷歌云存储、阿里云OSS、华为云OBS、七牛云Kodo、腾讯云COS、MinIO、
AWS S3及其它兼容 S3 协议的平台。详情可以参考[x-file-storage](https://x-file-storage.xuyanwu.cn/)官网

## 组件坐标

```xml

<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-oss</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用配置

{{% alert title="提示" color="primary" %}}
注意官网默认配置是以dromara.x-file-storage，由于macula oss没有使用官网的starter，所以这里是以macula.oss开头，其他保持不变。
{{% /alert %}}
```yaml
# 配置存储平台 ，第一位 test-minio 为默认存储平台
macula:
  oss: #文件存储配置
    default-platform: local-1 #默认使用的存储平台
    thumbnail-suffix: ".min.jpg" #缩略图后缀，例如【.min.jpg】【.png】
    local: # 本地存储（不推荐使用），不使用的情况下可以不写
      - platform: local-1 # 存储平台标识
        enable-storage: true  #启用存储
        enable-access: true #启用访问（线上请使用 Nginx 配置，效率更高）
        domain: "" # 访问域名，例如：“http://127.0.0.1:8030/test/file/”，注意后面要和 path-patterns 保持一致，“/”结尾，本地存储建议使用相对路径，方便后期更换域名
        base-path: D:/Temp/test/ # 存储地址
        path-patterns: /test/file/** # 访问路径，开启 enable-access 后，通过此路径可以访问到上传的文件
    local-plus: # 本地存储升级版，不使用的情况下可以不写
      - platform: local-plus-1 # 存储平台标识
        enable-storage: true  #启用存储
        enable-access: true #启用访问（线上请使用 Nginx 配置，效率更高）
        domain: "" # 访问域名，例如：“http://127.0.0.1:8030/”，注意后面要和 path-patterns 保持一致，“/”结尾，本地存储建议使用相对路径，方便后期更换域名
        base-path: local-plus/ # 基础路径
        path-patterns: /** # 访问路径
        storage-path: D:/Temp/ # 存储路径
    huawei-obs: # 华为云 OBS ，不使用的情况下可以不写
      - platform: huawei-obs-1 # 存储平台标识
        enable-storage: false  # 启用存储
        access-key: ??
        secret-key: ??
        end-point: ??
        bucket-name: ??
        domain: ?? # 访问域名，注意“/”结尾，例如：http://abc.obs.com/
        base-path: hy/ # 基础路径
    aliyun-oss: # 阿里云 OSS ，不使用的情况下可以不写
      - platform: aliyun-oss-1 # 存储平台标识
        enable-storage: false  # 启用存储
        access-key: ??
        secret-key: ??
        end-point: ??
        bucket-name: ??
        domain: ?? # 访问域名，注意“/”结尾，例如：https://abc.oss-cn-shanghai.aliyuncs.com/
        base-path: hy/ # 基础路径
    qiniu-kodo: # 七牛云 kodo ，不使用的情况下可以不写
      - platform: qiniu-kodo-1 # 存储平台标识
        enable-storage: false  # 启用存储
        access-key: ??
        secret-key: ??
        bucket-name: ??
        domain: ?? # 访问域名，注意“/”结尾，例如：http://abc.hn-bkt.clouddn.com/
        base-path: base/ # 基础路径
    tencent-cos: # 腾讯云 COS
      - platform: tencent-cos-1 # 存储平台标识
        enable-storage: true  # 启用存储
        secret-id: ??
        secret-key: ??
        region: ?? #存仓库所在地域
        bucket-name: ??
        domain: ?? # 访问域名，注意“/”结尾，例如：https://abc.cos.ap-nanjing.myqcloud.com/
        base-path: hy/ # 基础路径
    minio: # MinIO，由于 MinIO SDK 支持 AWS S3，其它兼容 AWS S3 协议的存储平台也都可配置在这里
      - platform: minio-1 # 存储平台标识
        enable-storage: true  # 启用存储
        access-key: ??
        secret-key: ??
        end-point: ??
        bucket-name: ??
        domain: ?? # 访问域名，注意“/”结尾，例如：http://minio.abc.com/abc/
        base-path: hy/ # 基础路径
    aws-s3: # AWS S3，其它兼容 AWS S3 协议的存储平台也都可配置在这里
      - platform: aws-s3-1 # 存储平台标识
        enable-storage: true  # 启用存储
        access-key: ??
        secret-key: ??
        region: ?? # 与 end-point 参数至少填一个
        end-point: ?? # 与 region 参数至少填一个
        bucket-name: ??
        domain: ?? # 访问域名，注意“/”结尾，例如：https://abc.hn-bkt.clouddn.com/
        base-path: s3/ # 基础路径
    ftp: # FTP
      - platform: ftp-1 # 存储平台标识
        enable-storage: true  # 启用存储
        host: ?? # 主机，例如：192.168.1.105
        port: 21 # 端口，默认21
        user: anonymous # 用户名，默认 anonymous（匿名）
        password: "" # 密码，默认空
        domain: ?? # 访问域名，注意“/”结尾，例如：ftp://192.168.1.105/
        base-path: ftp/ # 基础路径
        storage-path: /www/wwwroot/file.abc.com/ # 存储路径，可以配合 Nginx 实现访问，注意“/”结尾，默认“/”
    sftp: # SFTP
      - platform: sftp-1 # 存储平台标识
        enable-storage: true  # 启用存储
        host: ?? # 主机，例如：192.168.1.105
        port: 22 # 端口，默认22
        user: root # 用户名
        password: ?? # 密码或私钥密码
        private-key-path: ?? # 私钥路径，兼容Spring的ClassPath路径、文件路径、HTTP路径等，例如：classpath:id_rsa_2048
        domain: ?? # 访问域名，注意“/”结尾，例如：https://file.abc.com/
        base-path: sftp/ # 基础路径
        storage-path: /www/wwwroot/file.abc.com/ # 存储路径，可以配合 Nginx 实现访问，注意“/”结尾，默认“/”
    webdav: # WebDAV
      - platform: webdav-1 # 存储平台标识
        enable-storage: true  # 启用存储
        server: ?? # 服务器地址，例如：http://192.168.1.105:8405/
        user: ?? # 用户名
        password: ?? # 密码
        domain: ?? # 访问域名，注意“/”结尾，例如：https://file.abc.com/
        base-path: webdav/ # 基础路径
        storage-path: / # 存储路径，可以配合 Nginx 实现访问，注意“/”结尾，默认“/”
    google-cloud: # 谷歌云存储
      - platform: google-1 # 存储平台标识
        enable-storage: true  # 启用存储
        project-id: ?? # 项目 id
        bucket-name: ??
        credentials-path: file:/deploy/example-key.json # 授权 key json 路径，兼容Spring的ClassPath路径、文件路径、HTTP路径等
        domain: ?? # 访问域名，注意“/”结尾，例如：https://storage.googleapis.com/test-bucket/
        base-path: hy/ # 基础路径
    fastdfs:
      - platform: fastdfs-1 # 存储平台标识
        enable-storage: true  # 启用存储
        run-mod: COVER #运行模式，默认 COVER（覆盖模式），强制用 FastDFS 返回的路径及文件名覆盖 FileInfo 中的 path 及 filename。
        tracker-server: # Tracker Server 配置
          server-addr: ?? # Tracker Server 地址（IP:PORT），多个用英文逗号隔开
        domain: ?? # 访问域名，注意“/”结尾，例如：https://file.abc.com/
        base-path: "" # 基础路径，强烈建议留空，详情查看兼容性说明章节
```

注意配置每个平台前面都有个-号，通过以下方式可以配置多个：

```yaml
local:
  - platform: local-1 # 存储平台标识
    enable-storage: true
    enable-access: true
    domain: ""
    base-path: D:/Temp/test/
    path-patterns: /test/file/**
  - platform: local-2 # 存储平台标识，注意这里不能重复
    enable-storage: true
    enable-access: true
    domain: ""
    base-path: D:/Temp/test2/
    path-patterns: /test2/file/**
```

## 核心功能

### 上传

支持 File、MultipartFile、byte[]
、InputStream、URL、URI、String、HttpServletRequest，大文件会自动分片上传。如果想支持更多方式，请阅读 [文件适配器](https://x-file-storage.xuyanwu.cn/#/%E6%96%87%E4%BB%B6%E9%80%82%E9%85%8D%E5%99%A8)
章节

```java
@RestController
public class FileDetailController {

    @Autowired
    private FileStorageService fileStorageService;//注入实列

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public FileInfo upload(MultipartFile file) {
        return fileStorageService.of(file).upload();
    }
    
    /**
     * 上传文件，成功返回文件 url
     */
    @PostMapping("/upload2")
    public String upload2(MultipartFile file) {
        FileInfo fileInfo = fileStorageService.of(file)
                .setPath("upload/") //保存到相对路径下，为了方便管理，不需要可以不写
                .setObjectId("0")   //关联对象id，为了方便管理，不需要可以不写
                .setObjectType("0") //关联对象类型，为了方便管理，不需要可以不写
                .putAttr("role","admin") //保存一些属性，可以在切面、保存上传记录、自定义存储平台等地方获取使用，不需要可以不写
                .upload();  //将文件上传到对应地方
        return fileInfo == null ? "上传失败！" : fileInfo.getUrl();
    }

    /**
     * 上传图片，成功返回文件信息
     * 图片处理使用的是 https://github.com/coobird/thumbnailator
     */
    @PostMapping("/upload-image")
    public FileInfo uploadImage(MultipartFile file) {
        return fileStorageService.of(file)
                .image(img -> img.size(1000,1000))  //将图片大小调整到 1000*1000
                .thumbnail(th -> th.size(200,200))  //再生成一张 200*200 的缩略图
                .upload();
    }

    /**
     * 上传文件到指定存储平台，成功返回文件信息
     */
    @PostMapping("/upload-platform")
    public FileInfo uploadPlatform(MultipartFile file) {
        return fileStorageService.of(file)
                .setPlatform("aliyun-oss-1")    //使用指定的存储平台
                .upload();
    }

    /**
     * 直接读取 HttpServletRequest 中的文件进行上传，成功返回文件信息
     * 使用这种方式有些注意事项，请查看文档 基础功能-上传 章节
     */
    @PostMapping("/upload-request")
    public FileInfo uploadPlatform(HttpServletRequest request) {
        return fileStorageService.of(request).upload();
    }
}

```

### 其它操作
```java
//手动构造文件信息，可用于其它操作
FileInfo fileInfo = new FileInfo()
        .setPlatform("huawei-obs-1")
        .setBasePath("test/")
        .setPath("aa/")
        .setFilename("image.png")
        .setThFilename("image.png.min.jpg");

//文件是否存在
boolean exists = fileStorageService.exists(fileInfo);
//下载
byte[] bytes = fileStorageService.download(fileInfo).bytes();
//删除
fileStorageService.delete(fileInfo);
//其它更多操作
```

具体的上传、下载、保存记录等所有操作请参考[官方网站](https://x-file-storage.xuyanwu.cn/#/%E5%9F%BA%E7%A1%80%E5%8A%9F%E8%83%BD)

## 依赖引入

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.dromara.x-file-storage</groupId>
        <artifactId>x-file-storage-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## 版权说明

- x-file-storage：https://github.com/dromara/x-file-storage/blob/main/LICENSE