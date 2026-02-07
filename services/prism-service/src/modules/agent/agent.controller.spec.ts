import { Test, TestingModule } from '@nestjs/testing';
import { AgentController } from './agent.controller';
import { AgentService } from './agent.service';
import { AgentResponseDto } from './dto/agent-response.dto';
import { AgentRole } from './agent.entity';
import { PaginationDto, PaginatedResult } from '../../common/dto/pagination.dto';

describe('AgentController', () => {
  let controller: AgentController;
  let agentService: Record<string, jest.Mock>;

  const userId = 'user-123';

  const makeAgentResponse = (
    overrides?: Partial<AgentResponseDto>,
  ): AgentResponseDto => {
    const dto = new AgentResponseDto();
    dto.id = 1;
    dto.userId = userId;
    dto.providerId = 1;
    dto.name = 'Test Agent';
    dto.role = AgentRole.PM;
    dto.description = null;
    dto.systemPrompt = 'You are a PM agent';
    dto.model = 'gpt-4o';
    dto.temperature = 0.7;
    dto.maxTokens = 4096;
    dto.createdAt = new Date('2024-01-01');
    dto.updatedAt = new Date('2024-01-01');
    return Object.assign(dto, overrides);
  };

  beforeEach(async () => {
    agentService = {
      create: jest.fn(),
      findAll: jest.fn(),
      findOne: jest.fn(),
      update: jest.fn(),
      remove: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      controllers: [AgentController],
      providers: [{ provide: AgentService, useValue: agentService }],
    }).compile();

    controller = module.get<AgentController>(AgentController);
  });

  describe('create', () => {
    it('should call service.create and return result', async () => {
      const dto = {
        providerId: 1,
        name: 'New Agent',
        role: AgentRole.BACKEND,
        systemPrompt: 'You are a backend developer',
        model: 'gpt-4o',
      };
      const expected = makeAgentResponse({ name: 'New Agent', role: AgentRole.BACKEND });
      agentService.create.mockResolvedValue(expected);

      const result = await controller.create(userId, dto);

      expect(agentService.create).toHaveBeenCalledWith(userId, dto);
      expect(result).toBe(expected);
    });
  });

  describe('findAll', () => {
    it('should call service.findAll with pagination', async () => {
      const pagination = new PaginationDto();
      const expected: PaginatedResult<AgentResponseDto> = {
        items: [makeAgentResponse()],
        page: 1,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      };
      agentService.findAll.mockResolvedValue(expected);

      const result = await controller.findAll(userId, pagination);

      expect(agentService.findAll).toHaveBeenCalledWith(userId, pagination);
      expect(result).toBe(expected);
    });
  });

  describe('findOne', () => {
    it('should call service.findOne with correct params', async () => {
      const expected = makeAgentResponse();
      agentService.findOne.mockResolvedValue(expected);

      const result = await controller.findOne(userId, 1);

      expect(agentService.findOne).toHaveBeenCalledWith(userId, 1);
      expect(result).toBe(expected);
    });
  });

  describe('update', () => {
    it('should call service.update with correct params', async () => {
      const dto = { name: 'Updated Agent', temperature: 0.9 };
      const expected = makeAgentResponse({ name: 'Updated Agent', temperature: 0.9 });
      agentService.update.mockResolvedValue(expected);

      const result = await controller.update(userId, 1, dto);

      expect(agentService.update).toHaveBeenCalledWith(userId, 1, dto);
      expect(result).toBe(expected);
    });
  });

  describe('remove', () => {
    it('should call service.remove with correct params', async () => {
      agentService.remove.mockResolvedValue(undefined);

      await controller.remove(userId, 1);

      expect(agentService.remove).toHaveBeenCalledWith(userId, 1);
    });
  });

  describe('service delegation', () => {
    it('should pass userId from decorator to service', async () => {
      agentService.findOne.mockResolvedValue(makeAgentResponse());
      await controller.findOne('another-user', 5);
      expect(agentService.findOne).toHaveBeenCalledWith('another-user', 5);
    });

    it('should pass all update dto fields to service', async () => {
      const dto = {
        name: 'Updated',
        role: AgentRole.DEVOPS,
        model: 'claude-3',
        maxTokens: 8192,
      };
      agentService.update.mockResolvedValue(makeAgentResponse(dto));

      await controller.update(userId, 1, dto);

      expect(agentService.update).toHaveBeenCalledWith(userId, 1, dto);
    });

    it('should delegate remove and return void', async () => {
      agentService.remove.mockResolvedValue(undefined);
      const result = await controller.remove(userId, 1);
      expect(result).toBeUndefined();
    });
  });
});
