/** @type {import('tailwindcss').Config} */
export default {
    content: [
        './index.html',
        './src/**/*.{vue,js,ts,jsx,tsx}',
        '../portal-shell/src/**/*.{vue,js,ts,jsx,tsx}',
        '../blog-frontend/src/**/*.{vue,js,ts,jsx,tsx}'
    ],
    darkMode: ['class', '[data-theme="dark"]'],
    theme: {
        extend: {
            colors: {
                'text': {
                    'primary': 'var(--color-text-primary)',
                    'secondary': 'var(--color-text-secondary)',
                    'inverse': 'var(--color-text-inverse)'
                },
                'bg': {
                    'primary': 'var(--color-bg-primary)',
                    'secondary': 'var(--color-bg-secondary)',
                    'tertiary': 'var(--color-bg-tertiary)'
                },
                'border': {
                    'default': 'var(--color-border-default)',
                    'hover': 'var(--color-border-hover)'
                },
                'interactive': {
                    'default': 'var(--color-interactive-default)',
                    'hover': 'var(--color-interactive-hover)'
                }
            },
            spacing: {
                'xs': 'var(--spacing-xs)',
                'sm': 'var(--spacing-sm)',
                'md': 'var(--spacing-md)',
                'lg': 'var(--spacing-lg)',
                'xl': 'var(--spacing-xl)',
                '2xl': 'var(--spacing-2xl)'
            },
            fontSize: {
                'xs': 'var(--font-size-xs)',
                'sm': 'var(--font-size-sm)',
                'base': 'var(--font-size-base)',
                'lg': 'var(--font-size-lg)',
                'xl': 'var(--font-size-xl)',
                '2xl': 'var(--font-size-2xl)',
                '3xl': 'var(--font-size-3xl)'
            },
            fontWeight: {
                'normal': 'var(--font-weight-normal)',
                'medium': 'var(--font-weight-medium)',
                'semibold': 'var(--font-weight-semibold)',
                'bold': 'var(--font-weight-bold)'
            },
            fontFamily: {
                'sans': 'var(--font-family-sans)',
                'mono': 'var(--font-family-mono)'
            },
            borderRadius: {
                'none': 'var(--border-radius-none)',
                'sm': 'var(--border-radius-sm)',
                'DEFAULT': 'var(--border-radius-default)',
                'md': 'var(--border-radius-md)',
                'lg': 'var(--border-radius-lg)',
                'full': 'var(--border-radius-full)'
            }
        }
    },
    plugins: []
}
