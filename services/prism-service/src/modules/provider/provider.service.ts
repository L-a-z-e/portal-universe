import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { AIProvider, ProviderType } from './provider.entity';
import { CreateProviderDto } from './dto/create-provider.dto';
import { UpdateProviderDto } from './dto/update-provider.dto';
import {
  ProviderResponseDto,
  VerifyProviderResponseDto,
} from './dto/provider-response.dto';
import { EncryptionUtil } from '../../common/utils/encryption.util';
import { BusinessException } from '../../common/filters/business.exception';
import { findOneOrThrow } from '../../common/utils/repository.util';
import { AIProviderFactory } from '../ai/ai-provider.factory';
import {
  PaginationDto,
  PaginatedResult,
} from '../../common/dto/pagination.dto';

@Injectable()
export class ProviderService {
  constructor(
    @InjectRepository(AIProvider)
    private readonly providerRepository: Repository<AIProvider>,
    private readonly encryptionUtil: EncryptionUtil,
  ) {}

  async create(
    userId: string,
    dto: CreateProviderDto,
  ): Promise<ProviderResponseDto> {
    // Check for duplicate name
    const existing = await this.providerRepository.findOne({
      where: { userId, name: dto.name },
    });
    if (existing) {
      throw BusinessException.duplicateResource('Provider with this name');
    }

    const provider = this.providerRepository.create({
      userId,
      providerType: dto.providerType,
      name: dto.name,
      apiKeyEncrypted: this.encryptionUtil.encrypt(dto.apiKey || ''),
      baseUrl: dto.baseUrl || this.getDefaultBaseUrl(dto.providerType),
      isActive: true,
    });

    const saved = await this.providerRepository.save(provider);
    return this.toResponseDto(saved);
  }

  async findAll(
    userId: string,
    pagination?: PaginationDto,
  ): Promise<PaginatedResult<ProviderResponseDto>> {
    const [providers, total] = await this.providerRepository.findAndCount({
      where: { userId },
      order: { createdAt: 'DESC' },
      skip: pagination?.skip ?? 0,
      take: pagination?.take ?? 20,
    });
    const items = providers.map((p) => this.toResponseDto(p));
    const size = pagination?.size ?? 20;
    return {
      items,
      total,
      page: pagination?.page ?? 1,
      size,
      totalPages: Math.ceil(total / size),
    };
  }

  async findOne(userId: string, id: number): Promise<ProviderResponseDto> {
    const provider = await this.findByIdAndUser(userId, id);
    return this.toResponseDto(provider);
  }

  async update(
    userId: string,
    id: number,
    dto: UpdateProviderDto,
  ): Promise<ProviderResponseDto> {
    const provider = await this.findByIdAndUser(userId, id);

    if (dto.name !== undefined) {
      // Check for duplicate name (exclude current)
      const existing = await this.providerRepository.findOne({
        where: { userId, name: dto.name },
      });
      if (existing && existing.id !== id) {
        throw BusinessException.duplicateResource('Provider with this name');
      }
      provider.name = dto.name;
    }

    if (dto.apiKey !== undefined) {
      provider.apiKeyEncrypted = this.encryptionUtil.encrypt(dto.apiKey);
    }

    if (dto.baseUrl !== undefined) {
      provider.baseUrl = dto.baseUrl;
    }

    if (dto.isActive !== undefined) {
      provider.isActive = dto.isActive;
    }

    const updated = await this.providerRepository.save(provider);
    return this.toResponseDto(updated);
  }

  async remove(userId: string, id: number): Promise<void> {
    const provider = await this.findByIdAndUser(userId, id);
    await this.providerRepository.remove(provider);
  }

  async verify(userId: string, id: number): Promise<VerifyProviderResponseDto> {
    const provider = await this.findByIdAndUser(userId, id);
    const apiKey = this.encryptionUtil.decrypt(provider.apiKeyEncrypted);

    try {
      const models = await this.fetchModels(
        provider.providerType,
        apiKey,
        provider.baseUrl,
      );

      // Update models in database
      provider.models = models;
      await this.providerRepository.save(provider);

      return {
        success: true,
        message: 'Connection successful',
        models,
      };
    } catch (error) {
      const message =
        error instanceof Error ? error.message : 'Connection failed';
      throw BusinessException.providerConnectionFailed(message);
    }
  }

  async getModels(userId: string, id: number): Promise<string[]> {
    const provider = await this.findByIdAndUser(userId, id);

    if (provider.models && provider.models.length > 0) {
      return provider.models;
    }

    // Fetch fresh models
    const apiKey = this.encryptionUtil.decrypt(provider.apiKeyEncrypted);
    const models = await this.fetchModels(
      provider.providerType,
      apiKey,
      provider.baseUrl,
    );

    provider.models = models;
    await this.providerRepository.save(provider);

    return models;
  }

  // Internal methods
  private async findByIdAndUser(
    userId: string,
    id: number,
  ): Promise<AIProvider> {
    return findOneOrThrow(
      this.providerRepository,
      { where: { id, userId } },
      'Provider',
    );
  }

  /**
   * Get provider entity with decrypted API key (for internal use)
   */
  async getProviderWithApiKey(
    userId: string,
    id: number,
  ): Promise<{ provider: AIProvider; apiKey: string }> {
    const provider = await this.findByIdAndUser(userId, id);
    const apiKey = this.encryptionUtil.decrypt(provider.apiKeyEncrypted);
    return { provider, apiKey };
  }

  private toResponseDto(entity: AIProvider): ProviderResponseDto {
    const decryptedKey = this.encryptionUtil.decrypt(entity.apiKeyEncrypted);
    const maskedKey = this.encryptionUtil.maskApiKey(decryptedKey);
    return ProviderResponseDto.from(entity, maskedKey);
  }

  private getDefaultBaseUrl(type: ProviderType): string | null {
    switch (type) {
      case ProviderType.OPENAI:
        return 'https://api.openai.com/v1';
      case ProviderType.ANTHROPIC:
        return 'https://api.anthropic.com';
      case ProviderType.OLLAMA:
        return 'http://localhost:11434';
      case ProviderType.AZURE_OPENAI:
        return null; // User must provide
      default:
        return null;
    }
  }

  private async fetchModels(
    type: ProviderType,
    apiKey: string,
    baseUrl: string | null,
  ): Promise<string[]> {
    const provider = AIProviderFactory.create(type, apiKey, baseUrl);
    return provider.listModels();
  }
}
