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

import config from "@/config";
import {defineStore} from "pinia";

export const useGlobalStore = defineStore({
    id: 'global',
    state: () => ({
        //移动端布局
        ismobile: false,
        //布局
        layout: config.LAYOUT,
        //菜单是否折叠 toggle
        menuIsCollapse: config.MENU_IS_COLLAPSE,
        //多标签栏
        layoutTags: config.LAYOUT_TAGS,
        //主题
        theme: config.THEME,
    }),
    actions: {
        setIsMobile(flag) {
            this.ismobile = flag
        },
        toggleMenuIsCollapse() {
            this.menuIsCollapse = !this.menuIsCollapse
        },
        toggleLayoutTags() {
            this.layoutTags = !this.layoutTags
        }
    }
});
