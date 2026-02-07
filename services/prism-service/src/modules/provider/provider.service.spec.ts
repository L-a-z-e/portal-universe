import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { ProviderService } from './provider.service';
import { AIProvider, ProviderType } from './provider.entity';
import { EncryptionUtil } from '../../common/utils/encryption.util';
import { BusinessException } from '../../common/filters/business.exception';
import { AIProviderFactory } from '../ai/ai-provider.factory';
import { PaginationDto } from '../../common/dto/pagination.dto';

describe('ProviderService', () => {
  let service: ProviderService;
  let providerRepo: Record<string, jest.Mock>;
  let encryptionUtil: Record<string, jest.Mock>;

  const userId = 'user-123';

  const makeProvider = (overrides?: Partial<AIProvider>): AIProvider =>
    ({
      id: 1,
      userId,
      providerType: ProviderType.OPENAI,
      name: 'Test Provider',
      apiKeyEncrypted: 'iv:tag:encrypted',
      baseUrl: 'https://api.openai.com/v1',
      isActive: true,
      models: null,
      createdAt: new Date('2024-01-01'),
      updatedAt: new Date('2024-01-01'),
      agents: [],
      ...overrides,
    }) as AIProvider;

  beforeEach(async () => {
    providerRepo = {
      findOne: jest.fn(),
      findAndCount: jest.fn(),
      create: jest.fn((data: Partial<AIProvider>) => ({
        ...makeProvider(),
        ...data,
      })),
      save: jest.fn((entity: AIProvider) =>
        Promise.resolve({ ...entity, id: entity.id || 1 }),
      ),
      remove: jest.fn().mockResolvedValue(undefined),
    };

    encryptionUtil = {
      encrypt: jest.fn().mockReturnValue('iv:tag:encrypted'),
      decrypt: jest.fn().mockReturnValue('sk-test-api-key-12345'),
      maskApiKey: jest.fn().mockReturnValue('sk-...2345'),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ProviderService,
        { provide: getRepositoryToken(AIProvider), useValue: providerRepo },
        { provide: EncryptionUtil, useValue: encryptionUtil },
      ],
    }).compile();

    service = module.get<ProviderService>(ProviderService);
  });

  describe('create', () => {
    const dto = {
      providerType: ProviderType.OPENAI,
      name: 'My OpenAI',
      apiKey: 'sk-test-api-key-12345',
    };

    it('should create provider with encrypted apiKey', async () => {
      providerRepo.findOne.mockResolvedValue(null);

      const result = await service.create(userId, dto);

      expect(encryptionUtil.encrypt).toHaveBeenCalledWith('sk-test-api-key-12345');
      expect(providerRepo.save).toHaveBeenCalled();
      expect(result.name).toBe('My OpenAI');
      expect(result.apiKeyMasked).toBe('sk-...2345');
    });

    it('should throw duplicate on existing name', async () => {
      providerRepo.findOne.mockResolvedValue(makeProvider());

      await expect(service.create(userId, dto)).rejects.toThrow(
        BusinessException,
      );
    });
  });

  describe('findAll', () => {
    it('should return paginated providers', async () => {
      const providers = [makeProvider(), makeProvider({ id: 2, name: 'P2' })];
      providerRepo.findAndCount.mockResolvedValue([providers, 2]);

      const pagination = new PaginationDto();
      pagination.page = 1;
      pagination.size = 20;

      const result = await service.findAll(userId, pagination);

      expect(result.items).toHaveLength(2);
      expect(result.totalElements).toBe(2);
      expect(result.page).toBe(1);
      expect(result.size).toBe(20);
    });

    it('should calculate totalPages correctly', async () => {
      providerRepo.findAndCount.mockResolvedValue([
        [makeProvider()],
        25,
      ]);

      const pagination = new PaginationDto();
      pagination.page = 1;
      pagination.size = 10;

      const result = await service.findAll(userId, pagination);

      expect(result.totalPages).toBe(3); // ceil(25/10) = 3
    });

    it('should filter by userId', async () => {
      providerRepo.findAndCount.mockResolvedValue([[], 0]);

      await service.findAll(userId);

      expect(providerRepo.findAndCount).toHaveBeenCalledWith(
        expect.objectContaining({
          where: { userId },
        }),
      );
    });
  });

  describe('findOne', () => {
    it('should return provider with masked apiKey', async () => {
      providerRepo.findOne.mockResolvedValue(makeProvider());

      const result = await service.findOne(userId, 1);

      expect(result.apiKeyMasked).toBe('sk-...2345');
      expect(encryptionUtil.decrypt).toHaveBeenCalledWith('iv:tag:encrypted');
      expect(encryptionUtil.maskApiKey).toHaveBeenCalledWith(
        'sk-test-api-key-12345',
      );
    });

    it('should throw notFound for invalid id', async () => {
      providerRepo.findOne.mockResolvedValue(null);

      await expect(service.findOne(userId, 999)).rejects.toThrow(
        BusinessException,
      );
    });
  });

  describe('update', () => {
    it('should update provider name', async () => {
      const provider = makeProvider();
      // 1st call: findByIdAndUser
      providerRepo.findOne.mockResolvedValueOnce(provider);
      // 2nd call: duplicate check
      providerRepo.findOne.mockResolvedValueOnce(null);

      const result = await service.update(userId, 1, { name: 'Updated' });

      expect(result.name).toBe('Updated');
    });

    it('should update apiKey with encryption', async () => {
      providerRepo.findOne.mockResolvedValue(makeProvider());

      await service.update(userId, 1, { apiKey: 'new-key' });

      expect(encryptionUtil.encrypt).toHaveBeenCalledWith('new-key');
    });

    it('should not update apiKey when not provided', async () => {
      providerRepo.findOne.mockResolvedValueOnce(makeProvider());
      providerRepo.findOne.mockResolvedValueOnce(null);

      await service.update(userId, 1, { name: 'New Name' });

      // encrypt should not be called for apiKey (only in toResponseDto via decrypt)
      expect(encryptionUtil.encrypt).not.toHaveBeenCalled();
    });
  });

  describe('remove', () => {
    it('should delete provider', async () => {
      const provider = makeProvider();
      providerRepo.findOne.mockResolvedValue(provider);

      await service.remove(userId, 1);

      expect(providerRepo.remove).toHaveBeenCalledWith(provider);
    });

    it('should throw notFound on delete', async () => {
      providerRepo.findOne.mockResolvedValue(null);

      await expect(service.remove(userId, 999)).rejects.toThrow(
        BusinessException,
      );
    });
  });

  describe('verify', () => {
    it('should verify connection successfully', async () => {
      const provider = makeProvider();
      providerRepo.findOne.mockResolvedValue(provider);

      const mockLlmProvider = {
        listModels: jest
          .fn()
          .mockResolvedValue(['gpt-4', 'gpt-3.5-turbo']),
      };
      jest
        .spyOn(AIProviderFactory, 'create')
        .mockReturnValue(mockLlmProvider as any);

      const result = await service.verify(userId, 1);

      expect(result.success).toBe(true);
      expect(result.models).toEqual(['gpt-4', 'gpt-3.5-turbo']);
    });

    it('should throw on verify failure', async () => {
      providerRepo.findOne.mockResolvedValue(makeProvider());

      jest.spyOn(AIProviderFactory, 'create').mockReturnValue({
        listModels: jest
          .fn()
          .mockRejectedValue(new Error('Connection refused')),
      } as any);

      await expect(service.verify(userId, 1)).rejects.toThrow(
        BusinessException,
      );
    });

    it('should save models on verify', async () => {
      const provider = makeProvider();
      providerRepo.findOne.mockResolvedValue(provider);

      jest.spyOn(AIProviderFactory, 'create').mockReturnValue({
        listModels: jest.fn().mockResolvedValue(['gpt-4']),
      } as any);

      await service.verify(userId, 1);

      expect(providerRepo.save).toHaveBeenCalledWith(
        expect.objectContaining({ models: ['gpt-4'] }),
      );
    });
  });

  describe('getModels', () => {
    it('should return cached models', async () => {
      const provider = makeProvider({ models: ['gpt-4', 'gpt-3.5-turbo'] });
      providerRepo.findOne.mockResolvedValue(provider);
      const spy = jest.spyOn(AIProviderFactory, 'create');

      const result = await service.getModels(userId, 1);

      expect(result).toEqual(['gpt-4', 'gpt-3.5-turbo']);
      expect(spy).not.toHaveBeenCalled();
    });

    it('should fetch models when cache empty', async () => {
      const provider = makeProvider({ models: null });
      providerRepo.findOne.mockResolvedValue(provider);

      jest.spyOn(AIProviderFactory, 'create').mockReturnValue({
        listModels: jest.fn().mockResolvedValue(['gpt-4']),
      } as any);

      const result = await service.getModels(userId, 1);

      expect(result).toEqual(['gpt-4']);
      expect(providerRepo.save).toHaveBeenCalled();
    });
  });

  describe('getProviderWithApiKey', () => {
    it('should return provider with decrypted key', async () => {
      providerRepo.findOne.mockResolvedValue(makeProvider());

      const result = await service.getProviderWithApiKey(userId, 1);

      expect(result.provider).toBeDefined();
      expect(result.apiKey).toBe('sk-test-api-key-12345');
      expect(encryptionUtil.decrypt).toHaveBeenCalled();
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });
});
