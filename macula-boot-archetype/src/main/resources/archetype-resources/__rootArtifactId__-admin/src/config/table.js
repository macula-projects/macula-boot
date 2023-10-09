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

//数据表格配置

import tool from '@/utils/tool'

export default {
    // successCode: 200,												//请求完成代码
    successCode: '00000',												//请求完成代码
    pageSize: 20,													//表格每一页条数
    pageSizes: [10, 20, 30, 40, 50],								//表格可设置的一页条数
    paginationLayout: "total, sizes, prev, pager, next, jumper",	//表格分页布局，可设置"total, sizes, prev, pager, next, jumper"
    parseData: function (res) {										//数据分析
        return {
            data: res.data,				//分析无分页的数据字段结构
            rows: res.data.records,//rows: res.data.rows,		//分析行数据字段结构
            total: res.data.total,		//分析总数字段结构
            summary: res.data.summary,	//分析合计行字段结构
            msg: res.msg,			//分析描述字段结构
            code: res.code				//分析状态字段结构
        }
    },
    request: {							//请求规定字段
        page: 'pageNum',					//规定当前分页字段
        pageSize: 'pageSize',			//规定一页条数字段
        prop: 'prop',					//规定排序字段名字段
        order: 'order'					//规定排序规格字段
    },
    /**
     * 自定义列保存处理
     * @tableName scTable组件的props->tableName
     * @column 用户配置好的列
     */
    columnSettingSave: function (tableName, column) {
        return new Promise((resolve) => {
            setTimeout(() => {
                //这里为了演示使用了session和setTimeout演示，开发时应用数据请求
                tool.session.set(tableName, column)
                resolve(true)
            }, 1000)
        })
    },
    /**
     * 获取自定义列
     * @tableName scTable组件的props->tableName
     * @column 组件接受到的props->column
     */
    columnSettingGet: function (tableName, column) {
        return new Promise((resolve) => {
            //这里为了演示使用了session和setTimeout演示，开发时应用数据请求
            const userColumn = tool.session.get(tableName)
            if (userColumn) {
                resolve(userColumn)
            } else {
                resolve(column)
            }
        })
    },
    /**
     * 重置自定义列
     * @tableName scTable组件的props->tableName
     * @column 组件接受到的props->column
     */
    columnSettingReset: function (tableName, column) {
        return new Promise((resolve) => {
            //这里为了演示使用了session和setTimeout演示，开发时应用数据请求
            setTimeout(() => {
                tool.session.remove(tableName)
                resolve(column)
            }, 1000)
        })
    }
}
