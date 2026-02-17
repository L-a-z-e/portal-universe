import { readFileSync } from 'fs';
import { join, resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const colorPath = join(__dirname, '../portal-universe/frontend/design-system/src/tokens/base/colors.json');
const colorContent = readFileSync(colorPath, 'utf-8');
const colorTokens = JSON.parse(colorContent);

const colorReferences = {};

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

console.log('Testing buildColorMap with colorTokens.color:');
buildColorMap(colorTokens.color || colorTokens);

console.log('\n=== ALL COLOR REFERENCES ===');
Object.entries(colorReferences)
    .sort()
    .forEach(([key, val]) => {
        console.log(`${key}: ${val}`);
    });

console.log(`\n✅ Total: ${Object.keys(colorReferences).length} colors`);
console.log('\n=== CHECKING SPECIFIC KEYS ===');
console.log(`Has "color.green.600"? ${colorReferences['color.green.600'] ? '✅' : '❌'}`);
console.log(`Has "neutral.green.600"? ${colorReferences['neutral.green.600'] ? '✅' : '❌'}`);
console.log(`Has "green.600"? ${colorReferences['green.600'] ? '✅' : '❌'}`);