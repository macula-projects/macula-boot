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

export default {
    //hex颜色转rgb颜色
    HexToRgb(str) {
        str = str.replace("#", "")
        var hxs = str.match(/../g)
        for (var i = 0; i < 3; i++) hxs[i] = parseInt(hxs[i], 16)
        return hxs
    },
    //rgb颜色转hex颜色
    RgbToHex(a, b, c) {
        var hexs = [a.toString(16), b.toString(16), c.toString(16)]
        for (var i = 0; i < 3; i++) {
            if (hexs[i].length == 1) hexs[i] = "0" + hexs[i]
        }
        return "#" + hexs.join("");
    },
    //加深
    darken(color, level) {
        var rgbc = this.HexToRgb(color)
        for (var i = 0; i < 3; i++) rgbc[i] = Math.floor(rgbc[i] * (1 - level))
        return this.RgbToHex(rgbc[0], rgbc[1], rgbc[2])
    },
    //变淡
    lighten(color, level) {
        var rgbc = this.HexToRgb(color)
        for (var i = 0; i < 3; i++) rgbc[i] = Math.floor((255 - rgbc[i]) * level + rgbc[i])
        return this.RgbToHex(rgbc[0], rgbc[1], rgbc[2])
    }
}
