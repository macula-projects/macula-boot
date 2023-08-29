import config from "@/config"
import http from "@/utils/request"

export default {
    dept: {
        reqUrl: "",
        list: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/dept`,
            name: "部门列表",
            get: async function (params) {
                return await http.get(this.url, params);
            }
        },
        options: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/dept/options`,
            name: "获取部门下拉选项",
            get: async function () {
                var res = await http.get(this.url);
                return res;
            }
        },
        add: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/dept`,
            name: "添加部门",
            post: async function (data = {}) {
                return await http.post(this.url, data)
            }
        },
        edit: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/dept`,
            name: "编辑部门",
            put: async function (data = {}, id) {
                var reqUrl = this.url + '/' + id
                return await http.put(reqUrl, data)
            }
        },
        del: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/dept`,
            name: "删除部门",
            delete: async function (data = {}) {
                var reqUrl = this.url + '/' + data
                return await http.delete(reqUrl, data)
            }
        }
    },
}
