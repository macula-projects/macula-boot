import sysConfig from '@/config'
import {defineStore} from "pinia"
import tool from '@/utils/tool'

function getToolDataTenantOptions() {
    if (tool.data.get('tenantStoreTenantOptions')) {
        return JSON.parse(tool.data.get('tenantStoreTenantOptions'))
    }
    return null
}

export const useTenantStore = defineStore({
    id: 'tenant',
    state: () => ({
        tenantOptions: getToolDataTenantOptions() || [],
        tenantId: tool.data.get(sysConfig.TENANT_ID) || null,
        tenantLabel: tool.data.get('tenantLabel') || ''
    }),
    actions: {
        pushTenantOptions(tenantOptions) {
            if (tenantOptions && tenantOptions instanceof Array) {
                tenantOptions.forEach(item => {
                    this.tenantOptions.push(item)
                })
                tool.data.set('tenantStoreTenantOptions', JSON.stringify(this.tenantOptions))
            }
        },
        updateTenantId(tenantId) {
            this.tenantId = tenantId
            tool.data.set(sysConfig.TENANT_ID, this.tenantId)
        },
        updateTenantLabel(tenantLabel) {
            this.tenantLabel = tenantLabel
            tool.data.set('tenantLabel', this.tenantLabel)
        },
        clearTenantOptions() {
            this.tenantOptions = []
            tool.data.remove('tenantStoreTenantOptions')
        }
    }
})