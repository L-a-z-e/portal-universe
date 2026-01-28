import {
  Injectable,
  OnModuleInit,
  OnModuleDestroy,
  Logger,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Kafka, Producer, ProducerRecord } from 'kafkajs';

export interface TaskEvent {
  taskId: number;
  boardId: number;
  userId: string;
  title: string;
  status: string;
  agentName?: string;
  executionId?: number;
  timestamp: string;
}

@Injectable()
export class KafkaProducer implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(KafkaProducer.name);
  private kafka: Kafka;
  private producer: Producer;
  private isConnected = false;

  constructor(private configService: ConfigService) {
    const brokers = this.configService.get<string[]>('kafka.brokers') || [
      'localhost:9092',
    ];

    this.kafka = new Kafka({
      clientId: 'prism-service',
      brokers,
      retry: {
        initialRetryTime: 100,
        retries: 3,
      },
    });

    this.producer = this.kafka.producer();
  }

  async onModuleInit() {
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

  async onModuleDestroy() {
    if (this.isConnected) {
      await this.producer.disconnect();
      this.logger.log('Kafka producer disconnected');
    }
  }

  async sendTaskCompleted(event: TaskEvent): Promise<void> {
    await this.send('prism.task.completed', event);
  }

  async sendTaskFailed(
    event: TaskEvent & { errorMessage: string },
  ): Promise<void> {
    await this.send('prism.task.failed', event);
  }

  private async send(topic: string, message: object): Promise<void> {
    if (!this.isConnected) {
      this.logger.debug(`Kafka not connected, skipping event: ${topic}`);
      return;
    }

    const record: ProducerRecord = {
      topic,
      messages: [
        {
          key: String((message as TaskEvent).taskId),
          value: JSON.stringify(message),
          timestamp: String(Date.now()),
        },
      ],
    };

    try {
      await this.producer.send(record);
      this.logger.debug(
        `Event sent to ${topic}: taskId=${(message as TaskEvent).taskId}`,
      );
    } catch (error) {
      this.logger.error(`Failed to send event to ${topic}:`, error);
    }
  }
}
