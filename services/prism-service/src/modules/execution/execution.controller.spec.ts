import { Test, TestingModule } from '@nestjs/testing';
import { ExecutionController } from './execution.controller';
import { ExecutionService } from './execution.service';
import { ExecutionResponseDto } from './dto/execution-response.dto';
import { ExecutionStatus } from './execution.entity';

describe('ExecutionController', () => {
  let controller: ExecutionController;
  let executionService: Record<string, jest.Mock>;

  const userId = 'user-123';

  const makeExecutionResponse = (
    overrides?: Partial<ExecutionResponseDto>,
  ): ExecutionResponseDto => {
    const dto = new ExecutionResponseDto();
    dto.id = 1;
    dto.taskId = 1;
    dto.agentId = 1;
    dto.agentName = 'PM Agent';
    dto.executionNumber = 1;
    dto.status = ExecutionStatus.COMPLETED;
    dto.inputPrompt = 'Task: Test';
    dto.outputResult = 'AI generated result';
    dto.userFeedback = null;
    dto.inputTokens = 150;
    dto.outputTokens = 300;
    dto.durationMs = 3000;
    dto.errorMessage = null;
    dto.startedAt = new Date('2024-01-01T00:00:00.000Z');
    dto.completedAt = new Date('2024-01-01T00:00:03.000Z');
    dto.createdAt = new Date('2024-01-01');
    return Object.assign(dto, overrides);
  };

  beforeEach(async () => {
    executionService = {
      findByTask: jest.fn(),
      findOne: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      controllers: [ExecutionController],
      providers: [
        { provide: ExecutionService, useValue: executionService },
      ],
    }).compile();

    controller = module.get<ExecutionController>(ExecutionController);
  });

  describe('findByTask', () => {
    it('should call executionService.findByTask with correct params', async () => {
      const executions = [
        makeExecutionResponse({ executionNumber: 2 }),
        makeExecutionResponse({ id: 2, executionNumber: 1 }),
      ];
      executionService.findByTask.mockResolvedValue(executions);

      const result = await controller.findByTask(userId, 1);

      expect(executionService.findByTask).toHaveBeenCalledWith(userId, 1);
      expect(result).toHaveLength(2);
    });

    it('should return empty array when no executions', async () => {
      executionService.findByTask.mockResolvedValue([]);

      const result = await controller.findByTask(userId, 1);

      expect(result).toEqual([]);
    });
  });

  describe('findOne', () => {
    it('should call executionService.findOne with correct params', async () => {
      const expected = makeExecutionResponse();
      executionService.findOne.mockResolvedValue(expected);

      const result = await controller.findOne(userId, 1);

      expect(executionService.findOne).toHaveBeenCalledWith(userId, 1);
      expect(result).toBe(expected);
    });
  });

  describe('service delegation', () => {
    it('should pass userId from decorator to findByTask', async () => {
      executionService.findByTask.mockResolvedValue([]);
      await controller.findByTask('other-user', 42);
      expect(executionService.findByTask).toHaveBeenCalledWith('other-user', 42);
    });

    it('should pass userId from decorator to findOne', async () => {
      executionService.findOne.mockResolvedValue(makeExecutionResponse());
      await controller.findOne('other-user', 99);
      expect(executionService.findOne).toHaveBeenCalledWith('other-user', 99);
    });
  });
});
