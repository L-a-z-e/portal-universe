import { WinstonModule, utilities } from 'nest-winston';
import * as winston from 'winston';
import { trace, context } from '@opentelemetry/api';

const traceFormat = winston.format((info) => {
  const span = trace.getSpan(context.active());
  if (span) {
    const spanContext = span.spanContext();
    info.traceId = spanContext.traceId;
    info.spanId = spanContext.spanId;
  } else {
    info.traceId = '0'.repeat(32);
    info.spanId = '0'.repeat(16);
  }
  info.service_name = 'prism-service';
  return info;
});

const isLocal = process.env.NODE_ENV === 'local';

export const winstonLogger = WinstonModule.createLogger({
  transports: [
    new winston.transports.Console({
      format: isLocal
        ? winston.format.combine(
            winston.format.timestamp(),
            traceFormat(),
            utilities.format.nestLike('PrismService', { prettyPrint: true }),
          )
        : winston.format.combine(
            winston.format.timestamp(),
            traceFormat(),
            winston.format.json(),
          ),
    }),
  ],
});
