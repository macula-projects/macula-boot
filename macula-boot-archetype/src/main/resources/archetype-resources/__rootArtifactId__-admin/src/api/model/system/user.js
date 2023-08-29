import config from "@/config"
import http from "@/utils/request"

export default {
    user: {
        list: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/users`,
            name: "获取用户列表",
            get: async function (params) {
                return await http.get(this.url, params);
            }
        },
        add: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/users`,
            name: "添加用户",
            post: async function (data = {}) {
                return await http.post(this.url, data)
            }
        },
        edit: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/users`,
            name: "编辑用户",
            put: async function (data = {}, id) {
                var reqUrl = this.url + '/' + id
                return await http.put(reqUrl, data)
            }
        },
        del: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/users`,
            name: "删除用户",
            delete: async function (data = {}) {
                var reqUrl = this.url + '/' + data
                return await http.delete(reqUrl, data)
            }
        },
        resetPassword: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/users`,
            name: "修改密码",
            patch: async function (data = {}) {
                var reqUrl = this.url + '/' + data.id + '/password?password=' + data.password
                //let param = {"password":data.password}
                return await http.patch(reqUrl, data)
            }
        },
        listByIds: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/users/getUsers`,
            name: "根据id获取用户列表",
            get: async function (params) {
                return await http.get(this.url, params);
            }
        },
    }
}
