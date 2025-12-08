import { readFileSync, writeFileSync } from 'fs';
import { join, resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const tokensDir = resolve(__dirname, '../src/tokens');
const outputFile = resolve(__dirname, '../src/styles/index.css');

/**
 * 토큰 값을 재귀적으로 CSS 변수로 변환
 */
function flattenTokens(tokens, prefix, cssVars, parentKey = '') {
    for (const [key, value] of Object.entries(tokens)) {
        if (key.startsWith('$')) continue;

        const fullKey = parentKey ? `${parentKey}-${key}` : key;
        const cssVarName = prefix ? `${prefix}-${fullKey}` : `--${fullKey}`;

        if (typeof value === 'object' && value !== null && !('$value' in value)) {
            flattenTokens(value, prefix, cssVars, fullKey);
        } else if (value && typeof value === 'object' && '$value' in value) {
            cssVars.set(cssVarName, value.$value);
        } else if (typeof value === 'string' || typeof value === 'number') {
            cssVars.set(cssVarName, value);
        }
    }
}

/**
 * 색상 참조 {color.xxx.yyy} 형식을 실제 값으로 해결
 */
function resolveColorReference(value, colorReferences) {
    if (typeof value !== 'string') return value;

    const refMatch = value.match(/^\{([^}]+)\}$/);
    if (!refMatch) return value;

    const refPath = refMatch[1];
    const resolved = colorReferences[refPath];

    if (resolved) {
        return resolved;
    } else {
        return value;
    }
}

/**
 * 토큰 파일들을 읽고 CSS 변수로 변환
 */
