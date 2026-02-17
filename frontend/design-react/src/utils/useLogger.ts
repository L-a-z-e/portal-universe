import { useMemo } from 'react';
import { createLogger } from '@portal/design-core';
import type { LoggerOptions, Logger } from '@portal/design-core';

export function useLogger(options: LoggerOptions): Logger {
  return useMemo(
    () => createLogger(options),
    [options.moduleName, options.level, options.reporter],
  );
}
