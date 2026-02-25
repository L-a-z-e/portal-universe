import { KafkaProducer, TaskEvent } from './kafka.producer';
import { ConfigService } from '@nestjs/config';

const mockEncode = jest.fn();
const mockGetLatestSchemaId = jest.fn();

jest.mock('@kafkajs/confluent-schema-registry', () => ({
  SchemaRegistry: jest.fn().mockImplementation(() => ({
    encode: mockEncode,
    getLatestSchemaId: mockGetLatestSchemaId,
  })),
}));

describe('KafkaProducer', () => {
  let producer: KafkaProducer;
  let mockProducerInstance: {
    connect: jest.Mock;
    disconnect: jest.Mock;
    send: jest.Mock;
  };

  beforeEach(() => {
    mockProducerInstance = {
      connect: jest.fn().mockResolvedValue(undefined),
      disconnect: jest.fn().mockResolvedValue(undefined),
      send: jest.fn().mockResolvedValue(undefined),
    };

    jest.spyOn(require('kafkajs'), 'Kafka').mockImplementation(() => ({
      producer: jest.fn().mockReturnValue(mockProducerInstance),
    }));

    mockEncode.mockReset();
    mockGetLatestSchemaId.mockReset();
    mockGetLatestSchemaId.mockResolvedValue(1);
    mockEncode.mockResolvedValue(Buffer.from([0x00, 0x01, 0x02]));

    const configService = {
      get: jest.fn((key: string) => {
        if (key === 'kafka.brokers') return ['localhost:9092'];
        if (key === 'kafka.schemaRegistry.url')
          return 'http://localhost:18081';
        return undefined;
      }),
    } as unknown as ConfigService;

    producer = new KafkaProducer(configService);
    (producer as any).producer = mockProducerInstance;
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe('onModuleInit', () => {
    it('should connect producer and resolve schema IDs', async () => {
      await producer.onModuleInit();

      expect(mockProducerInstance.connect).toHaveBeenCalled();
      expect((producer as any).isConnected).toBe(true);
      expect(mockGetLatestSchemaId).toHaveBeenCalledTimes(2);
      expect((producer as any).isRegistryAvailable).toBe(true);
    });

    it('should handle Kafka connection failure gracefully', async () => {
      mockProducerInstance.connect.mockRejectedValue(
        new Error('Connection refused'),
      );

      await producer.onModuleInit();

      expect((producer as any).isConnected).toBe(false);
      expect((producer as any).isRegistryAvailable).toBe(true);
    });

    it('should handle Schema Registry failure gracefully', async () => {
      mockGetLatestSchemaId.mockRejectedValue(
        new Error('Registry unavailable'),
      );

      await producer.onModuleInit();

      expect((producer as any).isConnected).toBe(true);
      expect((producer as any).isRegistryAvailable).toBe(false);
    });
  });

  describe('onModuleDestroy', () => {
    it('should disconnect when connected', async () => {
      await producer.onModuleInit();
      await producer.onModuleDestroy();

      expect(mockProducerInstance.disconnect).toHaveBeenCalled();
    });

    it('should not disconnect when not connected', async () => {
      await producer.onModuleDestroy();

      expect(mockProducerInstance.disconnect).not.toHaveBeenCalled();
    });
  });

  describe('sendTaskCompleted', () => {
    const taskEvent: TaskEvent = {
      taskId: 1,
      boardId: 10,
      userId: 'user-123',
      title: 'Test Task',
      status: 'IN_REVIEW',
      agentName: 'PM Agent',
      executionId: 100,
      timestamp: '2026-02-21T10:00:00.000Z',
    };

    it('should send Avro-encoded event when registry is available', async () => {
      await producer.onModuleInit();
      await producer.sendTaskCompleted(taskEvent);

      expect(mockEncode).toHaveBeenCalledWith(
        1,
        expect.objectContaining({
          taskId: 1,
          boardId: 10,
          userId: 'user-123',
          title: 'Test Task',
          status: 'IN_REVIEW',
          agentName: 'PM Agent',
          executionId: 100,
          timestamp: new Date('2026-02-21T10:00:00.000Z').getTime(),
        }),
      );
      expect(mockProducerInstance.send).toHaveBeenCalledWith(
        expect.objectContaining({
          topic: 'prism.task.completed',
          messages: expect.arrayContaining([
            expect.objectContaining({
              key: '1',
              value: expect.any(Buffer),
            }),
          ]),
        }),
      );
    });

    it('should fall back to JSON when registry is unavailable', async () => {
      mockGetLatestSchemaId.mockRejectedValue(
        new Error('Registry unavailable'),
      );

      await producer.onModuleInit();
      await producer.sendTaskCompleted(taskEvent);

      expect(mockEncode).not.toHaveBeenCalled();
      expect(mockProducerInstance.send).toHaveBeenCalledWith(
        expect.objectContaining({
          topic: 'prism.task.completed',
          messages: expect.arrayContaining([
            expect.objectContaining({
              key: '1',
              value: expect.any(String),
            }),
          ]),
        }),
      );
    });

    it('should convert nullable fields to null when undefined', async () => {
      const eventWithoutOptionals: TaskEvent = {
        taskId: 2,
        boardId: 10,
        userId: 'user-123',
        title: 'Test Task',
        status: 'IN_REVIEW',
        timestamp: '2026-02-21T10:00:00.000Z',
      };

      await producer.onModuleInit();
      await producer.sendTaskCompleted(eventWithoutOptionals);

      expect(mockEncode).toHaveBeenCalledWith(
        1,
        expect.objectContaining({
          agentName: null,
          executionId: null,
        }),
      );
    });

    it('should skip sending when not connected', async () => {
      mockProducerInstance.connect.mockRejectedValue(
        new Error('Connection refused'),
      );

      await producer.onModuleInit();
      await producer.sendTaskCompleted(taskEvent);

      expect(mockProducerInstance.send).not.toHaveBeenCalled();
    });
  });

  describe('sendTaskFailed', () => {
    it('should send Avro-encoded failed event', async () => {
      await producer.onModuleInit();
      const failedEvent = {
        taskId: 2,
        boardId: 10,
        userId: 'user-123',
        title: 'Failed Task',
        status: 'FAILED',
        errorMessage: 'AI execution timed out',
        timestamp: '2026-02-21T10:00:00.000Z',
      };

      await producer.sendTaskFailed(failedEvent);

      expect(mockEncode).toHaveBeenCalledWith(
        1,
        expect.objectContaining({
          taskId: 2,
          errorMessage: 'AI execution timed out',
          timestamp: new Date('2026-02-21T10:00:00.000Z').getTime(),
        }),
      );
      expect(mockProducerInstance.send).toHaveBeenCalledWith(
        expect.objectContaining({
          topic: 'prism.task.failed',
        }),
      );
    });
  });
});
