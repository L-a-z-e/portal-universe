import { cpSync, mkdirSync, existsSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const srcDir = resolve(__dirname, '../src/styles');
const distDir = resolve(__dirname, '../dist/styles');

if (!existsSync(distDir)) {
    mkdirSync(distDir, { recursive: true });
}

cpSync(srcDir, distDir, { recursive: true });
console.log('  styles copied to dist/styles/');
