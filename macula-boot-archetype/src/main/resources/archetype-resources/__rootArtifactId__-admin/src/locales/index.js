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

import sysConfig from "@/config"
import tool from '@/utils/tool'
import {createI18n} from 'vue-i18n'
import zhCn from "element-plus/dist/locale/zh-cn";
import en from "element-plus/dist/locale/en";

import my_zh_cn from './lang/zh-cn.js'
import my_en from './lang/en.js'

const messages = {
    'zh-cn': {
        el: zhCn,
        ...my_zh_cn
    },
    'en': {
        el: en,
        ...my_en
    }
}

const i18n = createI18n({
    locale: tool.data.get("APP_LANG") || sysConfig.LANG,
    fallbackLocale: 'zh-cn',
    globalInjection: true,
    messages,
})

export default i18n;
