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

import {ElMessage} from 'element-plus'

export default {
    mounted(el, binding) {
        el.$value = binding.value
        el.handler = () => {
            const textarea = document.createElement('textarea')
            textarea.readOnly = 'readonly'
            textarea.style.position = 'absolute'
            textarea.style.left = '-9999px'
            textarea.value = el.$value
            document.body.appendChild(textarea)
            textarea.select()
            textarea.setSelectionRange(0, textarea.value.length)
            const result = document.execCommand('Copy')
            if (result) {
                ElMessage.success("复制成功")
            }
            document.body.removeChild(textarea)
        }
        el.addEventListener('click', el.handler)
    },
    updated(el, binding) {
        el.$value = binding.value
    },
    unmounted(el) {
        el.removeEventListener('click', el.handler)
    }
}
