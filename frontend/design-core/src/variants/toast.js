"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.toastVariants = exports.toastBase = void 0;
exports.toastBase = 'flex gap-3 p-4 rounded-lg bg-bg-card border shadow-lg';
exports.toastVariants = {
    info: {
        container: 'border-status-info/30',
        icon: 'text-status-info',
    },
    success: {
        container: 'border-status-success/30',
        icon: 'text-status-success',
    },
    warning: {
        container: 'border-status-warning/30',
        icon: 'text-status-warning',
    },
    error: {
        container: 'border-status-error/30',
        icon: 'text-status-error',
    },
};
