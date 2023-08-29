import config from '@/config'
import http from '@/utils/request'

export default {
    tenant: {
        pages: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/tenants`,
            name: '获取租户管理列表',
            get: async function (params) {
                return await http.get(this.url, params)
            }
        },
        add: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/tenants`,
            name: '新增，保存租户信息',
            post: async function (data) {
                return await http.post(this.url, data)
            }
        },
        edit: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/tenants`,
            name: '更新租户信息',
            put: async function (id, data) {
                return await http.put(`${this.url}/${id}`, data)
            }
        },
        del: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/tenants`,
            name: '根据id删除租户信息',
            delete: async function (id) {
                return await http.delete(`${this.url}/${id}`)
            }
        },
        options: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/tenants/options`,
            name: '获取租户下拉选项',
            get: async function (params) {
                return await http.get(this.url, params)
            }
        }
    }
}