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

import API from "@/api";

//审批工作流人员/组织选择器配置

export default {
    //配置接口正常返回代码
    successCode: 200,
    //配置组织
    group: {
        //请求接口对象
        apiObj: API.system.dept.list,
        //接受数据字段映射
        parseData: function (res) {
            return {
                rows: res.data,
                msg: res.message,
                code: res.code
            }
        },
        //显示数据字段映射
        props: {
            key: 'id',
            label: 'label',
            children: 'children'
        }
    },
    //配置用户
    user: {
        apiObj: API.demo.page,
        pageSize: 20,
        parseData: function (res) {
            return {
                rows: res.data.rows,
                total: res.data.total,
                msg: res.message,
                code: res.code
            }
        },
        props: {
            key: 'id',
            label: 'user',
        },
        request: {
            page: 'page',
            pageSize: 'pageSize',
            groupId: 'groupId',
            keyword: 'keyword'
        }
    },
    //配置角色
    role: {
        //请求接口对象
        apiObj: API.system.dept.list,
        //接受数据字段映射
        parseData: function (res) {
            return {
                rows: res.data,
                msg: res.message,
                code: res.code
            }
        },
        //显示数据字段映射
        props: {
            key: 'id',
            label: 'label',
            children: 'children'
        }
    }
}
