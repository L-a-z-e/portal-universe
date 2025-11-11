/** @type {import('tailwindcss').Config} */
export default {
    content: [
        './index.html',
        './src/**/*.{vue,js,ts,jsx,tsx}',
        '../portal-shell/src/**/*.{vue,js,ts,jsx,tsx}',
        '../blog-frontend/src/**/*.{vue,js,ts,jsx,tsx}',
        '../shopping-frontend/src/**/*.{vue,js,ts,jsx,tsx}'
    ],

    darkMode: ['class', '[data-theme="dark"]'],

    theme: {
        extend: {
            // Colors - Semantic tokens (service-agnostic)
            colors: {
                // Brand
                'brand': {
                    'primary': 'var(--color-brand-primary)',
                    'primary-hover': 'var(--color-brand-primary-hover)',
                    'secondary': 'var(--color-brand-secondary)',
                },

                // Text
                'text': {
                    'heading': 'var(--color-text-heading)',
                    'body': 'var(--color-text-body)',
                    'meta': 'var(--color-text-meta)',
                    'muted': 'var(--color-text-muted)',
                    'inverse': 'var(--color-text-inverse)',
                    'link': 'var(--color-text-link)',
                    'link-hover': 'var(--color-text-link-hover)',
                },

                // Background
                'bg': {
                    'page': 'var(--color-bg-page)',
                    'card': 'var(--color-bg-card)',
                    'elevated': 'var(--color-bg-elevated)',
                    'muted': 'var(--color-bg-muted)',
                    'hover': 'var(--color-bg-hover)',
                },

                // Border
                'border': {
                    'default': 'var(--color-border-default)',
                    'hover': 'var(--color-border-hover)',
                    'focus': 'var(--color-border-focus)',
                    'muted': 'var(--color-border-muted)',
                },

                // Status
                'status': {
                    'success': 'var(--color-status-success)',
                    'success-bg': 'var(--color-status-success-bg)',
                    'error': 'var(--color-status-error)',
                    'error-bg': 'var(--color-status-error-bg)',
                    'warning': 'var(--color-status-warning)',
                    'warning-bg': 'var(--color-status-warning-bg)',
                    'info': 'var(--color-status-info)',
                    'info-bg': 'var(--color-status-info-bg)',
                },
            },

            // Typography
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

            // Spacing
            spacing: {
                'xs': 'var(--spacing-xs)',
                'sm': 'var(--spacing-sm)',
                'md': 'var(--spacing-md)',
                'lg': 'var(--spacing-lg)',
                'xl': 'var(--spacing-xl)',
                '2xl': 'var(--spacing-2xl)',
            },

            // Border Radius
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

            // Box Shadow
            boxShadow: {
                'sm': 'var(--shadow-sm)',
                'DEFAULT': 'var(--shadow-md)',
                'md': 'var(--shadow-md)',
                'lg': 'var(--shadow-lg)',
                'xl': 'var(--shadow-xl)',
            },

            // Max Width (for content containers)
            maxWidth: {
                'content': '768px',   // Blog posts
                'container': '1280px', // General layout
            },
        }
    },

    plugins: [
        // Tailwind Typography for blog posts
        require('@tailwindcss/typography'),
    ]
}