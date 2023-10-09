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

//文件选择器配置

export default {
    apiObj: API.common.upload,
    menuApiObj: API.common.file.menu,
    listApiObj: API.common.file.list,
    successCode: 200,
    maxSize: 30,
    max: 99,
    uploadParseData: function (res) {
        return {
            id: res.data.id,
            fileName: res.data.fileName,
            url: res.data.src
        }
    },
    listParseData: function (res) {
        return {
            rows: res.data.rows,
            total: res.data.total,
            msg: res.msg,
            code: res.code
        }
    },
    request: {
        page: 'page',
        pageSize: 'pageSize',
        keyword: 'keyword',
        menuKey: 'groupId'
    },
    menuProps: {
        key: 'id',
        label: 'label',
        children: 'children'
    },
    fileProps: {
        key: 'id',
        fileName: 'fileName',
        url: 'url'
    },
    files: {
        doc: {
            icon: 'sc-icon-file-word-2-fill',
            color: '#409eff'
        },
        docx: {
            icon: 'sc-icon-file-word-2-fill',
            color: '#409eff'
        },
        xls: {
            icon: 'sc-icon-file-excel-2-fill',
            color: '#67C23A'
        },
        xlsx: {
            icon: 'sc-icon-file-excel-2-fill',
            color: '#67C23A'
        },
        ppt: {
            icon: 'sc-icon-file-ppt-2-fill',
            color: '#F56C6C'
        },
        pptx: {
            icon: 'sc-icon-file-ppt-2-fill',
            color: '#F56C6C'
        }
    }
}
