import {
  Injectable,
  OnModuleInit,
  OnModuleDestroy,
  Logger,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Kafka, Producer, ProducerRecord } from 'kafkajs';
import { SchemaRegistry } from '@kafkajs/confluent-schema-registry';
import { PrismTopics } from './prism-topics';

export interface TaskEvent {
  taskId: number;
  boardId: number;
  userId: string;
  title: string;
  status: string;
  agentName?: string | null;
  executionId?: number | null;
  timestamp: string;
}

export interface TaskFailedEvent extends TaskEvent {
  errorMessage: string;
}

@Injectable()
export class KafkaProducer implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(KafkaProducer.name);
  private kafka: Kafka;
  private producer: Producer;
  private registry: SchemaRegistry;
  private isConnected = false;
  private isRegistryAvailable = false;

  private completedSchemaId: number | null = null;
  private failedSchemaId: number | null = null;

  constructor(private configService: ConfigService) {
    const brokers = this.configService.get<string[]>('kafka.brokers') || [
      'localhost:9092',
    ];
    const registryUrl =
      this.configService.get<string>('kafka.schemaRegistry.url') ||
      'http://localhost:18081';

    this.kafka = new Kafka({
      clientId: 'prism-service',
      brokers,
      retry: {
        initialRetryTime: 100,
        retries: 3,
      },
    });

    this.producer = this.kafka.producer();
    this.registry = new SchemaRegistry({ host: registryUrl });
  }

  async onModuleInit() {
    await Promise.all([this.connectProducer(), this.resolveSchemaIds()]);
  }

  async onModuleDestroy() {
    if (this.isConnected) {
      await this.producer.disconnect();
      this.logger.log('Kafka producer disconnected');
    }
  }

  async sendTaskCompleted(event: TaskEvent): Promise<void> {
    const avroPayload = this.toAvroPayload(event);
    await this.send(
      PrismTopics.TASK_COMPLETED,
      avroPayload,
      this.completedSchemaId,
    );
  }

  async sendTaskFailed(event: TaskFailedEvent): Promise<void> {
    const avroPayload = this.toAvroPayload(event);
    await this.send(
      PrismTopics.TASK_FAILED,
      avroPayload,
      this.failedSchemaId,
    );
  }

  private async connectProducer(): Promise<void> {
    try {
      await this.producer.connect();
      this.isConnected = true;
      this.logger.log('Kafka producer connected');
    } catch (error) {
      this.logger.warn(
        'Kafka connection failed, events will be skipped:',
        error,
      );
    }
  }

  private async resolveSchemaIds(): Promise<void> {
    try {
      const [completedId, failedId] = await Promise.all([
        this.registry.getLatestSchemaId(
          `${PrismTopics.TASK_COMPLETED}-value`,
        ),
        this.registry.getLatestSchemaId(`${PrismTopics.TASK_FAILED}-value`),
      ]);
      this.completedSchemaId = completedId;
      this.failedSchemaId = failedId;
      this.isRegistryAvailable = true;
      this.logger.log(
        `Schema IDs resolved — completed: ${completedId}, failed: ${failedId}`,
      );
    } catch (error) {
      this.logger.warn(
        'Schema Registry unavailable, falling back to JSON:',
        error instanceof Error ? error.message : error,
      );
    }
  }

  private toAvroPayload(
    event: TaskEvent | TaskFailedEvent,
  ): Record<string, unknown> {
    return {
      ...event,
      agentName: event.agentName ?? null,
      executionId: event.executionId ?? null,
      timestamp: new Date(event.timestamp).getTime(),
    };
  }

  private async send(
    topic: string,
    payload: Record<string, unknown>,
    schemaId: number | null,
  ): Promise<void> {
    if (!this.isConnected) {
      this.logger.debug(`Kafka not connected, skipping event: ${topic}`);
      return;
    }

    const key = String(payload.taskId);
    let value: Buffer | string;

    if (this.isRegistryAvailable && schemaId !== null) {
      value = await this.registry.encode(schemaId, payload);
    } else {
      value = JSON.stringify(payload);
    }

    const record: ProducerRecord = {
      topic,
      messages: [{ key, value, timestamp: String(Date.now()) }],
    };

    try {
      await this.producer.send(record);
      this.logger.debug(
        `Event sent to ${topic}: taskId=${payload.taskId} (${this.isRegistryAvailable ? 'avro' : 'json'})`,
      );
    } catch (error) {
      this.logger.error(`Failed to send event to ${topic}:`, error);
    }
  }
}
