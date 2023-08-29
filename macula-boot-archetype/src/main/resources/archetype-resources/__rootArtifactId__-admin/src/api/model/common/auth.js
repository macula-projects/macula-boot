import config from "@/config"
import http from "@/utils/request"

export default {
    systemToken: {
        url: `${config.API_URL}/${config.MODEL.system}/api/token`,
        name: "macula V5 system提供隐式获取登录token接口",
        post: async function (data = {}) {
            return await http.post(this.url, data)
        }
    },
    getUserInfo: {
        url: `${config.API_URL}/${config.MODEL.system}/api/token/userInfo`,
        name: "macula V5 system提供获取当前登录用户信息接口",
        get: async function () {
            return await http.get(this.url)
        }
    }
}
