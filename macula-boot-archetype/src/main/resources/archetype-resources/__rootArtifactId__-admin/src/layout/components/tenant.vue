<template>
  <el-dropdown class="user panel-item" trigger="click" @command="changeTenant">
    <div class="user-avatar">
      <label>{{ tenantLabel }}</label>
      <el-icon v-if="tenantOptions.length>1" class="el-icon--right">
        <el-icon-arrow-down/>
      </el-icon>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item v-for="(tenantOption, index) in tenantOptions" :key="index" :command="tenantOption"
                          :divided="0 != index">{{ tenantOption.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script>
import tenant from './tenant'
import {useTenantStore} from '@/stores/tenant';
import {mapState, mapActions} from 'pinia';

export default {
  data() {
    return {
      userName: ''
    }
  },
  computed: {
    ...mapState(useTenantStore, ['tenantOptions', 'tenantLabel'])
  },
  methods: {
    ...mapActions(useTenantStore, ['updateTenantId', 'updateTenantLabel']),
    changeTenant(tenantOption) {
      this.updateTenantId(tenantOption.value)
      this.updateTenantLabel(tenantOption.label)
      let reg = /\#reloaded$/
      if (!reg.test(location.href)) {
        location.href = location.href + "#reloaded"
      } else {
        location.href = location.href.replace(reg, '')
      }
    }
  }
}
</script>

<style scoped>
.user-bar {
  display: flex;
  align-items: center;
  height: 100%;
}

.user-bar .panel-item {
  padding: 0 10px;
  cursor: pointer;
  height: 100%;
  display: flex;
  align-items: center;
}

.user-bar .panel-item i {
  font-size: 16px;
}

.user-bar .panel-item:hover {
  background: rgba(0, 0, 0, 0.1);
}

.user-bar .user-avatar {
  height: 49px;
  display: flex;
  align-items: center;
}

.user-bar .user-avatar label {
  display: inline-block;
  margin-left: 5px;
  font-size: 12px;
  cursor: pointer;
}
</style>
