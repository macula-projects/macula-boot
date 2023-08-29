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

import {useViewTagsStore} from '@/stores/viewTags'
import {nextTick} from 'vue'

export function beforeEach(to, from) {
    var adminMain = document.querySelector('#adminui-main')
    if (!adminMain) {
        return false
    }

    const viewTagsStore = useViewTagsStore()
    viewTagsStore.updateViewTags({
        fullPath: from.fullPath,
        scrollTop: adminMain.scrollTop
    })
}

export function afterEach(to) {
    var adminMain = document.querySelector('#adminui-main')
    if (!adminMain) {
        return false
    }

    const viewTagsStore = useViewTagsStore()
    nextTick(() => {
        var beforeRoute = viewTagsStore.viewTags.filter(v => v.fullPath == to.fullPath)[0]
        if (beforeRoute) {
            adminMain.scrollTop = beforeRoute.scrollTop || 0
        }
    })
}