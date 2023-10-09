/*
 * Copyright (c) 2022 Macula
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

#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service1.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ${package}.service1.form.ApplicationForm;
import ${package}.service1.pojo.bo.ApplicationBO;
import ${package}.service1.pojo.entity.Application;
import ${package}.service1.vo.app.ApplicationVO;
import org.mapstruct.Mapper;

/**
 * 应用对象转换器
 */
@Mapper
public interface ApplicationConverter {

    Application form2Entity(ApplicationForm appForm);

    Page<ApplicationVO> bo2Vo(Page<ApplicationBO> bo);

}