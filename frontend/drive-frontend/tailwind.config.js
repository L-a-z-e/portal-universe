import designSystemPreset from '@portal/design-tokens/tailwind';

/** @type {import('tailwindcss').Config} */
export default {
    presets: [designSystemPreset],
    content: [
        "./index.html",
        "./src/**/*.{vue,js,ts,jsx,tsx}",
    ],
    plugins: [],
}
