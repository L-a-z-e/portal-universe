import { createApp } from 'vue'
import { createPinia } from "pinia";
import './style.css'
import App from './App.vue'
import router from './router'
import {useAuthStore} from "./store/auth.ts";
import userManager from "./services/authService.ts";

const app = createApp(App);
const pinia = createPinia();

app.use(router);
app.use(pinia);
app.mount('#app');

const authStore = useAuthStore();

userManager.getUser().then(user => {
  if (user && user.access_token) {
    authStore.login(user.access_token);
  }
});