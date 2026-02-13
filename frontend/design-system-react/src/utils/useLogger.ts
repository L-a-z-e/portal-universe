import { useMemo } from 'react';
import { createLogger } from '@portal/design-types';
import type { LoggerOptions, Logger } from '@portal/design-types';

export function useLogger(options: LoggerOptions): Logger {
  return useMemo(
    () => createLogger(options),
    [options.moduleName, options.level, options.reporter],
  );
}
