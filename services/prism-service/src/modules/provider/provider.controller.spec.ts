import { Test, TestingModule } from '@nestjs/testing';
import { ProviderController } from './provider.controller';
import { ProviderService } from './provider.service';
import { ProviderResponseDto, VerifyProviderResponseDto } from './dto/provider-response.dto';
import { ProviderType } from './provider.entity';
import { PaginationDto, PaginatedResult } from '../../common/dto/pagination.dto';

describe('ProviderController', () => {
  let controller: ProviderController;
  let providerService: Record<string, jest.Mock>;

  const userId = 'user-123';

  const makeProviderResponse = (
    overrides?: Partial<ProviderResponseDto>,
  ): ProviderResponseDto => {
    const dto = new ProviderResponseDto();
    dto.id = 1;
    dto.userId = userId;
    dto.providerType = ProviderType.OPENAI;
    dto.name = 'Test Provider';
    dto.apiKeyMasked = 'sk-...2345';
    dto.baseUrl = 'https://api.openai.com/v1';
    dto.isActive = true;
    dto.models = null;
    dto.createdAt = new Date('2024-01-01');
    dto.updatedAt = new Date('2024-01-01');
    return Object.assign(dto, overrides);
  };

  beforeEach(async () => {
    providerService = {
      create: jest.fn(),
      findAll: jest.fn(),
      findOne: jest.fn(),
      update: jest.fn(),
      remove: jest.fn(),
      verify: jest.fn(),
      getModels: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      controllers: [ProviderController],
      providers: [
        { provide: ProviderService, useValue: providerService },
      ],
    }).compile();

    controller = module.get<ProviderController>(ProviderController);
  });

  describe('create', () => {
    it('should call service.create and return result', async () => {
      const dto = {
        providerType: ProviderType.OPENAI,
        name: 'My OpenAI',
        apiKey: 'sk-test',
      };
      const expected = makeProviderResponse({ name: 'My OpenAI' });
      providerService.create.mockResolvedValue(expected);

      const result = await controller.create(userId, dto);

      expect(providerService.create).toHaveBeenCalledWith(userId, dto);
      expect(result).toBe(expected);
    });
  });

  describe('findAll', () => {
    it('should call service.findAll with pagination', async () => {
      const pagination = new PaginationDto();
      const expected: PaginatedResult<ProviderResponseDto> = {
        items: [makeProviderResponse()],
        page: 1,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      };
      providerService.findAll.mockResolvedValue(expected);

      const result = await controller.findAll(userId, pagination);

      expect(providerService.findAll).toHaveBeenCalledWith(userId, pagination);
      expect(result).toBe(expected);
    });
  });

  describe('findOne', () => {
    it('should call service.findOne with correct params', async () => {
      const expected = makeProviderResponse();
      providerService.findOne.mockResolvedValue(expected);

      const result = await controller.findOne(userId, 1);

      expect(providerService.findOne).toHaveBeenCalledWith(userId, 1);
      expect(result).toBe(expected);
    });
  });

  describe('update', () => {
    it('should call service.update with correct params', async () => {
      const dto = { name: 'Updated Name' };
      const expected = makeProviderResponse({ name: 'Updated Name' });
      providerService.update.mockResolvedValue(expected);

      const result = await controller.update(userId, 1, dto);

      expect(providerService.update).toHaveBeenCalledWith(userId, 1, dto);
      expect(result).toBe(expected);
    });
  });

  describe('remove', () => {
    it('should call service.remove with correct params', async () => {
      providerService.remove.mockResolvedValue(undefined);

      await controller.remove(userId, 1);

      expect(providerService.remove).toHaveBeenCalledWith(userId, 1);
    });
  });

  describe('verify', () => {
    it('should call service.verify and return result', async () => {
      const expected: VerifyProviderResponseDto = {
        success: true,
        message: 'Connection successful',
        models: ['gpt-4'],
      };
      providerService.verify.mockResolvedValue(expected);

      const result = await controller.verify(userId, 1);

      expect(providerService.verify).toHaveBeenCalledWith(userId, 1);
      expect(result).toBe(expected);
    });
  });

  describe('getModels', () => {
    it('should call service.getModels and return result', async () => {
      const expected = ['gpt-4', 'gpt-3.5-turbo'];
      providerService.getModels.mockResolvedValue(expected);

      const result = await controller.getModels(userId, 1);

      expect(providerService.getModels).toHaveBeenCalledWith(userId, 1);
      expect(result).toEqual(expected);
    });
  });

  describe('method count', () => {
    it('should have 7 methods', () => {
      expect(controller.create).toBeDefined();
      expect(controller.findAll).toBeDefined();
      expect(controller.findOne).toBeDefined();
      expect(controller.update).toBeDefined();
      expect(controller.remove).toBeDefined();
      expect(controller.verify).toBeDefined();
      expect(controller.getModels).toBeDefined();
    });
  });

  describe('service delegation', () => {
    it('should delegate findOne to service with userId and id', async () => {
      providerService.findOne.mockResolvedValue(makeProviderResponse());
      await controller.findOne('different-user', 42);
      expect(providerService.findOne).toHaveBeenCalledWith('different-user', 42);
    });

    it('should delegate remove to service with userId and id', async () => {
      providerService.remove.mockResolvedValue(undefined);
      await controller.remove('different-user', 42);
      expect(providerService.remove).toHaveBeenCalledWith('different-user', 42);
    });
  });
});
