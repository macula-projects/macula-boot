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

import router from '@/router'
import {defineStore} from "pinia";

const reg = /\#reloaded$/

export const useViewTagsStore = defineStore({
    id: 'viewTags',
    state: () => ({
        viewTags: []
    }),
    actions: {
        pushViewTags(route) {
            let backPathIndex = this.viewTags.findIndex(item => item.fullPath == router.options.history.state.back)
            let target = this.viewTags.find((item) => item.fullPath.replace(reg, '') === route.fullPath.replace(reg, ''))
            let isName = route.name
            if (!target && isName) {
                if (backPathIndex == -1) {
                    this.viewTags.push(route)
                } else {
                    this.viewTags.splice(backPathIndex + 1, 0, route)
                }
            }
        },
        removeViewTags(route) {
            this.viewTags.forEach((item, index) => {
                if (item.fullPath === route.fullPath) {
                    this.viewTags.splice(index, 1)
                }
            })
        },
        updateViewTags(route) {
            this.viewTags.forEach((item) => {
                if (item.fullPath == route.fullPath) {
                    item = Object.assign(item, route)
                }
            })
        },
        updateViewTagsTitle(title = '') {
            const nowFullPath = location.hash.substring(1)
            this.viewTags.forEach((item) => {
                if (item.fullPath == nowFullPath) {
                    item.meta.title = title
                }
            })
        },
        clearViewTags() {
            this.viewTags = []
        }
    }
});
