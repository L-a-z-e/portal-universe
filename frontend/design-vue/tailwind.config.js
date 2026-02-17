// packages/design-system/tailwind.config.js
import designSystemPreset from '@portal/design-core/tailwind';

/** @type {import('tailwindcss').Config} */
export default {
    presets: [designSystemPreset],
    content: [
        './src/**/*.{vue,js,ts,jsx,tsx}',
        '../design-core/src/variants/**/*.ts',
        '../design-core/src/styles/**/*.css',
    ],
}