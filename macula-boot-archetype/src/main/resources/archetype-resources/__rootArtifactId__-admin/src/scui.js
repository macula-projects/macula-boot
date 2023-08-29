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

import config from "./config"
import api from './api'
import tool from './utils/tool'
import http from "./utils/request"
import {permission, rolePermission} from './utils/permission'

import errorHandler from './utils/errorHandler'

import * as scDirectives from './directives'
import * as elIcons from '@element-plus/icons-vue'
import * as scIcons from './assets/icons'

export default {
    install(app) {
        //挂载全局对象
        app.config.globalProperties.$CONFIG = config;
        app.config.globalProperties.$TOOL = tool;
        app.config.globalProperties.$HTTP = http;
        app.config.globalProperties.$API = api;
        app.config.globalProperties.$AUTH = permission;
        app.config.globalProperties.$ROLE = rolePermission;

        //统一注册el-icon图标
        for (let icon in elIcons) {
            app.component(`ElIcon${icon}`, elIcons[icon])
        }
        //统一注册sc-icon图标
        for (let icon in scIcons) {
            app.component(`ScIcon${icon}`, scIcons[icon])
        }
        // 统一注册自定义指令
        for (let directives in scDirectives) {
            app.directive(`${directives}`, scDirectives[directives])
        }

        //关闭async-validator全局控制台警告
        window.ASYNC_VALIDATOR_NO_WARNING = 1

        //全局代码错误捕捉
        app.config.errorHandler = errorHandler
    }
}
