import { KafkaProducer, TaskEvent } from './kafka.producer';
import { ConfigService } from '@nestjs/config';

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

    // Mock the Kafka class on the module level
    jest.spyOn(require('kafkajs'), 'Kafka').mockImplementation(() => ({
      producer: jest.fn().mockReturnValue(mockProducerInstance),
    }));

    const configService = {
      get: jest.fn().mockReturnValue(['localhost:9092']),
    } as unknown as ConfigService;

    producer = new KafkaProducer(configService);

    // Manually set the private producer field to our mock
    (producer as any).producer = mockProducerInstance;
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe('onModuleInit', () => {
    it('should connect producer on init', async () => {
      await producer.onModuleInit();

      expect(mockProducerInstance.connect).toHaveBeenCalled();
      expect((producer as any).isConnected).toBe(true);
    });

    it('should handle connection failure gracefully', async () => {
      mockProducerInstance.connect.mockRejectedValue(
        new Error('Connection refused'),
      );

      await producer.onModuleInit();

      expect((producer as any).isConnected).toBe(false);
    });
  });

  describe('onModuleDestroy', () => {
    it('should disconnect when connected', async () => {
      await producer.onModuleInit();
      await producer.onModuleDestroy();

      expect(mockProducerInstance.disconnect).toHaveBeenCalled();
    });

    it('should not disconnect when not connected', async () => {
      // Not calling onModuleInit, so isConnected = false
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
      status: 'DONE',
      agentName: 'PM Agent',
      executionId: 100,
      timestamp: new Date().toISOString(),
    };

    it('should send event to prism.task.completed topic', async () => {
      await producer.onModuleInit();
      await producer.sendTaskCompleted(taskEvent);

      expect(mockProducerInstance.send).toHaveBeenCalledWith(
        expect.objectContaining({
          topic: 'prism.task.completed',
          messages: expect.arrayContaining([
            expect.objectContaining({
              key: '1',
              value: JSON.stringify(taskEvent),
            }),
          ]),
        }),
      );
    });

    it('should skip sending when not connected', async () => {
      // Don't call onModuleInit
      await producer.sendTaskCompleted(taskEvent);

      expect(mockProducerInstance.send).not.toHaveBeenCalled();
    });
  });

  describe('sendTaskFailed', () => {
    it('should send event to prism.task.failed topic', async () => {
      await producer.onModuleInit();
      const failedEvent = {
        taskId: 2,
        boardId: 10,
        userId: 'user-123',
        title: 'Failed Task',
        status: 'CANCELLED',
        errorMessage: 'AI execution timed out',
        timestamp: new Date().toISOString(),
      };

      await producer.sendTaskFailed(failedEvent);

      expect(mockProducerInstance.send).toHaveBeenCalledWith(
        expect.objectContaining({
          topic: 'prism.task.failed',
        }),
      );
    });
  });
});
