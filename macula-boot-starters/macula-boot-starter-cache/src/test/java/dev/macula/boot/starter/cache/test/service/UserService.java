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

package dev.macula.boot.starter.cache.test.service;

import dev.macula.boot.starter.cache.test.vo.User;

/**
 * <p>
 * <b>UserService</b>
 * </p>
 *
 * @author Rain
 * @since 2022-01-30
 */
public interface UserService {

    User getUser(String userId);

    User getUser2(String userId);

    User getUser3();
}
