import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { AgentService } from './agent.service';
import { Agent, AgentRole } from './agent.entity';
import { AIProvider, ProviderType } from '../provider/provider.entity';
import { BusinessException } from '../../common/filters/business.exception';
import { PaginationDto } from '../../common/dto/pagination.dto';

describe('AgentService', () => {
  let service: AgentService;
  let agentRepo: Record<string, jest.Mock>;
  let providerRepo: Record<string, jest.Mock>;

  const userId = 'user-123';

  const makeProvider = (overrides?: Partial<AIProvider>): AIProvider =>
    ({
      id: 1,
      userId,
      providerType: ProviderType.OPENAI,
      name: 'OpenAI Provider',
      apiKeyEncrypted: 'encrypted',
      baseUrl: 'https://api.openai.com/v1',
      isActive: true,
      models: null,
      createdAt: new Date('2024-01-01'),
      updatedAt: new Date('2024-01-01'),
      agents: [],
      ...overrides,
    }) as AIProvider;

  const makeAgent = (overrides?: Partial<Agent>): Agent =>
    ({
      id: 1,
      userId,
      providerId: 1,
      provider: makeProvider(),
      name: 'Test Agent',
      role: AgentRole.PM,
      description: 'Test description',
      systemPrompt: 'You are a PM agent',
      model: 'gpt-4o',
      temperature: 0.7,
      maxTokens: 4096,
      createdAt: new Date('2024-01-01'),
      updatedAt: new Date('2024-01-01'),
      tasks: [],
      executions: [],
      ...overrides,
    }) as Agent;

  beforeEach(async () => {
    agentRepo = {
      findOne: jest.fn(),
      findAndCount: jest.fn(),
      create: jest.fn((data: Partial<Agent>) => ({ ...makeAgent(), ...data })),
      save: jest.fn((entity: Agent) =>
        Promise.resolve({ ...entity, id: entity.id || 1 }),
      ),
      remove: jest.fn().mockResolvedValue(undefined),
    };

    providerRepo = {
      findOne: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AgentService,
        { provide: getRepositoryToken(Agent), useValue: agentRepo },
        { provide: getRepositoryToken(AIProvider), useValue: providerRepo },
      ],
    }).compile();

    service = module.get<AgentService>(AgentService);
  });

  describe('create', () => {
    const dto = {
      providerId: 1,
      name: 'PM Agent',
      role: AgentRole.PM,
      systemPrompt: 'You are a PM agent',
      model: 'gpt-4o',
    };

    it('should create agent with provider', async () => {
      providerRepo.findOne.mockResolvedValue(makeProvider());
      // 1st agentRepo.findOne: duplicate check -> null
      agentRepo.findOne
        .mockResolvedValueOnce(null)
        // 2nd agentRepo.findOne: findByIdAndUser reload
        .mockResolvedValueOnce(makeAgent({ name: 'PM Agent' }));

      const result = await service.create(userId, dto);

      expect(result.name).toBe('PM Agent');
      expect(result.providerId).toBe(1);
      expect(result.provider).toBeDefined();
      expect(agentRepo.save).toHaveBeenCalled();
    });

    it('should throw notFound for invalid provider', async () => {
      providerRepo.findOne.mockResolvedValue(null);

      await expect(service.create(userId, dto)).rejects.toThrow(
        BusinessException,
      );
    });

    it('should throw duplicate on existing name', async () => {
      providerRepo.findOne.mockResolvedValue(makeProvider());
      agentRepo.findOne.mockResolvedValue(makeAgent()); // duplicate

      await expect(service.create(userId, dto)).rejects.toThrow(
        BusinessException,
      );
    });

    it('should set default temperature and maxTokens', async () => {
      providerRepo.findOne.mockResolvedValue(makeProvider());
      agentRepo.findOne
        .mockResolvedValueOnce(null)
        .mockResolvedValueOnce(makeAgent());

      await service.create(userId, {
        providerId: 1,
        name: 'Agent',
        role: AgentRole.CUSTOM,
        systemPrompt: 'prompt',
        model: 'gpt-4o',
        // temperature and maxTokens not provided
      });

      expect(agentRepo.create).toHaveBeenCalledWith(
        expect.objectContaining({
          temperature: 0.7,
          maxTokens: 4096,
        }),
      );
    });
  });

  describe('findAll', () => {
    it('should return paginated agents', async () => {
      const agents = [makeAgent(), makeAgent({ id: 2, name: 'Agent 2' })];
      agentRepo.findAndCount.mockResolvedValue([agents, 2]);

      const pagination = new PaginationDto();
      pagination.page = 1;
      pagination.size = 20;

      const result = await service.findAll(userId, pagination);

      expect(result.items).toHaveLength(2);
      expect(result.total).toBe(2);
    });

    it('should include provider relation', async () => {
      agentRepo.findAndCount.mockResolvedValue([[makeAgent()], 1]);

      await service.findAll(userId);

      expect(agentRepo.findAndCount).toHaveBeenCalledWith(
        expect.objectContaining({
          relations: ['provider'],
        }),
      );
    });

    it('should filter by userId', async () => {
      agentRepo.findAndCount.mockResolvedValue([[], 0]);

      await service.findAll(userId);

      expect(agentRepo.findAndCount).toHaveBeenCalledWith(
        expect.objectContaining({
          where: { userId },
        }),
      );
    });
  });

  describe('findOne', () => {
    it('should return single agent', async () => {
      agentRepo.findOne.mockResolvedValue(makeAgent());

      const result = await service.findOne(userId, 1);

      expect(result.id).toBe(1);
      expect(result.name).toBe('Test Agent');
    });

    it('should throw notFound for invalid id', async () => {
      agentRepo.findOne.mockResolvedValue(null);

      await expect(service.findOne(userId, 999)).rejects.toThrow(
        BusinessException,
      );
    });
  });

  describe('update', () => {
    it('should update agent fields', async () => {
      // findByIdAndUser
      agentRepo.findOne.mockResolvedValueOnce(makeAgent());
      // duplicate check
      agentRepo.findOne.mockResolvedValueOnce(null);
      // reload after save
      agentRepo.findOne.mockResolvedValueOnce(
        makeAgent({ name: 'Updated', description: 'New desc' }),
      );

      const result = await service.update(userId, 1, {
        name: 'Updated',
        description: 'New desc',
      });

      expect(result.name).toBe('Updated');
      expect(result.description).toBe('New desc');
    });

    it('should update provider reference', async () => {
      agentRepo.findOne
        .mockResolvedValueOnce(makeAgent())
        .mockResolvedValueOnce(makeAgent({ providerId: 2 }));
      providerRepo.findOne.mockResolvedValue(makeProvider({ id: 2 }));

      const result = await service.update(userId, 1, { providerId: 2 });

      expect(result.providerId).toBe(2);
    });
  });

  describe('remove', () => {
    it('should delete agent', async () => {
      const agent = makeAgent();
      agentRepo.findOne.mockResolvedValue(agent);

      await service.remove(userId, 1);

      expect(agentRepo.remove).toHaveBeenCalledWith(agent);
    });

    it('should throw notFound on delete', async () => {
      agentRepo.findOne.mockResolvedValue(null);

      await expect(service.remove(userId, 999)).rejects.toThrow(
        BusinessException,
      );
    });
  });

  describe('getAgentEntity', () => {
    it('should return agent entity', async () => {
      const agent = makeAgent();
      agentRepo.findOne.mockResolvedValue(agent);

      const result = await service.getAgentEntity(userId, 1);

      expect(result).toBe(agent);
    });
  });
});
