<script setup lang="ts">

import {useAuthStore} from "./store/auth.ts";
import { login, logout } from "./services/authService.ts";

const authStore = useAuthStore();

</script>

<template>
  <div id="portal-shell-container">
    <header>
      <h1>Portal Universe</h1>
      <nav>
        <router-link to="/">Home</router-link> |
        <router-link to="/blog">Blog</router-link> |
<!--        <router-link to="/shopping">Shopping</router-link>-->
      </nav>
      <div class="auth-status">
        <template v-if="authStore.isAuthenticated">
          <span>Welcome, {{ authStore.user?.name }}!</span>
          <button @click="logout">Logout</button>
        </template>
        <template v-else>
          <button @click="login">Login</button>
        </template>
      </div>
    </header>

    <main>
      <Suspense>
        <template #default>
          <router-view :key="$route.path" v-slot="{ Component }">
            <component :is="Component" />
          </router-view>
        </template>
        <template #fallback>
          Loading Page
        </template>
      </Suspense>
    </main>

    <footer>
      <p>Portal Universe</p>
    </footer>
  </div>
</template>

<style scoped>
#portal-shell-container {
  max-width: 1200px;
  margin: 0 auto;
  font-family: sans-serif;
}

header {
  padding: 1rem;
  border-bottom: 1px solid #ccc;
}

nav a {
  margin: 0 1rem;
  font-weight: bold;
  text-decoration: none;
}

nav a.router-link-exact-active {
  color: #42b983;
}

main {
  padding: 1rem;
}

</style>