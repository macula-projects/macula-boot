import config from "@/config"
import http from "@/utils/request"

export default {
    menu: {
        add: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/menus`,
            name: '添加菜单',
            post: async function (data) {
                return await http.post(this.url, data)
            }
        },
        del: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/menus`,
            name: '删除菜单',
            delete: async function (ids, params) {
                return await http.delete(`${this.url}/${ids}`, params)
            }
        },
        methodOption: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/menus/methodOption`,
            name: '获取请求方法下拉列表',
            get: async function () {
                return await http.get(this.url)
            }
        },
        list: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/menus`,
            name: '获取菜单列表',
            get: async function (params) {
                return await http.get(this.url, params)
            }
        },
        routes: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/menus/routes`,
            name: '路由列表',
            get: async function (params) {
                return await http.get(this.url, params)
            }
        }
    }
}
