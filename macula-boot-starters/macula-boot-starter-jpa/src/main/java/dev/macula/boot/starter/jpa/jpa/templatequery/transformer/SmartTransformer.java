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

package dev.macula.boot.starter.jpa.jpa.templatequery.transformer;

import org.hibernate.transform.BasicTransformerAdapter;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;

/**
 * .
 * <p>
 *
 * @author <a href="mailto:stormning@163.com">stormning</a>
 * @version V1.0, 2015/9/1.
 */
public class SmartTransformer extends BasicTransformerAdapter {

    private static final long serialVersionUID = -6916576469017073306L;

    private static DefaultConversionService conversionService = new DefaultConversionService();

    private final Class<?> clazz;

    public SmartTransformer(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Object transformTuple(Object[] tuple, String[] aliases) {
        if (tuple != null && tuple.length > 0) {
            return conversionService.convert(tuple[0], clazz);
        }
        return null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List transformList(List list) {
        return super.transformList(list);
    }
}
