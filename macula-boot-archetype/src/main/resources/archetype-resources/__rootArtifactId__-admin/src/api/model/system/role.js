import config from "@/config"
import http from "@/utils/request"

export default {
    role: {
        list: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/roles/pages`,
            name: '获取角色分布列表',
            get: async function (param) {
                return await http.get(this.url, param)
            }
        },
        post: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/roles`,
            name: '添加角色',
            post: async function (data) {
                return await http.post(this.url, data)
            }
        },
        del: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/roles`,
            name: '删除角色',
            delete: async function (ids) {
                return await http.delete(`${this.url}/${ids}`)
            }
        },
        options: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/roles/options`,
            name: "获取角色下拉选项",
            get: async function () {
                var res = await http.get(this.url);
                return res;
            }
        },
        validtorRoleCode: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/roles/validtor/code`,
            name: '角色编码值合法性验证',
            get: async function (params) {
                return await http.get(this.url, params)
            }
        },
        validtorRoleName: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/roles/validtor/name`,
            name: '角色名字值合法性验证',
            get: async function (params) {
                return await http.get(this.url, params)
            }
        },
        optionsByDataScope: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/roles/optionsByDataScope`,
            name: '获取角色数据权限下拉选项',
            get: async function () {
                return await http.get(this.url)
            }
        },
        getRoleMenuIds: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/roles`,
            name: '获取角色的菜单ID集合',
            get: async function (roleId, params) {
                return await http.get(`${this.url}/${roleId}/menuIds`, params)
            }
        },
        updateRoleMenus: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/roles`,
            name: '分配角色的资源菜单权限',
            put: async function (roleId, data) {
                return await http.put(`${this.url}/${roleId}/menus`, data)
            }
        }
    }
}