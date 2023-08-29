import config from "@/config"
import http from "@/utils/request"

export default {
    application: {
        listPages: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/app`,
            name: "应用分页列表",
            get: async function (params) {
                return await http.get(this.url, params);
            }
        },
        add: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/app`,
            name: "添加应用",
            post: async function (data = {}) {
                return await http.post(this.url, data)
            }
        },
        edit: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/app`,
            name: "编辑应用",
            put: async function (data = {}, id) {
                var reqUrl = this.url + '/' + id
                return await http.put(reqUrl, data)
            }
        },
        del: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/app`,
            name: "删除应用",
            delete: async function (data = {}) {
                var reqUrl = this.url + '/' + data
                return await http.delete(reqUrl, data)
            }
        },
        addMaintainer: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/app/addMaintainer`,
            name: "添加维护人",
            put: async function (data = {}, id) {
                var reqUrl = this.url + '/' + id
                return await http.put(reqUrl, data)
            }
        },
    },
}
