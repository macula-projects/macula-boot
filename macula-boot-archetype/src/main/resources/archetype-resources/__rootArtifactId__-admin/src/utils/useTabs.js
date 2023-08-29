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

import {nextTick} from 'vue'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import router from '@/router'
import {useViewTagsStore} from '@/stores/viewTags'
import {useIframeStore} from '@/stores/iframe'
import {useKeepAliveStore} from '@/stores/keepAlive'

export default {
    //刷新标签
    refresh() {
        NProgress.start()
        const keepAliveStore = useKeepAliveStore()
        const route = router.currentRoute.value
        keepAliveStore.removeKeepLive(route.name)
        keepAliveStore.setRouteShow(false)
        nextTick(() => {
            keepAliveStore.pushKeepLive(route.name)
            keepAliveStore.setRouteShow(true)
            NProgress.done()
        })
    },
    //关闭标签
    close(tag) {
        const route = tag || router.currentRoute.value
        const viewTagsStore = useViewTagsStore()
        const keepAliveStore = useKeepAliveStore()
        const iframeStore = useIframeStore()
        viewTagsStore.removeViewTags(route)
        iframeStore.removeIframeList(route)
        keepAliveStore.removeKeepLive(route.name)
        const tagList = viewTagsStore.viewTags
        const latestView = tagList.slice(-1)[0]
        if (latestView) {
            router.push(latestView)
        } else {
            router.push('/')
        }
    },
    //关闭标签后处理
    closeNext(next) {
        const route = router.currentRoute.value
        const viewTagsStore = useViewTagsStore()
        const keepAliveStore = useKeepAliveStore()
        const iframeStore = useIframeStore()
        viewTagsStore.removeViewTags(route)
        iframeStore.removeIframeList(route)
        keepAliveStore.removeKeepLive(route.name)
        if (next) {
            const tagList = viewTagsStore.viewTags
            next(tagList)
        }
    },
    //关闭其他
    closeOther() {
        const route = router.currentRoute.value
        const viewTagsStore = useViewTagsStore()
        const tagList = [...viewTagsStore.viewTags]
        tagList.forEach(tag => {
            if (tag.meta && tag.meta.affix || route.fullPath == tag.fullPath) {
                return true
            } else {
                this.close(tag)
            }
        })
    },
    //设置标题
    setTitle(title) {
        const viewTagsStore = useViewTagsStore()
        viewTagsStore.updateViewTagsTitle(title)
    }
}
