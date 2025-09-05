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

package dev.macula.boot.starter.jpa.test;

import dev.macula.boot.starter.jpa.test.entity.Blog;
import dev.macula.boot.starter.jpa.test.repository.BlogRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * BlogRepository 的单元测试类.
 *
 * @author blinkfox on 2019-08-16.
 */
@SpringBootTest
public class BlogRepositoryTest {

    @Resource
    private BlogRepository blogRepository;

    /**
     * 测试使用 {@link com.blinkfox.fenix.jpa.QueryFenix} 注解根据任意参数多条件模糊分页查询博客信息.
     */
    @Test
    public void queryMyBlogs() {
        // 构造查询的相关参数.
        List<String> ids = Arrays.asList("1", "2", "3", "4", "5", "6");
        Blog blog = new Blog().setAuthor("ZhangSan");
        Pageable pageable = PageRequest.of(0, 3, Sort.by(Sort.Order.desc("createTime")));

        // 查询并断言查询结果的正确性.
        Page<Blog> blogs = blogRepository.queryMyBlogs(ids, blog, pageable);
        Assertions.assertEquals(4, blogs.getTotalElements());
        Assertions.assertEquals(3, blogs.getContent().size());
    }
}
