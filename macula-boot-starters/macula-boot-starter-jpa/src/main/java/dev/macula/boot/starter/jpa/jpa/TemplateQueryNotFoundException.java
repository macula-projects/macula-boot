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
 * NotFoundTemplateException.java 2016年11月2日
 */
package dev.macula.boot.starter.jpa.jpa;

import dev.macula.boot.exception.MaculaException;

/**
 * <p>
 * <b>TemplateQueryNotFoundException</b> 找不到SQL定义的模板异常
 * </p>
 *
 * @author Rain
 * @version $Id$
 * @since 2016年11月2日
 */
public class TemplateQueryNotFoundException extends MaculaException {

    private static final long serialVersionUID = 1L;

    public TemplateQueryNotFoundException(String message) {
        super(message);
    }
}
