import { Test, TestingModule } from '@nestjs/testing';
import { TaskController } from './task.controller';
import { TaskService } from './task.service';
import { ExecutionService } from '../execution/execution.service';
import { TaskResponseDto } from './dto/task-response.dto';
import { TaskContextResponseDto } from './dto/task-context.dto';
import { ExecutionResponseDto } from '../execution/dto/execution-response.dto';
import { TaskStatus, TaskPriority } from './task.entity';
import { ExecutionStatus } from '../execution/execution.entity';

describe('TaskController', () => {
  let controller: TaskController;
  let taskService: Record<string, jest.Mock>;
  let executionService: Record<string, jest.Mock>;

  const userId = 'user-123';

  const makeTaskResponse = (
    overrides?: Partial<TaskResponseDto>,
  ): TaskResponseDto => {
    const dto = new TaskResponseDto();
    dto.id = 1;
    dto.boardId = 1;
    dto.agentId = 1;
    dto.title = 'Test Task';
    dto.description = 'Description';
    dto.status = TaskStatus.TODO;
    dto.priority = TaskPriority.MEDIUM;
    dto.position = 0;
    dto.dueDate = null;
    dto.referencedTaskIds = null;
    dto.availableActions = ['execute', 'cancel'];
    dto.createdAt = new Date('2024-01-01');
    dto.updatedAt = new Date('2024-01-01');
    return Object.assign(dto, overrides);
  };

  const makeExecutionResponse = (
    overrides?: Partial<ExecutionResponseDto>,
  ): ExecutionResponseDto => {
    const dto = new ExecutionResponseDto();
    dto.id = 1;
    dto.taskId = 1;
    dto.agentId = 1;
    dto.agentName = 'PM Agent';
    dto.executionNumber = 1;
    dto.status = ExecutionStatus.PENDING;
    dto.inputPrompt = 'Task: Test';
    dto.outputResult = null;
    dto.createdAt = new Date('2024-01-01');
    return Object.assign(dto, overrides);
  };

  beforeEach(async () => {
    taskService = {
      create: jest.fn(),
      findAllByBoard: jest.fn(),
      findOne: jest.fn(),
      update: jest.fn(),
      remove: jest.fn(),
      changePosition: jest.fn(),
      execute: jest.fn(),
      approve: jest.fn(),
      reject: jest.fn(),
      cancel: jest.fn(),
      reopen: jest.fn(),
      getContext: jest.fn(),
    };

    executionService = {
      executeTask: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      controllers: [TaskController],
      providers: [
        { provide: TaskService, useValue: taskService },
        { provide: ExecutionService, useValue: executionService },
      ],
    }).compile();

    controller = module.get<TaskController>(TaskController);
  });

  describe('create', () => {
    it('should call taskService.create with boardId and dto', async () => {
      const dto = { title: 'New Task', description: 'Desc' };
      const expected = makeTaskResponse({ title: 'New Task' });
      taskService.create.mockResolvedValue(expected);

      const result = await controller.create(userId, 1, dto);

      expect(taskService.create).toHaveBeenCalledWith(userId, 1, dto);
      expect(result).toBe(expected);
    });
  });

  describe('findAllByBoard', () => {
    it('should call taskService.findAllByBoard', async () => {
      const tasks = [makeTaskResponse(), makeTaskResponse({ id: 2 })];
      taskService.findAllByBoard.mockResolvedValue(tasks);

      const result = await controller.findAllByBoard(userId, 1);

      expect(taskService.findAllByBoard).toHaveBeenCalledWith(userId, 1);
      expect(result).toHaveLength(2);
    });
  });

  describe('findOne', () => {
    it('should call taskService.findOne', async () => {
      const expected = makeTaskResponse();
      taskService.findOne.mockResolvedValue(expected);

      const result = await controller.findOne(userId, 1);

      expect(taskService.findOne).toHaveBeenCalledWith(userId, 1);
      expect(result).toBe(expected);
    });
  });

  describe('getContext', () => {
    it('should call taskService.getContext', async () => {
      const expected: TaskContextResponseDto = {
        previousExecutions: [],
        referencedTasks: [],
      };
      taskService.getContext.mockResolvedValue(expected);

      const result = await controller.getContext(userId, 1);

      expect(taskService.getContext).toHaveBeenCalledWith(userId, 1);
      expect(result).toBe(expected);
    });
  });

  describe('update', () => {
    it('should call taskService.update', async () => {
      const dto = { title: 'Updated', priority: TaskPriority.HIGH };
      const expected = makeTaskResponse({
        title: 'Updated',
        priority: TaskPriority.HIGH,
      });
      taskService.update.mockResolvedValue(expected);

      const result = await controller.update(userId, 1, dto);

      expect(taskService.update).toHaveBeenCalledWith(userId, 1, dto);
      expect(result).toBe(expected);
    });
  });

  describe('remove', () => {
    it('should call taskService.remove', async () => {
      taskService.remove.mockResolvedValue(undefined);

      await controller.remove(userId, 1);

      expect(taskService.remove).toHaveBeenCalledWith(userId, 1);
    });
  });

  describe('changePosition', () => {
    it('should call taskService.changePosition', async () => {
      const dto = { position: 3 };
      const expected = makeTaskResponse({ position: 3 });
      taskService.changePosition.mockResolvedValue(expected);

      const result = await controller.changePosition(userId, 1, dto);

      expect(taskService.changePosition).toHaveBeenCalledWith(userId, 1, dto);
      expect(result.position).toBe(3);
    });
  });

  describe('execute', () => {
    it('should call taskService.execute then executionService.executeTask', async () => {
      const taskResult = makeTaskResponse({ status: TaskStatus.IN_PROGRESS });
      taskService.execute.mockResolvedValue(taskResult);
      const execResult = makeExecutionResponse();
      executionService.executeTask.mockResolvedValue(execResult);

      const result = await controller.execute(userId, 1);

      expect(taskService.execute).toHaveBeenCalledWith(userId, 1);
      expect(executionService.executeTask).toHaveBeenCalledWith(userId, 1);
      expect(result).toBe(execResult);
    });
  });

  describe('approve', () => {
    it('should call taskService.approve', async () => {
      const expected = makeTaskResponse({ status: TaskStatus.DONE });
      taskService.approve.mockResolvedValue(expected);

      const result = await controller.approve(userId, 1);

      expect(taskService.approve).toHaveBeenCalledWith(userId, 1);
      expect(result.status).toBe(TaskStatus.DONE);
    });
  });

  describe('reject', () => {
    it('should call taskService.reject with feedback', async () => {
      const expected = makeTaskResponse({ status: TaskStatus.IN_PROGRESS });
      taskService.reject.mockResolvedValue(expected);

      const result = await controller.reject(userId, 1, {
        feedback: 'Needs improvement',
      });

      expect(taskService.reject).toHaveBeenCalledWith(
        userId,
        1,
        'Needs improvement',
      );
      expect(result.status).toBe(TaskStatus.IN_PROGRESS);
    });
  });

  describe('cancel', () => {
    it('should call taskService.cancel', async () => {
      const expected = makeTaskResponse({ status: TaskStatus.CANCELLED });
      taskService.cancel.mockResolvedValue(expected);

      const result = await controller.cancel(userId, 1);

      expect(taskService.cancel).toHaveBeenCalledWith(userId, 1);
      expect(result.status).toBe(TaskStatus.CANCELLED);
    });
  });

  describe('reopen', () => {
    it('should call taskService.reopen', async () => {
      const expected = makeTaskResponse({ status: TaskStatus.TODO });
      taskService.reopen.mockResolvedValue(expected);

      const result = await controller.reopen(userId, 1);

      expect(taskService.reopen).toHaveBeenCalledWith(userId, 1);
      expect(result.status).toBe(TaskStatus.TODO);
    });
  });

  describe('service delegation', () => {
    it('should pass correct userId to all methods', async () => {
      taskService.findOne.mockResolvedValue(makeTaskResponse());
      await controller.findOne('different-user', 5);
      expect(taskService.findOne).toHaveBeenCalledWith('different-user', 5);
    });
  });
});
