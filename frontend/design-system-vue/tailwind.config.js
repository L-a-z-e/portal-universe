// packages/design-system/tailwind.config.js
import designSystemPreset from '@portal/design-tokens/tailwind';

/** @type {import('tailwindcss').Config} */
export default {
    presets: [designSystemPreset],
    content: [
        './src/**/*.{vue,js,ts,jsx,tsx}',
    ],
}