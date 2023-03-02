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

package dev.macula.boot.starter.springfox;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code Swagger3DemoController} is
 *
 * @author rain
 * @since 2022/7/5 19:19
 */
@RestController
@RequestMapping(value = "swagger")
@Tag(name = "swagger演示controller", description = "模拟添删改查接口,生成接口文档")
public class Swagger3DemoController {
    public static Integer id = 1;
    public static Map<Integer, User> users = new HashMap();

    @PostMapping("add")
    @Operation(summary = "添加用户接口", description = "可以用来新增用户")
    @Parameter(name = "user", description = "用户实体模型")
    public ResponseEntity<Map> addUser(@RequestBody User user) {
        users.put(id++, user);
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("delete/{id}")
    @Operation(summary = "删除用户接口", description = "可以用来删除用户")
    @Parameter(name = "id", description = "用户ID")
    public ResponseEntity<Map> deleteUserById(@PathVariable(name = "id") Integer id) {
        users.remove(id);
        return ResponseEntity.ok(users);
    }

    @PutMapping("update")
    @Operation(summary = "修改用户接口", description = "可以用来修改用户")
    @Parameter(name = "id", description = "用户ID", in = ParameterIn.QUERY)
    @Parameters({@Parameter(name = "userName", description = "用户名", in = ParameterIn.QUERY),
        @Parameter(name = "passWord", description = "密码", in = ParameterIn.QUERY)})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = {@Content(mediaType = "application/x-www-form-urlencoded")})
    public ResponseEntity<Map> updateUser(Integer id, String userName, String passWord) {
        User user = users.get(id);
        user.setUserName(userName);
        user.setPassWord(passWord);
        users.put(id, user);
        return ResponseEntity.ok(users);
    }

    @GetMapping("find")
    @Operation(summary = "查找用户接口", description = "可以用来查询用户")
    @Parameter(name = "id", description = "用户ID")
    public ResponseEntity<User> findUserById(Integer id) {
        User user = users.get(id);
        return ResponseEntity.ok(user);
    }

}

@Data
@Schema(description = "用户实体模型")
class User {
    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "密码")
    private String passWord;
}
