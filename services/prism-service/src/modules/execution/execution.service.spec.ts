import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { ExecutionService } from './execution.service';
import { Execution, ExecutionStatus } from './execution.entity';
import { Task, TaskStatus, TaskPriority } from '../task/task.entity';
import { Agent, AgentRole } from '../agent/agent.entity';
import { TaskService } from '../task/task.service';
import { AgentService } from '../agent/agent.service';
import { AIService } from '../ai/ai.service';
import { KafkaProducer } from '../event/kafka.producer';
import { SseService } from '../sse/sse.service';
import { BusinessException } from '../../common/filters/business.exception';

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 50));

describe('ExecutionService', () => {
  let service: ExecutionService;
  let executionRepo: Record<string, jest.Mock>;
  let taskRepo: Record<string, jest.Mock>;
  let taskService: Record<string, jest.Mock>;
  let agentService: Record<string, jest.Mock>;
  let aiService: Record<string, jest.Mock>;
  let kafkaProducer: Record<string, jest.Mock>;
  let sseService: Record<string, jest.Mock>;
  let mockExecQueryBuilder: Record<string, jest.Mock>;

  const userId = 'user-123';

  const makeAgent = (overrides?: Partial<Agent>): Agent =>
    ({
      id: 1,
      userId,
      providerId: 1,
      name: 'PM Agent',
      role: AgentRole.PM,
      systemPrompt: 'You are a PM',
      model: 'gpt-4o',
      temperature: 0.7,
      maxTokens: 4096,
      ...overrides,
    }) as Agent;

  const makeTask = (overrides?: Partial<Task>): Task =>
    ({
      id: 1,
      boardId: 1,
      agentId: 1,
      agent: makeAgent(),
      title: 'Test Task',
      description: 'Task description',
      status: TaskStatus.IN_PROGRESS,
      priority: TaskPriority.MEDIUM,
      position: 0,
      dueDate: null,
      referencedTaskIds: null,
      createdAt: new Date(),
      updatedAt: new Date(),
      executions: [],
      ...overrides,
    }) as Task;

  const makeExecution = (overrides?: Partial<Execution>): Execution =>
    ({
      id: 1,
      taskId: 1,
      agentId: 1,
      executionNumber: 1,
      status: ExecutionStatus.PENDING,
      inputPrompt: 'Task: Test Task\n\nDescription:\nTask description',
      outputResult: null,
      userFeedback: null,
      inputTokens: null,
      outputTokens: null,
      durationMs: null,
      errorMessage: null,
      startedAt: null,
      completedAt: null,
      createdAt: new Date(),
      agent: makeAgent(),
      task: makeTask(),
      ...overrides,
    }) as Execution;

  beforeEach(async () => {
    mockExecQueryBuilder = {
      leftJoin: jest.fn().mockReturnThis(),
      where: jest.fn().mockReturnThis(),
      andWhere: jest.fn().mockReturnThis(),
      getOne: jest.fn(),
    };

    executionRepo = {
      findOne: jest.fn(),
      find: jest.fn(),
      count: jest.fn(),
      create: jest.fn((data: Partial<Execution>) => ({
        ...makeExecution(),
        ...data,
      })),
      save: jest.fn((entity: Execution) =>
        Promise.resolve({ ...entity, id: entity.id || 1 }),
      ),
      createQueryBuilder: jest.fn().mockReturnValue(mockExecQueryBuilder),
    };

    taskRepo = {
      findOne: jest.fn(),
    };

    taskService = {
      getTaskEntity: jest.fn(),
      completeTask: jest.fn().mockResolvedValue(undefined),
    };

    agentService = {
      getAgentEntity: jest.fn(),
    };

    aiService = {
      generate: jest.fn(),
    };

    kafkaProducer = {
      sendTaskCompleted: jest.fn().mockResolvedValue(undefined),
      sendTaskFailed: jest.fn().mockResolvedValue(undefined),
    };

    sseService = {
      emitExecutionStarted: jest.fn(),
      emitExecutionCompleted: jest.fn(),
      emitExecutionFailed: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ExecutionService,
        { provide: getRepositoryToken(Execution), useValue: executionRepo },
        { provide: getRepositoryToken(Task), useValue: taskRepo },
        { provide: TaskService, useValue: taskService },
        { provide: AgentService, useValue: agentService },
        { provide: AIService, useValue: aiService },
        { provide: KafkaProducer, useValue: kafkaProducer },
        { provide: SseService, useValue: sseService },
      ],
    }).compile();

    service = module.get<ExecutionService>(ExecutionService);
  });

  describe('executeTask', () => {
    beforeEach(() => {
      taskService.getTaskEntity.mockResolvedValue(makeTask());
      agentService.getAgentEntity.mockResolvedValue(makeAgent());
      executionRepo.count.mockResolvedValue(0);
      // For the async runExecution: findOne returns the execution
      executionRepo.findOne.mockResolvedValue(makeExecution());
      aiService.generate.mockResolvedValue({
        content: 'AI result',
        inputTokens: 100,
        outputTokens: 200,
        model: 'gpt-4o',
      });
    });

    it('should create execution record', async () => {
      const result = await service.executeTask(userId, 1);

      expect(executionRepo.create).toHaveBeenCalledWith(
        expect.objectContaining({
          taskId: 1,
          agentId: 1,
          status: ExecutionStatus.PENDING,
        }),
      );
      expect(executionRepo.save).toHaveBeenCalled();
      expect(result).toBeDefined();
    });

    it('should throw notFound for invalid task', async () => {
      taskService.getTaskEntity.mockRejectedValue(
        BusinessException.notFound('Task'),
      );

      await expect(service.executeTask(userId, 999)).rejects.toThrow(
        BusinessException,
      );
    });

    it('should throw agentNotAssigned when no agent', async () => {
      taskService.getTaskEntity.mockResolvedValue(
        makeTask({ agentId: null, agent: null }),
      );

      await expect(service.executeTask(userId, 1)).rejects.toThrow(
        BusinessException,
      );
    });

    it('should calculate executionNumber', async () => {
      executionRepo.count.mockResolvedValue(3);

      await service.executeTask(userId, 1);

      expect(executionRepo.create).toHaveBeenCalledWith(
        expect.objectContaining({
          executionNumber: 4, // count(3) + 1
        }),
      );
    });

    it('should return execution dto immediately', async () => {
      const result = await service.executeTask(userId, 1);

      expect(result.id).toBeDefined();
      expect(result.status).toBe(ExecutionStatus.PENDING);
    });
  });

  describe('runExecution (async flow)', () => {
    // Track status snapshots because runExecution mutates the execution object in-place.
    // Without snapshots, all save call references would show the final status.
    let savedSnapshots: Array<{ status: string; [key: string]: any }>;

    beforeEach(() => {
      savedSnapshots = [];
      taskService.getTaskEntity.mockResolvedValue(makeTask());
      agentService.getAgentEntity.mockResolvedValue(makeAgent());
      executionRepo.count.mockResolvedValue(0);
      executionRepo.findOne.mockResolvedValue(makeExecution());

      // Override save to capture snapshot copies
      executionRepo.save = jest.fn((entity: any) => {
        savedSnapshots.push({ ...entity });
        return Promise.resolve({ ...entity, id: entity.id || 1 });
      });
    });

    it('should set status RUNNING on start', async () => {
      aiService.generate.mockResolvedValue({
        content: 'Result',
        inputTokens: 100,
        outputTokens: 200,
        model: 'gpt-4o',
      });

      await service.executeTask(userId, 1);
      await flushPromises();

      const runningSnapshot = savedSnapshots.find(
        (s) => s.status === ExecutionStatus.RUNNING,
      );
      expect(runningSnapshot).toBeDefined();
      expect(runningSnapshot!.startedAt).toBeDefined();
    });

    it('should emit SSE execution started', async () => {
      aiService.generate.mockResolvedValue({
        content: 'Result',
        inputTokens: 100,
        outputTokens: 200,
        model: 'gpt-4o',
      });

      await service.executeTask(userId, 1);
      await flushPromises();

      expect(sseService.emitExecutionStarted).toHaveBeenCalledWith(
        userId,
        1, // boardId
        1, // taskId
        1, // executionId
      );
    });

    it('should call AIService generate', async () => {
      aiService.generate.mockResolvedValue({
        content: 'Result',
        inputTokens: 100,
        outputTokens: 200,
        model: 'gpt-4o',
      });

      await service.executeTask(userId, 1);
      await flushPromises();

      expect(aiService.generate).toHaveBeenCalledWith(
        userId,
        1, // providerId
        expect.objectContaining({
          systemPrompt: 'You are a PM',
          model: 'gpt-4o',
        }),
      );
    });

    it('should save output on success', async () => {
      aiService.generate.mockResolvedValue({
        content: 'AI generated result',
        inputTokens: 150,
        outputTokens: 300,
        model: 'gpt-4o',
      });

      await service.executeTask(userId, 1);
      await flushPromises();

      const completedSnapshot = savedSnapshots.find(
        (s) => s.status === ExecutionStatus.COMPLETED,
      );
      expect(completedSnapshot).toBeDefined();
      expect(completedSnapshot!.outputResult).toBe('AI generated result');
      expect(completedSnapshot!.inputTokens).toBe(150);
      expect(completedSnapshot!.outputTokens).toBe(300);
    });

    it('should set status COMPLETED on success', async () => {
      aiService.generate.mockResolvedValue({
        content: 'Result',
        inputTokens: 100,
        outputTokens: 200,
        model: 'gpt-4o',
      });

      await service.executeTask(userId, 1);
      await flushPromises();

      const completedSnapshot = savedSnapshots.find(
        (s) => s.status === ExecutionStatus.COMPLETED,
      );
      expect(completedSnapshot).toBeDefined();
      expect(completedSnapshot!.completedAt).toBeDefined();
    });

    it('should send kafka task completed', async () => {
      aiService.generate.mockResolvedValue({
        content: 'Result',
        inputTokens: 100,
        outputTokens: 200,
        model: 'gpt-4o',
      });

      await service.executeTask(userId, 1);
      await flushPromises();

      expect(kafkaProducer.sendTaskCompleted).toHaveBeenCalledWith(
        expect.objectContaining({
          taskId: 1,
          boardId: 1,
          userId,
          status: 'IN_REVIEW',
        }),
      );
    });

    it('should emit SSE execution completed', async () => {
      aiService.generate.mockResolvedValue({
        content: 'Result content',
        inputTokens: 100,
        outputTokens: 200,
        model: 'gpt-4o',
      });

      await service.executeTask(userId, 1);
      await flushPromises();

      expect(sseService.emitExecutionCompleted).toHaveBeenCalledWith(
        userId,
        1,
        1,
        1,
        'Result content',
      );
    });

    it('should set status FAILED on error', async () => {
      aiService.generate.mockRejectedValue(new Error('AI error'));

      await service.executeTask(userId, 1);
      await flushPromises();

      const failedSnapshot = savedSnapshots.find(
        (s) => s.status === ExecutionStatus.FAILED,
      );
      expect(failedSnapshot).toBeDefined();
    });

    it('should save errorMessage on failure', async () => {
      aiService.generate.mockRejectedValue(new Error('AI model error'));

      await service.executeTask(userId, 1);
      await flushPromises();

      const failedSnapshot = savedSnapshots.find(
        (s) => s.status === ExecutionStatus.FAILED,
      );
      expect(failedSnapshot).toBeDefined();
      expect(failedSnapshot!.errorMessage).toBe('AI model error');
    });

    it('should send kafka task failed', async () => {
      aiService.generate.mockRejectedValue(new Error('AI error'));

      await service.executeTask(userId, 1);
      await flushPromises();

      expect(kafkaProducer.sendTaskFailed).toHaveBeenCalledWith(
        expect.objectContaining({
          taskId: 1,
          status: 'FAILED',
          errorMessage: 'AI error',
        }),
      );
    });

    it('should emit SSE execution failed', async () => {
      aiService.generate.mockRejectedValue(new Error('AI error'));

      await service.executeTask(userId, 1);
      await flushPromises();

      expect(sseService.emitExecutionFailed).toHaveBeenCalledWith(
        userId,
        1,
        1,
        1,
        'AI error',
      );
    });
  });

  describe('buildPrompt (via executeTask)', () => {
    it('should build prompt with referenced results', async () => {
      const task = makeTask({ referencedTaskIds: [2] });
      taskService.getTaskEntity.mockResolvedValue(task);
      agentService.getAgentEntity.mockResolvedValue(makeAgent());
      executionRepo.count.mockResolvedValue(0);
      executionRepo.findOne
        .mockResolvedValueOnce(
          makeExecution({
            taskId: 2,
            outputResult: 'Referenced output',
            status: ExecutionStatus.COMPLETED,
          }),
        )
        // For runExecution's findOne
        .mockResolvedValueOnce(makeExecution());
      taskRepo.findOne.mockResolvedValue(
        makeTask({ id: 2, title: 'Referenced Task' }),
      );
      aiService.generate.mockResolvedValue({
        content: 'Result',
        inputTokens: 100,
        outputTokens: 200,
        model: 'gpt-4o',
      });

      await service.executeTask(userId, 1);

      const createCall = executionRepo.create.mock.calls[0][0];
      expect(createCall.inputPrompt).toContain('Referenced Task Results');
      expect(createCall.inputPrompt).toContain('Referenced Task');
      expect(createCall.inputPrompt).toContain('Referenced output');
    });
  });

  describe('findByTask', () => {
    it('should find executions by task', async () => {
      taskService.getTaskEntity.mockResolvedValue(makeTask());
      executionRepo.find.mockResolvedValue([
        makeExecution(),
        makeExecution({ id: 2, executionNumber: 2 }),
      ]);

      const result = await service.findByTask(userId, 1);

      expect(result).toHaveLength(2);
      expect(executionRepo.find).toHaveBeenCalledWith(
        expect.objectContaining({
          where: { taskId: 1 },
          order: { executionNumber: 'DESC' },
        }),
      );
    });
  });

  describe('findOne', () => {
    it('should return execution by id', async () => {
      mockExecQueryBuilder.getOne.mockResolvedValue(makeExecution());

      const result = await service.findOne(userId, 1);

      expect(result.id).toBe(1);
      expect(result.status).toBe(ExecutionStatus.PENDING);
    });
  });
});
