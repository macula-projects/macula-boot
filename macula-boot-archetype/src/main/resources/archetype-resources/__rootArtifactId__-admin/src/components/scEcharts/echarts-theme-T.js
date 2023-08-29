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

const T = {
    "color": [
        "#409EFF",
        "#36CE9E",
        "#f56e6a",
        "#626c91",
        "#edb00d",
        "#909399"
    ],
    'grid': {
        'left': '3%',
        'right': '3%',
        'bottom': '10',
        'top': '40',
        'containLabel': true
    },
    "legend": {
        "textStyle": {
            "color": "#999"
        },
        "inactiveColor": "rgba(128,128,128,0.4)"
    },
    "categoryAxis": {
        "axisLine": {
            "show": true,
            "lineStyle": {
                "color": "rgba(128,128,128,0.2)",
                "width": 1
            }
        },
        "axisTick": {
            "show": false,
            "lineStyle": {
                "color": "#333"
            }
        },
        "axisLabel": {
            "color": "#999"
        },
        "splitLine": {
            "show": false,
            "lineStyle": {
                "color": [
                    "#eee"
                ]
            }
        },
        "splitArea": {
            "show": false,
            "areaStyle": {
                "color": [
                    "rgba(255,255,255,0.01)",
                    "rgba(0,0,0,0.01)"
                ]
            }
        }
    },
    "valueAxis": {
        "axisLine": {
            "show": false,
            "lineStyle": {
                "color": "#999"
            }
        },
        "splitLine": {
            "show": true,
            "lineStyle": {
                "color": "rgba(128,128,128,0.2)"
            }
        }
    }
}

export default T
