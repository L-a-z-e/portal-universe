import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from "@originjs/vite-plugin-federation";

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    federation({
      name: 'portal_shell_host',
      remotes: {
        'blog_remote': 'http://localhost:5082/assets/remoteEntry.js',
      },
      shared: ['vue']
    })
  ],
  base: './',
  server: {
    port: 50000,
    cors: true
  },
  build: {
    target: 'esnext'
  }
})
