import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import { createBlogRouter } from './router';

const app = createApp(App);
const router = createBlogRouter();
app.use(router);
app.mount('#app');
