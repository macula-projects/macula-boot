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
@Mapper(componentModel = MappingConstants.ComponentModel.DEFAULT)
public interface SourceTargetMapper {

    SourceTargetMapper MAPPER = Mappers.getMapper( SourceTargetMapper.class );

    @Mapping( source = "username", target = "user" )
    TargetDto toTarget( SourceDto s );
}
```

继承 `macula-boot-parent` 时已配置 Lombok、`mapstruct-plus-processor`、`lombok-mapstruct-binding` 和 `-Amapstruct.defaultComponentModel=spring`，不必重复复制编译插件配置。默认生成 Spring Bean，优先注入 Mapper；上面的 `Mappers.getMapper` 示例适用于显式使用默认组件模型且无 Spring 依赖的 Mapper。

仅导入 BOM 不会继承构建插件，需在应用父 POM 中配置对应 annotation processor。此模块无运行时 YAML 属性；转换实现于编译期生成，IDE 也需启用注解处理。

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
