import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  ParseIntPipe,
  HttpCode,
  HttpStatus,
  Query,
} from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
} from '@nestjs/swagger';
import { AgentService } from './agent.service';
import { CreateAgentDto } from './dto/create-agent.dto';
import { UpdateAgentDto } from './dto/update-agent.dto';
import { AgentResponseDto } from './dto/agent-response.dto';
import { CurrentUserId } from '../../common/decorators/current-user.decorator';
import {
  PaginationDto,
  PaginatedResult,
} from '../../common/dto/pagination.dto';

@ApiTags('AI Agents')
@ApiBearerAuth()
@Controller('agents')
export class AgentController {
  constructor(private readonly agentService: AgentService) {}

  @Post()
  @ApiOperation({ summary: 'Create a new AI agent' })
  @ApiResponse({ status: 201, type: AgentResponseDto })
  async create(
    @CurrentUserId() userId: string,
    @Body() dto: CreateAgentDto,
  ): Promise<AgentResponseDto> {
    return this.agentService.create(userId, dto);
  }

  @Get()
  @ApiOperation({ summary: 'List all agents for the current user' })
  @ApiResponse({ status: 200, type: [AgentResponseDto] })
  async findAll(
    @CurrentUserId() userId: string,
    @Query() pagination: PaginationDto,
  ): Promise<PaginatedResult<AgentResponseDto>> {
    return this.agentService.findAll(userId, pagination);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get agent details' })
  @ApiResponse({ status: 200, type: AgentResponseDto })
  async findOne(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<AgentResponseDto> {
    return this.agentService.findOne(userId, id);
  }

  @Put(':id')
  @ApiOperation({ summary: 'Update an agent' })
  @ApiResponse({ status: 200, type: AgentResponseDto })
  async update(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: UpdateAgentDto,
  ): Promise<AgentResponseDto> {
    return this.agentService.update(userId, id, dto);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Delete an agent' })
  @ApiResponse({ status: 204, description: 'Agent deleted' })
  async remove(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<void> {
    return this.agentService.remove(userId, id);
  }
}
