import { config } from 'dotenv';

// Load env file before any OTel config — mirrors ConfigModule's envFilePath logic
config({ path: `.env.${process.env.NODE_ENV || 'local'}` });

import { NodeSDK } from '@opentelemetry/sdk-node';
import { getNodeAutoInstrumentations } from '@opentelemetry/auto-instrumentations-node';
import { PrometheusExporter } from '@opentelemetry/exporter-prometheus';
import { ZipkinExporter } from '@opentelemetry/exporter-zipkin';
import { BatchSpanProcessor } from '@opentelemetry/sdk-trace-node';
import { Resource } from '@opentelemetry/resources';
import {
  ATTR_SERVICE_NAME,
  ATTR_SERVICE_VERSION,
} from '@opentelemetry/semantic-conventions';

const resource = new Resource({
  [ATTR_SERVICE_NAME]: 'prism-service',
  [ATTR_SERVICE_VERSION]: '0.0.1',
});

const prometheusExporter = new PrometheusExporter(
  { port: 9464 },
  () => {
    console.log('OTel Prometheus metrics available at http://localhost:9464/metrics');
  },
);

const tracingEnabled = process.env.OTEL_TRACES_EXPORTER !== 'none';

const spanProcessors = tracingEnabled
  ? [
      new BatchSpanProcessor(
        new ZipkinExporter({
          url:
            process.env.OTEL_EXPORTER_ZIPKIN_ENDPOINT ||
            'http://localhost:9411/api/v2/spans',
        }),
      ),
    ]
  : [];

const sdk = new NodeSDK({
  resource,
  metricReader: prometheusExporter,
  spanProcessors,
  instrumentations: [
    getNodeAutoInstrumentations({
      '@opentelemetry/instrumentation-fs': { enabled: false },
      '@opentelemetry/instrumentation-dns': { enabled: false },
      '@opentelemetry/instrumentation-net': { enabled: false },
    }),
  ],
});

sdk.start();

process.on('SIGTERM', () => {
  sdk.shutdown().then(
    () => console.log('OTel SDK shut down'),
    (err) => console.error('Error shutting down OTel SDK', err),
  );
});
