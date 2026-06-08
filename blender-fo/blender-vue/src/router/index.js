import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Modeling from '../views/Modeling.vue'
import Assets from '../views/Assets.vue'
import Plugins from '../views/Plugins.vue'
import Manage from '../views/Manage.vue'

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/modeling', name: 'Modeling', component: Modeling },
  { path: '/assets', name: 'Assets', component: Assets },
  { path: '/plugins', name: 'Plugins', component: Plugins },
  { path: '/manage', name: 'Manage', component: Manage }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
