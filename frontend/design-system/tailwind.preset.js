// packages/design-system/tailwind.preset.js

/** @type {import('tailwindcss').Config} */
export default {
    darkMode: ['class', '[data-theme="dark"]'],

    theme: {
        extend: {
            // 1. Colors - Semantic tokens (CHANGED: --color- → --semantic-)
            colors: {
                'brand': {
                    'primary': 'var(--semantic-brand-primary)',
                    'primaryHover': 'var(--semantic-brand-primaryHover)',
                    'secondary': 'var(--semantic-brand-secondary)',
                },
                'text': {
                    'heading': 'var(--semantic-text-heading)',
                    'body': 'var(--semantic-text-body)',
                    'meta': 'var(--semantic-text-meta)',
                    'muted': 'var(--semantic-text-muted)',
                    'inverse': 'var(--semantic-text-inverse)',
                    'link': 'var(--semantic-text-link)',
                    'linkHover': 'var(--semantic-text-linkHover)',
                },
                'bg': {
                    'page': 'var(--semantic-bg-page)',
                    'card': 'var(--semantic-bg-card)',
                    'elevated': 'var(--semantic-bg-elevated)',
                    'muted': 'var(--semantic-bg-muted)',
                    'hover': 'var(--semantic-bg-hover)',
                },
                'border': {
                    'default': 'var(--semantic-border-default)',
                    'hover': 'var(--semantic-border-hover)',
                    'focus': 'var(--semantic-border-focus)',
                    'muted': 'var(--semantic-border-muted)',
                },
                'status': {
                    'success': 'var(--semantic-status-success)',
                    'successBg': 'var(--semantic-status-successBg)',
                    'error': 'var(--semantic-status-error)',
                    'errorBg': 'var(--semantic-status-errorBg)',
                    'warning': 'var(--semantic-status-warning)',
                    'warningBg': 'var(--semantic-status-warningBg)',
                    'info': 'var(--semantic-status-info)',
                    'infoBg': 'var(--semantic-status-infoBg)',
                },
            },

            // 2. Typography (폰트, 사이즈, 두께, 행간)
            fontFamily: {
                'sans': 'var(--font-family-sans)',
                'mono': 'var(--font-family-mono)',
            },
            fontSize: {
                'xs': 'var(--font-size-xs)',
                'sm': 'var(--font-size-sm)',
                'base': 'var(--font-size-base)',
                'lg': 'var(--font-size-lg)',
                'xl': 'var(--font-size-xl)',
                '2xl': 'var(--font-size-2xl)',
                '3xl': 'var(--font-size-3xl)',
                '4xl': 'var(--font-size-4xl)',
                '5xl': 'var(--font-size-5xl)',
            },
            lineHeight: {
                'tight': 'var(--line-height-tight)',
                'snug': 'var(--line-height-snug)',
                'normal': 'var(--line-height-normal)',
                'relaxed': 'var(--line-height-relaxed)',
                'loose': 'var(--line-height-loose)',
            },
            fontWeight: {
                'light': 'var(--font-weight-light)',
                'normal': 'var(--font-weight-normal)',
                'medium': 'var(--font-weight-medium)',
                'semibold': 'var(--font-weight-semibold)',
                'bold': 'var(--font-weight-bold)',
            },

            // 3. Layout & Effects (간격, 둥글기, 그림자)
            spacing: {
                'xs': 'var(--spacing-xs)',
                'sm': 'var(--spacing-sm)',
                'md': 'var(--spacing-md)',
                'lg': 'var(--spacing-lg)',
                'xl': 'var(--spacing-xl)',
                '2xl': 'var(--spacing-2xl)',
            },
            borderRadius: {
                'none': 'var(--border-radius-none)',
                'sm': 'var(--border-radius-sm)',
                'DEFAULT': 'var(--border-radius-default)',
                'md': 'var(--border-radius-md)',
                'lg': 'var(--border-radius-lg)',
                'xl': 'var(--border-radius-xl)',
                '2xl': 'var(--border-radius-2xl)',
                'full': 'var(--border-radius-full)',
            },
            boxShadow: {
                'sm': 'var(--shadow-sm)',
                'DEFAULT': 'var(--shadow-md)',
                'md': 'var(--shadow-md)',
                'lg': 'var(--shadow-lg)',
                'xl': 'var(--shadow-xl)',
            },
            maxWidth: {
                'content': '768px',
                'container': '1280px',
            },
        }
    },

    plugins: [
        require('@tailwindcss/typography'),
    ]
}
