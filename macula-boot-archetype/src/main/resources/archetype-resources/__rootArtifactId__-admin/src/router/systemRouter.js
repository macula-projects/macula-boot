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

import config from "@/config"

//系统路由
const routes = [
    {
        name: "layout",
        path: "/",
        component: () => import(/* webpackChunkName: "layout" */ '@/layout'),
        redirect: config.DASHBOARD_URL || '/dashboard',
        children: []
    },
    {
        path: "/login",
        component: () => import(/* webpackChunkName: "login" */ '@/views/common/login'),
        meta: {
            title: "登录"
        }
    },
    {
        path: "/user_register",
        component: () => import(/* webpackChunkName: "userRegister" */ '@/views/common/login/userRegister'),
        meta: {
            title: "注册"
        }
    },
    {
        path: "/reset_password",
        component: () => import(/* webpackChunkName: "resetPassword" */ '@/views/common/login/resetPassword'),
        meta: {
            title: "重置密码"
        }
    }
]

export default routes;
