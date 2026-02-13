import { validate } from 'class-validator';
import { plainToInstance } from 'class-transformer';
import { PaginationDto } from './pagination.dto';
import { ApiResponse } from './api-response.dto';
import { CreateProviderDto } from '../../modules/provider/dto/create-provider.dto';
import { CreateAgentDto } from '../../modules/agent/dto/create-agent.dto';
import { CreateTaskDto } from '../../modules/task/dto/create-task.dto';
import { CreateBoardDto } from '../../modules/board/dto/create-board.dto';
import { ProviderType } from '../../modules/provider/provider.entity';
import { AgentRole } from '../../modules/agent/agent.entity';

describe('PaginationDto', () => {
  it('should have default values page=1, size=20', () => {
    const dto = new PaginationDto();
    expect(dto.page).toBe(1);
    expect(dto.size).toBe(20);
  });

  it('should calculate skip correctly', () => {
    const dto = plainToInstance(PaginationDto, { page: 3, size: 10 });
    expect(dto.skip).toBe(20);
    expect(dto.take).toBe(10);
  });

  it('should validate min/max constraints', async () => {
    const dto = plainToInstance(PaginationDto, { page: 0, size: 200 });
    const errors = await validate(dto);
    expect(errors.length).toBeGreaterThan(0);
  });
});

describe('ApiResponse', () => {
  it('should create success response', () => {
    const response = ApiResponse.success({ id: 1, name: 'test' });
    expect(response.success).toBe(true);
    expect(response.data).toEqual({ id: 1, name: 'test' });
    expect(response.error).toBeNull();
  });

  it('should create error response', () => {
    const response = ApiResponse.error('P001', 'Not found');
    expect(response.success).toBe(false);
    expect(response.data).toBeNull();
    expect(response.error?.code).toBe('P001');
    expect(response.error?.message).toBe('Not found');
    expect(response.error?.timestamp).toBeDefined();
  });

  it('should handle null data in success', () => {
    const response = ApiResponse.success(null);
    expect(response.success).toBe(true);
    expect(response.data).toBeNull();
  });
});

describe('CreateProviderDto', () => {
  it('should pass validation with valid data', async () => {
    const dto = plainToInstance(CreateProviderDto, {
      providerType: ProviderType.OPENAI,
      name: 'My OpenAI',
      apiKey: 'sk-test123',
    });
    const errors = await validate(dto);
    expect(errors).toHaveLength(0);
  });

  it('should fail validation when name is empty', async () => {
    const dto = plainToInstance(CreateProviderDto, {
      providerType: ProviderType.OPENAI,
      name: '',
    });
    const errors = await validate(dto);
    expect(errors.length).toBeGreaterThan(0);
  });
});

describe('CreateAgentDto', () => {
  it('should pass validation with valid data', async () => {
    const dto = plainToInstance(CreateAgentDto, {
      providerId: 1,
      name: 'PM Agent',
      role: AgentRole.PM,
      systemPrompt: 'You are a PM',
      model: 'gpt-4o',
    });
    const errors = await validate(dto);
    expect(errors).toHaveLength(0);
  });

  it('should fail validation with missing required fields', async () => {
    const dto = plainToInstance(CreateAgentDto, {});
    const errors = await validate(dto);
    expect(errors.length).toBeGreaterThan(0);
  });
});

describe('CreateTaskDto', () => {
  it('should pass validation with minimal valid data', async () => {
    const dto = plainToInstance(CreateTaskDto, {
      title: 'Implement feature',
    });
    const errors = await validate(dto);
    expect(errors).toHaveLength(0);
  });

  it('should fail validation when title is empty', async () => {
    const dto = plainToInstance(CreateTaskDto, {
      title: '',
    });
    const errors = await validate(dto);
    expect(errors.length).toBeGreaterThan(0);
  });
});

describe('CreateBoardDto', () => {
  it('should pass validation with valid data', async () => {
    const dto = plainToInstance(CreateBoardDto, {
      name: 'Project Alpha',
    });
    const errors = await validate(dto);
    expect(errors).toHaveLength(0);
  });

  it('should fail validation when name is empty', async () => {
    const dto = plainToInstance(CreateBoardDto, {
      name: '',
    });
    const errors = await validate(dto);
    expect(errors.length).toBeGreaterThan(0);
  });
});
