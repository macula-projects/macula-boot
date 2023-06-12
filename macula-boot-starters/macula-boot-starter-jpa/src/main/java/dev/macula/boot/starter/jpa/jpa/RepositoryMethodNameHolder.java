/*
 * Copyright 2004-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * RepositoryMethodNameHolder.java 2016年6月22日
 */
package dev.macula.boot.starter.jpa.jpa;

/**
 * <p>
 * <b>RepositoryMethodNameHolder</b> 存放当前线程的SQL名称
 * </p>
 *
 * @author Rain
 * @version $Id$
 * @since 2016年6月22日
 */
public class RepositoryMethodNameHolder {
    private static ThreadLocal<String> sqlName = new ThreadLocal<String>();

    public static void set(String name) {
        sqlName.set(name);
    }

    public static String get() {
        return sqlName.get();
    }

    public static void remove() {
        sqlName.remove();
    }
}
