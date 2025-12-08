// frontend/blog-frontend/tailwind.config.js
import designSystemPreset from '@portal/design-system/tailwind.preset.js';

console.log('🔥🔥🔥 [DEBUG] Blog Tailwind Config Loaded! 🔥🔥🔥');

/** @type {import('tailwindcss').Config} */
export default {
    presets: [designSystemPreset],
    content: [
        "./index.html",
        "./src/**/*.{vue,js,ts,jsx,tsx}",
    ],
    plugins: [],
}