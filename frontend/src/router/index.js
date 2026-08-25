import { createRouter, createWebHashHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import Dashboard from '../views/Dashboard.vue'
import Members from '../views/Members.vue'
import Overtime from '../views/Overtime.vue'
import CompUsage from '../views/CompUsage.vue'
import OtherComp from '../views/OtherComp.vue'
import Balance from '../views/Balance.vue'
import Employee from '../views/Employee.vue'
import Messages from '../views/Messages.vue'
import Settings from '../views/Settings.vue'

const routes = [
  { path: '/login', component: Login },
  {
    path: '/',
    component: Layout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'dashboard', component: Dashboard },
      { path: 'members', name: 'members', component: Members },
      { path: 'overtime', name: 'overtime', component: Overtime },
      { path: 'comp-usage', name: 'comp-usage', component: CompUsage },
      { path: 'other-comp', name: 'other-comp', component: OtherComp },
      { path: 'balance', name: 'balance', component: Balance },
      { path: 'employee', name: 'employee', component: Employee },
      { path: 'messages', name: 'messages', component: Messages },
      { path: 'settings', name: 'settings', component: Settings }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  if (to.path === '/login') return true
  if (!localStorage.getItem('token')) return '/login'

  // 角色可见性判断
  const adminViews = ['dashboard', 'members', 'overtime', 'comp-usage', 'other-comp', 'balance', 'settings']
  const employeeOnly = ['employee']
  if (user && user.role === 'EMPLOYEE' && adminViews.includes(to.name)) {
    ElMessage.error('员工账号仅可访问个人门户')
    return '/employee'
  }
  if (user && user.role !== 'EMPLOYEE' && employeeOnly.includes(to.name)) {
    return '/dashboard'
  }
  return true
})

export default router
