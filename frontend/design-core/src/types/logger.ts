export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

export interface ErrorReporter {
  captureError(error: unknown, context?: Record<string, unknown>): void;
  captureMessage(message: string, level: LogLevel): void;
}

export interface LoggerOptions {
  moduleName: string;
  level?: LogLevel;
  reporter?: ErrorReporter;
}

export interface Logger {
  debug(...args: unknown[]): void;
  info(...args: unknown[]): void;
  warn(...args: unknown[]): void;
  error(error: unknown, context?: Record<string, unknown>): void;
}

const LEVEL_PRIORITY: Record<LogLevel, number> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3,
};

export function createLogger(options: LoggerOptions): Logger {
  const { moduleName, level = 'debug', reporter } = options;
  const prefix = `[${moduleName}]`;
  const minPriority = LEVEL_PRIORITY[level];

  return {
    debug(...args: unknown[]) {
      if (minPriority <= LEVEL_PRIORITY.debug) {
        console.log(prefix, ...args);
      }
    },

    info(...args: unknown[]) {
      if (minPriority <= LEVEL_PRIORITY.info) {
        console.log(prefix, ...args);
      }
    },

    warn(...args: unknown[]) {
      if (minPriority <= LEVEL_PRIORITY.warn) {
        console.warn(prefix, ...args);
      }
    },

    error(error: unknown, context?: Record<string, unknown>) {
      if (minPriority <= LEVEL_PRIORITY.error) {
        console.error(prefix, error);
        if (context) {
          console.error(prefix, 'Context:', context);
        }
      }
      reporter?.captureError(error, { module: moduleName, ...context });
    },
  };
}