function buildTokens() {
    const cssVariables = new Map();
    const themes = new Map();
    const colorReferences = {};
    const unresolvedReferences = new Set();

    try {
        console.log('📖 Step 1: Building color reference map...');

        try {
            const colorPath = join(tokensDir, 'base', 'colors.json');
            const colorContent = readFileSync(colorPath, 'utf-8');
            const colorTokens = JSON.parse(colorContent);

            function buildColorMap(obj, prefix = '') {
                for (const [key, value] of Object.entries(obj)) {
                    if (key.startsWith('$')) continue;

                    const fullKey = prefix ? `${prefix}.${key}` : key;

                    if (typeof value === 'object' && value !== null && !('$value' in value)) {
                        buildColorMap(value, fullKey);
                    } else if (value && typeof value === 'object' && '$value' in value) {
                        colorReferences[fullKey] = value.$value;
                    } else if (typeof value === 'string') {
                        colorReferences[fullKey] = value;
                    }
                }
            }

            buildColorMap(colorTokens.color || colorTokens, 'color');

            console.log(`  ✅ Color reference map built (${Object.keys(colorReferences).length} colors)`);

            const colorRefSample = Object.entries(colorReferences).slice(0, 5);
            colorRefSample.forEach(([key, val]) => {
                console.log(`     • ${key}: ${val}`);
            });
        } catch (err) {
            console.log(`  ⚠️  Error building color reference map: ${err.message}`);
        }

        console.log('📖 Step 2: Reading base tokens...');

        ['colors', 'typography', 'spacing', 'border'].forEach(tokenType => {
            const filePath = join(tokensDir, 'base', `${tokenType}.json`);
            try {
                const content = readFileSync(filePath, 'utf-8');
                const tokens = JSON.parse(content);

                let prefix = '--token';
                let tokenContent = tokens;

                if (tokenType === 'colors') {
                    prefix = '--color';
                    tokenContent = tokens.color || tokens;
                } else if (tokenType === 'typography') {
                    prefix = '--typography';
                    tokenContent = tokens.typography || tokens;
                } else if (tokenType === 'spacing') {
                    prefix = '--spacing';
                    tokenContent = tokens.spacing || tokens;
                } else if (tokenType === 'border') {
                    prefix = '--border';
                    tokenContent = tokens.border || tokens;
                }

                flattenTokens(tokenContent, prefix, cssVariables);
                console.log(`  ✅ ${tokenType}.json loaded`);
            } catch (err) {
                console.log(`  ⚠️  ${tokenType}.json not found or invalid`);
            }
        });

        console.log('📖 Step 3: Reading semantic tokens...');
        try {
            const semanticPath = join(tokensDir, 'semantic', 'colors.json');
            const content = readFileSync(semanticPath, 'utf-8');
            const semanticTokens = JSON.parse(content);

            let semanticContent = semanticTokens;
            if (semanticTokens.semantic?.colors) {
                semanticContent = semanticTokens.semantic.colors;
            } else if (semanticTokens.color) {
                semanticContent = semanticTokens.color;
            } else if (semanticTokens.colors) {
                semanticContent = semanticTokens.colors;
            }

            function processSemantic(obj, prefix = '--semantic', parentKey = '') {
                for (const [key, value] of Object.entries(obj)) {
                    if (key.startsWith('$')) continue;

                    const fullKey = parentKey ? `${parentKey}-${key}` : key;
                    const varName = `${prefix}-${fullKey}`;

                    if (typeof value === 'object' && value !== null && !('$value' in value)) {
                        processSemantic(value, prefix, fullKey);
                    } else if (value && typeof value === 'object' && '$value' in value) {
                        const resolved = resolveColorReference(value.$value, colorReferences);

                        if (resolved.startsWith('{')) {
                            const refPath = resolved.match(/^\{([^}]+)\}$/)?.[1];
                            if (refPath) unresolvedReferences.add(refPath);
                        }

                        cssVariables.set(varName, resolved);
                    } else if (typeof value === 'string') {
                        const resolved = resolveColorReference(value, colorReferences);

                        if (resolved.startsWith('{')) {
                            const refPath = resolved.match(/^\{([^}]+)\}$/)?.[1];
                            if (refPath) unresolvedReferences.add(refPath);
                        }

                        cssVariables.set(varName, resolved);
                    }
                }
            }

            processSemantic(semanticContent);
            console.log('  ✅ semantic/colors.json loaded');
        } catch (err) {
            console.log(`  ⚠️  semantic/colors.json not found or invalid: ${err.message}`);
        }

        console.log('📖 Step 4: Reading theme tokens...');
        const themeFiles = ['blog', 'shopping'];

        themeFiles.forEach(themeName => {
            const filePath = join(tokensDir, 'themes', `${themeName}.json`);
            try {
                const content = readFileSync(filePath, 'utf-8');
                const themeTokens = JSON.parse(content);

                const themeVars = new Map();

                let themeContent = themeTokens;
                if (themeTokens.themes?.[themeName]) {
                    themeContent = themeTokens.themes[themeName];
                } else if (themeTokens[themeName]) {
                    themeContent = themeTokens[themeName];
                }

                function processTheme(obj, prefix = '--color') {
                    for (const [key, value] of Object.entries(obj)) {
                        if (key.startsWith('$')) continue;

                        const varName = `${prefix}-${key}`;

                        if (typeof value === 'object' && value !== null && !('$value' in value)) {
                            processTheme(value, `${prefix}-${key}`);
                        } else if (typeof value === 'string') {
                            const resolved = resolveColorReference(value, colorReferences);

                            if (resolved.startsWith('{')) {
                                const refPath = resolved.match(/^\{([^}]+)\}$/)?.[1];
                                if (refPath) unresolvedReferences.add(refPath);
                            }

                            themeVars.set(varName, resolved);
                        }
                    }
                }

                processTheme(themeContent);
                themes.set(themeName, themeVars);
                console.log(`  ✅ themes/${themeName}.json loaded`);
            } catch (err) {
                console.log(`  ⚠️  themes/${themeName}.json not found or invalid`);
            }
        });

        if (unresolvedReferences.size > 0) {
            console.log('\n⚠️  UNRESOLVED REFERENCES:');
            Array.from(unresolvedReferences).sort().forEach(ref => {
                console.log(`   • {${ref}}`);
            });
            console.log('\n📋 Available colors in reference map:');
            const availableColors = Object.keys(colorReferences)
                .filter(k => k.startsWith('color.'))
                .sort();
            availableColors.slice(0, 30).forEach(color => {
                console.log(`   • ${color}`);
            });
            if (availableColors.length > 30) {
                console.log(`   ... and ${availableColors.length - 30} more`);
            }
        } else {
            console.log('\n✅ All color references resolved successfully!');
        }

        console.log('\n🎨 Step 5: Generating CSS...');

        let cssContent = `@tailwind base;
@tailwind components;
@tailwind utilities;

/* ============================================
   Auto-generated Design System CSS Variables
   DO NOT EDIT MANUALLY - Generated by build-tokens.js
   ============================================ */

:root {
`;

        const sortedVars = Array.from(cssVariables.entries()).sort();
        for (const [key, value] of sortedVars) {
            cssContent += `    ${key}: ${value};\n`;
        }

        cssContent += `}

/* ============================================
   Dark Mode Overrides
   ============================================ */
[data-theme="dark"] {
    /* Brand Colors */
    --semantic-brand-primary: #20C997;
    --semantic-brand-primaryHover: #12B886;
    --semantic-brand-secondary: #63E6BE;
    
    /* Text Colors  */
    --semantic-text-heading: #F0F6FC;
    --semantic-text-body: #C9D1D9;
    --semantic-text-meta: #8B949E;
    --semantic-text-muted: #6E7681;
    --semantic-text-inverse: #111827;
    --semantic-text-link: #58A6FF;
    --semantic-text-linkHover: #A5D6FF;
    
    /* Background Colors  */
    --semantic-bg-page: #0F1419;
    --semantic-bg-card: #1C2128;
    --semantic-bg-elevated: #2D333B;
    --semantic-bg-muted: #22272E;
    --semantic-bg-hover: #2D333B;
    
    /* Border Colors  */
    --semantic-border-default: #444C56;
    --semantic-border-hover: #6E7681;
    --semantic-border-muted: #2C2C2C;
    --semantic-border-focus: #58A6FF;
    
    /* Status Colors  */
    --semantic-status-success: #20C997;
    --semantic-status-successBg: #0A4034;
    --semantic-status-error: #FA5252;
    --semantic-status-errorBg: #4A1C1C;
    --semantic-status-warning: #FCC419;
    --semantic-status-warningBg: #4A3C0F;
    --semantic-status-info: #339AF0;
    --semantic-status-infoBg: #1A3A52;
    
    /* Shadows */
    --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.3);
    --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.4);
    --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.5);
    --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.6);
}

/* ============================================
   Theme Overrides (Service-specific)
   ============================================ */
`;

        for (const [themeName, themeVars] of themes) {
            if (themeVars.size > 0) {
                cssContent += `[data-service="${themeName}"] {\n`;
                const sortedThemeVars = Array.from(themeVars.entries()).sort();
                for (const [key, value] of sortedThemeVars) {
                    cssContent += `    ${key}: ${value};\n`;
                }
                cssContent += `}\n\n`;
            }
        }

        cssContent += `/* ============================================
   Base Styles
   ============================================ */
@layer base {
    body {
        @apply bg-bg-page text-text-body;
        @apply font-sans antialiased;
        -webkit-font-smoothing: antialiased;
        -moz-osx-font-smoothing: grayscale;
    }

    h1, h2, h3, h4, h5, h6 {
        @apply font-semibold;
        text-wrap: balance;
    }

    code {
        @apply font-mono text-sm;
    }

    a {
        @apply transition-colors duration-200;
    }
}

/* ============================================
   Utility Classes
   ============================================ */
@layer utilities {
    .text-balance {
        text-wrap: balance;
    }

    .text-pretty {
        text-wrap: pretty;
    }
}
`;

        writeFileSync(outputFile, cssContent, 'utf-8');
        console.log(`✅ CSS variables written to: ${outputFile}`);
        console.log(`📊 Total variables generated: ${cssVariables.size}`);
        console.log(`🎨 Themes generated: ${themes.size}`);
        console.log('\n✨ Design tokens built successfully!');

    } catch (error) {
        console.error('❌ Fatal error:', error.message);
        process.exit(1);
    }
}

buildTokens();