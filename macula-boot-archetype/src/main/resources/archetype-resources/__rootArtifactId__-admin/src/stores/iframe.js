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

import {defineStore} from "pinia";

export const useIframeStore = defineStore({
    id: 'iframe',
    state: () => ({
        iframeList: []
    }),
    actions: {
        setIframeList(route) {
            this.iframeList = []
            this.iframeList.push(route)
        },
        pushIframeList(route) {
            let target = this.iframeList.find((item) => item.path === route.path)
            if (!target) {
                this.iframeList.push(route)
            }
        },
        removeIframeList(route) {
            this.iframeList.forEach((item, index) => {
                if (item.path === route.path) {
                    this.iframeList.splice(index, 1)
                }
            })
        },
        refreshIframe(route) {
            this.iframeList.forEach((item) => {
                if (item.path == route.path) {
                    var url = route.meta.url;
                    item.meta.url = '';
                    setTimeout(function () {
                        item.meta.url = url
                    }, 200);
                }
            })
        },
        clearIframeList() {
            this.iframeList = []
        }
    }
});