import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Agent } from './agent.entity';
import { AIProvider } from '../provider/provider.entity';
import { CreateAgentDto } from './dto/create-agent.dto';
import { UpdateAgentDto } from './dto/update-agent.dto';
import { AgentResponseDto } from './dto/agent-response.dto';
import { BusinessException } from '../../common/filters/business.exception';

@Injectable()
export class AgentService {
  constructor(
    @InjectRepository(Agent)
    private readonly agentRepository: Repository<Agent>,
    @InjectRepository(AIProvider)
    private readonly providerRepository: Repository<AIProvider>,
  ) {}

  async create(userId: string, dto: CreateAgentDto): Promise<AgentResponseDto> {
    // Verify provider exists and belongs to user
    const provider = await this.providerRepository.findOne({
      where: { id: dto.providerId, userId },
    });
    if (!provider) {
      throw BusinessException.notFound('Provider');
    }

    // Check for duplicate name
    const existing = await this.agentRepository.findOne({
      where: { userId, name: dto.name },
    });
    if (existing) {
      throw BusinessException.duplicateResource('Agent with this name');
    }

    const agent = this.agentRepository.create({
      userId,
      providerId: dto.providerId,
      name: dto.name,
      role: dto.role,
      description: dto.description,
      systemPrompt: dto.systemPrompt,
      model: dto.model,
      temperature: dto.temperature ?? 0.7,
      maxTokens: dto.maxTokens ?? 4096,
    });

    const saved = await this.agentRepository.save(agent);

    // Reload with provider relation
    const withProvider = await this.findByIdAndUser(userId, saved.id);
    return AgentResponseDto.from(withProvider);
  }

  async findAll(userId: string): Promise<AgentResponseDto[]> {
    const agents = await this.agentRepository.find({
      where: { userId },
      relations: ['provider'],
      order: { createdAt: 'DESC' },
    });
    return agents.map((a) => AgentResponseDto.from(a));
  }

  async findOne(userId: string, id: number): Promise<AgentResponseDto> {
    const agent = await this.findByIdAndUser(userId, id);
    return AgentResponseDto.from(agent);
  }

  async update(
    userId: string,
    id: number,
    dto: UpdateAgentDto,
  ): Promise<AgentResponseDto> {
    const agent = await this.findByIdAndUser(userId, id);

    if (dto.providerId !== undefined) {
      const provider = await this.providerRepository.findOne({
        where: { id: dto.providerId, userId },
      });
      if (!provider) {
        throw BusinessException.notFound('Provider');
      }
      agent.providerId = dto.providerId;
    }

    if (dto.name !== undefined) {
      const existing = await this.agentRepository.findOne({
        where: { userId, name: dto.name },
      });
      if (existing && existing.id !== id) {
        throw BusinessException.duplicateResource('Agent with this name');
      }
      agent.name = dto.name;
    }

    if (dto.role !== undefined) agent.role = dto.role;
    if (dto.description !== undefined) agent.description = dto.description;
    if (dto.systemPrompt !== undefined) agent.systemPrompt = dto.systemPrompt;
    if (dto.model !== undefined) agent.model = dto.model;
    if (dto.temperature !== undefined) agent.temperature = dto.temperature;
    if (dto.maxTokens !== undefined) agent.maxTokens = dto.maxTokens;

    await this.agentRepository.save(agent);

    const updated = await this.findByIdAndUser(userId, id);
    return AgentResponseDto.from(updated);
  }

  async remove(userId: string, id: number): Promise<void> {
    const agent = await this.findByIdAndUser(userId, id);
    await this.agentRepository.remove(agent);
  }

  /**
   * Get agent entity (for internal use by other services)
   */
  async getAgentEntity(userId: string, id: number): Promise<Agent> {
    return this.findByIdAndUser(userId, id);
  }

  private async findByIdAndUser(userId: string, id: number): Promise<Agent> {
    const agent = await this.agentRepository.findOne({
      where: { id, userId },
      relations: ['provider'],
    });
    if (!agent) {
      throw BusinessException.notFound('Agent');
    }
    return agent;
  }
}
