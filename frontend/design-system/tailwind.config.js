// packages/design-system/tailwind.config.js
import preset from './tailwind.preset.js';

/** @type {import('tailwindcss').Config} */
export default {
    presets: [preset],
    content: [
        './src/**/*.{vue,js,ts,jsx,tsx}',
    ],
}