## 概述

开发JAVA应用程序时会进行逻辑分层，各层之间为了解耦，通常会定义不同的对象进行数据传递（如XXXVO、XXXDTO、XXXPO、XXXBO）。当在不同层之间进行数据传递时，需要将这些对象进行相互转换。通常有两种处理方式：一是直接使用Setter和Getter方法转换，二是使用一些工具类进行转换（如BeanUtil.copyProperties）。第一种方式如果对象属性比较多时，要写很多的Getter/Setter代码，很繁琐；第二种方式虽然比第一种方式要简单很多，但是因为使用了反射，性能不佳。故在此推荐，统一使用MapStruct（MapStructPlus），代码简洁且不存在性能问题。

## 组件坐标

```xml

<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-mapstruct</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 核心功能

### 基于MapStruct

MapStruct是一个类型转换工具，通过定义类型转换接口（如下所示），然后在编译的时候自动生成实现类来工作。

```java
@Mapper
public interface SourceTargetMapper {

    SourceTargetMapper MAPPER = Mappers.getMapper( SourceTargetMapper.class );

    @Mapping( source = "username", target = "user" )
    TargetDto toTarget( SourceDto s );
}
```

另外，使用mapstruct前，需要在你的父工程的Maven项目的pom中定义以下build插件：

```xml

<build>
    <pluginManagement>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <!-- See https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html -->
                    <!-- Classpath elements to supply as annotation processor path. If specified, the compiler   -->
                    <!-- will detect annotation processors only in those classpath elements. If omitted, the     -->
                    <!-- default classpath is used to detect annotation processors. The detection itself depends -->
                    <!-- on the configuration of annotationProcessors.                                           -->
                    <!--                                                                                         -->
                    <!-- According to this documentation, the provided dependency processor is not considered!   -->
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>0.2.0</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </pluginManagement>
</build>
```

***MapStruct更多功能可以[参考官方文档](https://mapstruct.plus/mapstruct/1-5-3-Final.html)***

### 基于MapStruct Plus

下面演示如何使用 MapStruct Plus 来映射两个对象。

假设有两个类 `UserDto` 和 `User`，分别表示数据层对象和业务层对象：

```java
public class UserDto {
    private String username;
    private int age;
    private boolean young;

    // getter、setter、toString、equals、hashCode
}

public class User {
    private String username;
    private int age;
    private boolean young;

    // getter、setter、toString、equals、hashCode
}
```

测试：

```java

@SpringBootTest
public class QuickStartTest {
    private static final Converter converter = new Converter();

    @Test
    public void test() {
        User user = new User();
        user.setUsername("jack");
        user.setAge(23);
        user.setYoung(false);

        UserDto userDto = converter.convert(user, UserDto.class);
        System.out.println(userDto);    // UserDto{username='jack', age=23, young=false}

        assert user.getUsername().equals(userDto.getUsername());
        assert user.getAge() == userDto.getAge();
        assert user.isYoung() == userDto.isYoung();

        User newUser = converter.convert(userDto, User.class);

        System.out.println(newUser);    // User{username='jack', age=23, young=false}

        assert user.getUsername().equals(newUser.getUsername());
        assert user.getAge() == newUser.getAge();
        assert user.isYoung() == newUser.isYoung();
    }

}
```

***MapStruct Plus更多功能可以[参考官方文档](https://mapstruct.plus/)***

## 依赖引入

```xml

<dependencies>
    <dependency>
        <groupId>io.github.linpeilie</groupId>
        <artifactId>mapstruct-plus-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

## 版权说明

- MapStruct：https://github.com/mapstruct/mapstruct/blob/main/LICENSE.txt

- MapStruct Plus：https://github.com/linpeilie/mapstruct-plus/blob/main/LICENSE

