/// <reference types="vite/client" />
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import federation from '@originjs/vite-plugin-federation';
import { resolve } from 'path';
import path from 'path';
export default defineConfig(function (_a) {
    var mode = _a.mode;
    var env = loadEnv(mode, process.cwd(), '');
    var DEFAULT_REMOTES = {
        portal: 'http://localhost:30000/assets/shellEntry.js',
    };
    return {
        base: env.VITE_BASE_URL,
        plugins: [
            react(),
            federation({
                name: 'shopping-seller-frontend',
                filename: 'remoteEntry.js',
                remotes: {
                    portal: env.VITE_PORTAL_SHELL_REMOTE_URL || DEFAULT_REMOTES.portal,
                },
                exposes: {
                    './bootstrap': './src/bootstrap.tsx'
                },
                shared: ['react', 'react-dom', 'react-dom/client', 'axios', 'react-router-dom'],
            }),
        ],
        resolve: {
            alias: {
                '@portal/design-react/style.css': resolve(__dirname, '../design-react/src/styles/index.css'),
                '@portal/design-react': resolve(__dirname, '../design-react/src/index.ts'),
                '@': path.resolve(__dirname, './src'),
                '@components': path.resolve(__dirname, './src/components'),
                '@pages': path.resolve(__dirname, './src/pages'),
                '@stores': path.resolve(__dirname, './src/stores'),
                '@hooks': path.resolve(__dirname, './src/hooks'),
                '@types': path.resolve(__dirname, './src/types'),
            },
        },
        css: { postcss: './postcss.config.js' },
        server: { port: 30006, host: '0.0.0.0', cors: true, open: false },
        preview: { port: 30006, host: '0.0.0.0', cors: true, open: false },
        build: { target: 'esnext', minify: 'esbuild', cssCodeSplit: true, sourcemap: false, outDir: 'dist' },
        esbuild: mode === 'production' ? { pure: ['console.log', 'console.debug', 'console.group', 'console.groupEnd'] } : undefined,
    };
});
