import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { TaskService } from './task.service';
import { Task, TaskStatus, TaskPriority } from './task.entity';
import { Board } from '../board/board.entity';
import { Agent, AgentRole } from '../agent/agent.entity';
import { Execution, ExecutionStatus } from '../execution/execution.entity';
import { SseService } from '../sse/sse.service';
import { BusinessException } from '../../common/filters/business.exception';
import { TaskStateMachine } from './task-state-machine';

describe('TaskService', () => {
  let service: TaskService;
  let taskRepo: Record<string, jest.Mock>;
  let boardRepo: Record<string, jest.Mock>;
  let agentRepo: Record<string, jest.Mock>;
  let executionRepo: Record<string, jest.Mock>;
  let sseService: Record<string, jest.Mock>;
  let mockTaskQueryBuilder: Record<string, jest.Mock>;

  const userId = 'user-123';

  const makeAgent = (overrides?: Partial<Agent>): Agent =>
    ({
      id: 1,
      userId,
      providerId: 1,
      name: 'PM Agent',
      role: AgentRole.PM,
      description: null,
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
      status: TaskStatus.TODO,
      priority: TaskPriority.MEDIUM,
      position: 0,
      dueDate: null,
      referencedTaskIds: null,
      createdAt: new Date('2024-01-01'),
      updatedAt: new Date('2024-01-01'),
      executions: [],
      ...overrides,
    }) as Task;

  const makeExecution = (overrides?: Partial<Execution>): Execution =>
    ({
      id: 1,
      taskId: 1,
      agentId: 1,
      executionNumber: 1,
      status: ExecutionStatus.COMPLETED,
      inputPrompt: 'Task: Test',
      outputResult: 'Result',
      userFeedback: null,
      inputTokens: 100,
      outputTokens: 200,
      durationMs: 3000,
      errorMessage: null,
      startedAt: new Date(),
      completedAt: new Date(),
      createdAt: new Date(),
      agent: makeAgent(),
      ...overrides,
    }) as Execution;

  beforeEach(async () => {
    mockTaskQueryBuilder = {
      leftJoinAndSelect: jest.fn().mockReturnThis(),
      leftJoin: jest.fn().mockReturnThis(),
      where: jest.fn().mockReturnThis(),
      andWhere: jest.fn().mockReturnThis(),
      select: jest.fn().mockReturnThis(),
      orderBy: jest.fn().mockReturnThis(),
      getOne: jest.fn(),
      getRawOne: jest.fn(),
    };

    taskRepo = {
      findOne: jest.fn(),
      find: jest.fn(),
      create: jest.fn((data: Partial<Task>) => ({ ...makeTask(), ...data })),
      save: jest.fn((entity: Task) =>
        Promise.resolve({ ...entity, id: entity.id || 1 }),
      ),
      remove: jest.fn().mockResolvedValue(undefined),
      createQueryBuilder: jest.fn().mockReturnValue(mockTaskQueryBuilder),
    };

    boardRepo = {
      findOne: jest.fn(),
    };

    agentRepo = {
      findOne: jest.fn(),
    };

    executionRepo = {
      find: jest.fn(),
      findOne: jest.fn(),
    };

    sseService = {
      emitTaskCreated: jest.fn(),
      emitTaskUpdated: jest.fn(),
      emitTaskDeleted: jest.fn(),
      emitTaskMoved: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        TaskService,
        { provide: getRepositoryToken(Task), useValue: taskRepo },
        { provide: getRepositoryToken(Board), useValue: boardRepo },
        { provide: getRepositoryToken(Agent), useValue: agentRepo },
        { provide: getRepositoryToken(Execution), useValue: executionRepo },
        { provide: SseService, useValue: sseService },
      ],
    }).compile();

    service = module.get<TaskService>(TaskService);
  });

  describe('create', () => {
    const dto = {
      title: 'New Task',
      description: 'Description',
      priority: TaskPriority.HIGH,
      agentId: 1,
    };

    it('should create task with auto position', async () => {
      boardRepo.findOne.mockResolvedValue({ id: 1, userId });
      agentRepo.findOne.mockResolvedValue(makeAgent());
      mockTaskQueryBuilder.getRawOne.mockResolvedValue({ maxPos: 2 });
      // findOne -> findByIdAndUser
      mockTaskQueryBuilder.getOne.mockResolvedValue(
        makeTask({ title: 'New Task', position: 3 }),
      );

      const result = await service.create(userId, 1, dto);

      expect(taskRepo.create).toHaveBeenCalledWith(
        expect.objectContaining({ position: 3 }), // maxPos(2) + 1
      );
      expect(result.title).toBe('New Task');
    });

    it('should create task with agent', async () => {
      boardRepo.findOne.mockResolvedValue({ id: 1, userId });
      agentRepo.findOne.mockResolvedValue(makeAgent());
      mockTaskQueryBuilder.getRawOne.mockResolvedValue({ maxPos: null });
      mockTaskQueryBuilder.getOne.mockResolvedValue(makeTask());

      const result = await service.create(userId, 1, dto);

      expect(result.agentId).toBe(1);
    });

    it('should emit SSE on create', async () => {
      boardRepo.findOne.mockResolvedValue({ id: 1, userId });
      agentRepo.findOne.mockResolvedValue(makeAgent());
      mockTaskQueryBuilder.getRawOne.mockResolvedValue({ maxPos: null });
      mockTaskQueryBuilder.getOne.mockResolvedValue(makeTask());

      await service.create(userId, 1, dto);

      expect(sseService.emitTaskCreated).toHaveBeenCalledWith(
        userId,
        1,
        expect.any(Object),
      );
    });

    it('should throw notFound for invalid board', async () => {
      boardRepo.findOne.mockResolvedValue(null);

      await expect(service.create(userId, 999, dto)).rejects.toThrow(
        BusinessException,
      );
    });
  });

  describe('findAllByBoard', () => {
    it('should return tasks sorted by status and position', async () => {
      boardRepo.findOne.mockResolvedValue({ id: 1, userId });
      const tasks = [
        makeTask({ status: TaskStatus.TODO, position: 0 }),
        makeTask({ id: 2, status: TaskStatus.IN_PROGRESS, position: 0 }),
      ];
      taskRepo.find.mockResolvedValue(tasks);

      const result = await service.findAllByBoard(userId, 1);

      expect(result).toHaveLength(2);
      expect(taskRepo.find).toHaveBeenCalledWith(
        expect.objectContaining({
          order: { status: 'ASC', position: 'ASC' },
        }),
      );
    });

    it('should throw notFound for invalid board', async () => {
      boardRepo.findOne.mockResolvedValue(null);

      await expect(service.findAllByBoard(userId, 999)).rejects.toThrow(
        BusinessException,
      );
    });
  });

  describe('findOne', () => {
    it('should return single task', async () => {
      mockTaskQueryBuilder.getOne.mockResolvedValue(makeTask());

      const result = await service.findOne(userId, 1);

      expect(result.id).toBe(1);
      expect(result.title).toBe('Test Task');
    });

    it('should include availableActions in response', async () => {
      mockTaskQueryBuilder.getOne.mockResolvedValue(
        makeTask({ status: TaskStatus.TODO }),
      );

      const result = await service.findOne(userId, 1);

      expect(result.availableActions).toContain('execute');
      expect(result.availableActions).toContain('cancel');
    });

    it('should throw notFound for invalid task', async () => {
      mockTaskQueryBuilder.getOne.mockResolvedValue(null);

      await expect(service.findOne(userId, 999)).rejects.toThrow(
        BusinessException,
      );
    });
  });

  describe('update', () => {
    it('should update task fields', async () => {
      const task = makeTask();
      // findByIdAndUser
      mockTaskQueryBuilder.getOne.mockResolvedValueOnce(task);
      // findOne for result
      mockTaskQueryBuilder.getOne.mockResolvedValueOnce(
        makeTask({ title: 'Updated Title' }),
      );

      const result = await service.update(userId, 1, {
        title: 'Updated Title',
      });

      expect(result.title).toBe('Updated Title');
    });

    it('should emit SSE on update', async () => {
      mockTaskQueryBuilder.getOne
        .mockResolvedValueOnce(makeTask())
        .mockResolvedValueOnce(makeTask());

      await service.update(userId, 1, { title: 'Updated' });

      expect(sseService.emitTaskUpdated).toHaveBeenCalledWith(
        userId,
        1,
        expect.any(Object),
      );
    });
  });

  describe('remove', () => {
    it('should delete task', async () => {
      const task = makeTask();
      mockTaskQueryBuilder.getOne.mockResolvedValue(task);

      await service.remove(userId, 1);

      expect(taskRepo.remove).toHaveBeenCalledWith(task);
    });

    it('should emit SSE on delete', async () => {
      mockTaskQueryBuilder.getOne.mockResolvedValue(makeTask());

      await service.remove(userId, 1);

      expect(sseService.emitTaskDeleted).toHaveBeenCalledWith(userId, 1, 1);
    });
  });

  describe('changePosition', () => {
    it('should change task position', async () => {
      const task = makeTask({ position: 0 });
      mockTaskQueryBuilder.getOne
        .mockResolvedValueOnce(task)
        .mockResolvedValueOnce(makeTask({ position: 3 }));

      const result = await service.changePosition(userId, 1, { position: 3 });

      expect(taskRepo.save).toHaveBeenCalled();
      expect(result.position).toBe(3);
    });

    it('should emit SSE on position change', async () => {
      mockTaskQueryBuilder.getOne
        .mockResolvedValueOnce(makeTask({ status: TaskStatus.TODO }))
        .mockResolvedValueOnce(makeTask({ position: 2 }));

      await service.changePosition(userId, 1, { position: 2 });

      expect(sseService.emitTaskMoved).toHaveBeenCalledWith(
        userId,
        1,
        1,
        TaskStatus.TODO,
        TaskStatus.TODO,
        2,
      );
    });
  });

  describe('state transitions', () => {
    it('should execute task TODO to IN_PROGRESS', async () => {
      const task = makeTask({ status: TaskStatus.TODO, agentId: 1 });
      mockTaskQueryBuilder.getOne
        .mockResolvedValueOnce(task)
        .mockResolvedValueOnce(
          makeTask({ status: TaskStatus.IN_PROGRESS }),
        );

      const result = await service.execute(userId, 1);

      expect(result.status).toBe(TaskStatus.IN_PROGRESS);
    });

    it('should throw on execute non-TODO task', async () => {
      mockTaskQueryBuilder.getOne.mockResolvedValue(
        makeTask({ status: TaskStatus.DONE, agentId: 1 }),
      );

      await expect(service.execute(userId, 1)).rejects.toThrow(
        BusinessException,
      );
    });

    it('should throw agentNotAssigned on execute without agent', async () => {
      mockTaskQueryBuilder.getOne.mockResolvedValue(
        makeTask({ status: TaskStatus.TODO, agentId: null, agent: null }),
      );

      await expect(service.execute(userId, 1)).rejects.toThrow(
        BusinessException,
      );
    });

    it('should approve task IN_REVIEW to DONE', async () => {
      mockTaskQueryBuilder.getOne
        .mockResolvedValueOnce(makeTask({ status: TaskStatus.IN_REVIEW }))
        .mockResolvedValueOnce(makeTask({ status: TaskStatus.DONE }));

      const result = await service.approve(userId, 1);

      expect(result.status).toBe(TaskStatus.DONE);
    });

    it('should reject task IN_REVIEW to IN_PROGRESS', async () => {
      const task = makeTask({ status: TaskStatus.IN_REVIEW });
      mockTaskQueryBuilder.getOne
        .mockResolvedValueOnce(task) // reject -> findByIdAndUser
        .mockResolvedValueOnce(task) // performAction -> findByIdAndUser
        .mockResolvedValueOnce(
          makeTask({ status: TaskStatus.IN_PROGRESS }),
        ); // findOne for result

      const result = await service.reject(userId, 1, 'Needs improvement');

      expect(result.status).toBe(TaskStatus.IN_PROGRESS);
    });

    it('should cancel task', async () => {
      mockTaskQueryBuilder.getOne
        .mockResolvedValueOnce(makeTask({ status: TaskStatus.TODO }))
        .mockResolvedValueOnce(makeTask({ status: TaskStatus.CANCELLED }));

      const result = await service.cancel(userId, 1);

      expect(result.status).toBe(TaskStatus.CANCELLED);
    });

    it('should reopen task DONE to TODO', async () => {
      mockTaskQueryBuilder.getOne
        .mockResolvedValueOnce(makeTask({ status: TaskStatus.DONE }))
        .mockResolvedValueOnce(makeTask({ status: TaskStatus.TODO }));

      const result = await service.reopen(userId, 1);

      expect(result.status).toBe(TaskStatus.TODO);
    });

    it('should reopen task CANCELLED to TODO', async () => {
      mockTaskQueryBuilder.getOne
        .mockResolvedValueOnce(makeTask({ status: TaskStatus.CANCELLED }))
        .mockResolvedValueOnce(makeTask({ status: TaskStatus.TODO }));

      const result = await service.reopen(userId, 1);

      expect(result.status).toBe(TaskStatus.TODO);
    });
  });

  describe('completeTask', () => {
    it('should complete task to IN_REVIEW', async () => {
      const task = makeTask({ status: TaskStatus.IN_PROGRESS });
      taskRepo.findOne.mockResolvedValue(task);

      await service.completeTask(1);

      expect(task.status).toBe(TaskStatus.IN_REVIEW);
      expect(taskRepo.save).toHaveBeenCalledWith(
        expect.objectContaining({ status: TaskStatus.IN_REVIEW }),
      );
    });
  });

  describe('getContext', () => {
    it('should return context with executions', async () => {
      mockTaskQueryBuilder.getOne.mockResolvedValue(makeTask());
      executionRepo.find.mockResolvedValue([makeExecution()]);

      const result = await service.getContext(userId, 1);

      expect(result.previousExecutions).toHaveLength(1);
      expect(result.previousExecutions[0].id).toBe(1);
    });

    it('should return context with referenced tasks', async () => {
      const task = makeTask({ referencedTaskIds: [2, 3] });
      mockTaskQueryBuilder.getOne.mockResolvedValue(task);
      executionRepo.find.mockResolvedValue([]); // previous executions

      const refTasks = [
        makeTask({ id: 2, title: 'Ref Task 2' }),
        makeTask({ id: 3, title: 'Ref Task 3' }),
      ];
      taskRepo.find.mockResolvedValue(refTasks);
      executionRepo.findOne
        .mockResolvedValueOnce(makeExecution({ taskId: 2 }))
        .mockResolvedValueOnce(null);

      const result = await service.getContext(userId, 1);

      expect(result.referencedTasks).toHaveLength(2);
      expect(result.referencedTasks[0].taskTitle).toBe('Ref Task 2');
      expect(result.referencedTasks[0].lastExecution).not.toBeNull();
      expect(result.referencedTasks[1].lastExecution).toBeNull();
    });
  });

  describe('getTaskEntity', () => {
    it('should filter by userId', async () => {
      mockTaskQueryBuilder.getOne.mockResolvedValue(makeTask());

      await service.getTaskEntity(userId, 1);

      expect(mockTaskQueryBuilder.andWhere).toHaveBeenCalledWith(
        'board.userId = :userId',
        { userId },
      );
    });
  });
});
