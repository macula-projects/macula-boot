import config from "@/config"
import http from "@/utils/request"

export default {
    systemToken: {
        url: `${config.IAM_URL}/oauth2/token`,
        name: "macula V5 system提供隐式获取登录token接口",
        post: async function (data = {}, config = {}) {
            return await http.post(this.url, data, config)
        }
    },
    getUserInfo: {
        url: `${config.API_URL}/${config.MODEL.admin}/api/v1/users/me`,
        name: "macula V5 system提供获取当前登录用户信息接口",
        get: async function () {
            return await http.get(this.url)
        }
    },
    getRoutes: {
        url: `${config.API_URL}/${config.MODEL.admin}/api/v1/menus/routes`,
        name: "macula V5 system提供获取当前菜单接口",
        get: async function () {
            return await http.get(this.url)
        }
    }
}
