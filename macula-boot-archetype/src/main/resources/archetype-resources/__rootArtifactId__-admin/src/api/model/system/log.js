import config from "@/config"
import http from "@/utils/request"

export default {
    log: {
        list: {
            url: `${config.API_URL}/${config.MODEL.system}/api/v1/audit/log/page`,
            name: "日志列表",
            get: async function (params) {
                return await http.get(this.url, params);
            }
        }
    },
}
