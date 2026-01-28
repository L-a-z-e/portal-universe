import { Controller, Get, Param, ParseIntPipe } from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
} from '@nestjs/swagger';
import { ExecutionService } from './execution.service';
import { ExecutionResponseDto } from './dto/execution-response.dto';
import { CurrentUserId } from '../../common/decorators/current-user.decorator';

@ApiTags('Executions')
@ApiBearerAuth()
@Controller()
export class ExecutionController {
  constructor(private readonly executionService: ExecutionService) {}

  @Get('tasks/:taskId/executions')
  @ApiOperation({ summary: 'List all executions for a task' })
  @ApiResponse({ status: 200, type: [ExecutionResponseDto] })
  async findByTask(
    @CurrentUserId() userId: string,
    @Param('taskId', ParseIntPipe) taskId: number,
  ): Promise<ExecutionResponseDto[]> {
    return this.executionService.findByTask(userId, taskId);
  }

  @Get('executions/:id')
  @ApiOperation({ summary: 'Get execution details' })
  @ApiResponse({ status: 200, type: ExecutionResponseDto })
  async findOne(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<ExecutionResponseDto> {
    return this.executionService.findOne(userId, id);
  }
}
