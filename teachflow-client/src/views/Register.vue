<template>
  <div class="auth-container">
    <el-card class="auth-card">
      <template #header><h2>用户注册</h2></template>
      <el-form :model="regForm" :rules="rules" ref="regFormRef">
        <el-form-item prop="username">
          <el-input v-model="regForm.username" placeholder="设置账号" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="regForm.password" type="password" placeholder="设置密码" size="large" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="regForm.confirmPassword" type="password" placeholder="确认密码" size="large" show-password />
        </el-form-item>
        <el-button type="primary" class="full-width" :loading="loading" @click="handleRegister">立即注册</el-button>
        <div class="footer-link">
          已有账号？<el-link type="primary" @click="router.push('/login')">去登录</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const regFormRef = ref(null)
const loading = ref(false)

const regForm = reactive({ username: '', password: '', confirmPassword: '' })

// 校验逻辑：两次密码必须一致
const validatePass2 = (rule, value, callback) => {
  if (value !== regForm.password) callback(new Error('两次输入密码不一致!'))
  else callback()
}

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validatePass2, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  const valid = await regFormRef.value.validate()
  if (!valid) return

  loading.value = true
  try {
    await register({ username: regForm.username, password: regForm.password })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    // 拦截器已经报过错了
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-container { display: flex; justify-content: center; align-items: center; height: 100vh; background: #f5f7fa; }
.auth-card { width: 400px; }
.full-width { width: 100%; }
.footer-link { text-align: center; margin-top: 15px; font-size: 14px; }
</style>