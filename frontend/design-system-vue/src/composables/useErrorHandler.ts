import type { App, ComponentPublicInstance } from 'vue';
import { createLogger } from '@portal/design-types';
import type { ErrorReporter } from '@portal/design-types';

export interface ErrorHandlerOptions {
  moduleName: string;
  reporter?: ErrorReporter;
}

export function setupErrorHandler(app: App, options: ErrorHandlerOptions): void {
  const logger = createLogger({
    moduleName: options.moduleName,
    reporter: options.reporter,
  });

  app.config.errorHandler = (
    err: unknown,
    instance: ComponentPublicInstance | null,
    info: string,
  ) => {
    logger.error(err, {
      info,
      component: instance?.$options.name ?? undefined,
    });
  };

  if (typeof window !== 'undefined') {
    window.addEventListener('unhandledrejection', (event) => {
      logger.error(event.reason, { type: 'unhandledrejection' });
      event.preventDefault();
    });
  }
}
