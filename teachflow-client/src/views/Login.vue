<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <h2>教学流程分析系统</h2>
        </div>
      </template>

      <el-form :model="loginForm" :rules="rules" ref="loginFormRef" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input
              v-model="loginForm.username"
              placeholder="请输入账号"
              size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
              type="primary"
              style="width: 100%; margin-top: 10px;"
              size="large"
              :loading="loading"
              @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
        <div style="text-align: center; margin-top: 15px; font-size: 14px;">
          还没有账号？<el-link type="primary" @click="router.push('/register')">立即注册</el-link>
        </div>
      </el-form>
    </el-card>
  </div>

</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth' // 【必须引入】

const router = useRouter()
const authStore = useAuthStore() // 初始化 Store
const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })
const rules = { /* ...保持不变... */ }

const handleLogin = () => {
  loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 【核心修改】调用 store 的 login 方法，它会处理好所有存取逻辑
        await authStore.login(loginForm)

        ElMessage.success('登录成功')
        router.push('/dashboard') // 只有这里执行后，路由守卫才会放行
      } catch (error) {
        // 这里的报错会被 request.js 拦截器处理，也可以自行提示
        ElMessage.error(error.response?.data?.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f0f2f5;
}

.login-card {
  width: 400px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
  text-align: center;
  margin: 0;
  color: #303133;
}
</style>