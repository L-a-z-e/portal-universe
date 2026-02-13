import { createLogger } from '@portal/design-types';
import type { LoggerOptions, Logger } from '@portal/design-types';

export function useLogger(options: LoggerOptions): Logger {
  return createLogger(options);
}
