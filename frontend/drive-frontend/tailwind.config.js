import designSystemPreset from '@portal/design-core/tailwind';

/** @type {import('tailwindcss').Config} */
export default {
    presets: [designSystemPreset],
    content: [
        "./index.html",
        "./src/**/*.{vue,js,ts,jsx,tsx}",
        "../design-core/src/variants/**/*.ts",
        "../design-core/src/styles/**/*.css",
        "../design-vue/src/**/*.{vue,js,ts}",
    ],
    plugins: [],
}
