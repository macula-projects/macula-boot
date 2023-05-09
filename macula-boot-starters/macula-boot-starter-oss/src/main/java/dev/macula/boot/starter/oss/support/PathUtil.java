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

package dev.macula.boot.starter.oss.support;

public class PathUtil {

    /**
     * 获取父路径
     */
    public static String getParent(String path) {
        if (path.endsWith("/") || path.endsWith("\\")) {
            path = path.substring(0, path.length() - 1);
        }
        int endIndex = Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
        return endIndex > -1 ? path.substring(0, endIndex) : null;
    }

    /**
     * 合并路径
     */
    public static String join(String... paths) {
        StringBuilder sb = new StringBuilder();
        for (String path : paths) {
            String left = sb.toString();
            boolean leftHas = left.endsWith("/") || left.endsWith("\\");
            boolean rightHas = path.endsWith("/") || path.endsWith("\\");

            if (leftHas && rightHas) {
                sb.append(path.substring(1));
            } else if (!left.isEmpty() && !leftHas && !rightHas) {
                sb.append("/").append(path);
            } else {
                sb.append(path);
            }
        }
        return sb.toString();
    }
}
