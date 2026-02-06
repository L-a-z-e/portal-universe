import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Patch,
  Body,
  Param,
  ParseIntPipe,
  HttpCode,
  HttpStatus,
  Inject,
  forwardRef,
} from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
} from '@nestjs/swagger';
import { TaskService } from './task.service';
import { ExecutionService } from '../execution/execution.service';
import { CreateTaskDto } from './dto/create-task.dto';
import { UpdateTaskDto, ChangePositionDto } from './dto/update-task.dto';
import { TaskResponseDto } from './dto/task-response.dto';
import { TaskContextResponseDto } from './dto/task-context.dto';
import { ExecutionResponseDto } from '../execution/dto/execution-response.dto';
import { CurrentUserId } from '../../common/decorators/current-user.decorator';

@ApiTags('Tasks')
@ApiBearerAuth()
@Controller()
export class TaskController {
  constructor(
    private readonly taskService: TaskService,
    @Inject(forwardRef(() => ExecutionService))
    private readonly executionService: ExecutionService,
  ) {}

  @Post('boards/:boardId/tasks')
  @ApiOperation({ summary: 'Create a new task in a board' })
  @ApiResponse({ status: 201, type: TaskResponseDto })
  async create(
    @CurrentUserId() userId: string,
    @Param('boardId', ParseIntPipe) boardId: number,
    @Body() dto: CreateTaskDto,
  ): Promise<TaskResponseDto> {
    return this.taskService.create(userId, boardId, dto);
  }

  @Get('boards/:boardId/tasks')
  @ApiOperation({ summary: 'List all tasks in a board' })
  @ApiResponse({ status: 200, type: [TaskResponseDto] })
  async findAllByBoard(
    @CurrentUserId() userId: string,
    @Param('boardId', ParseIntPipe) boardId: number,
  ): Promise<TaskResponseDto[]> {
    return this.taskService.findAllByBoard(userId, boardId);
  }

  @Get('tasks/:id')
  @ApiOperation({ summary: 'Get task details' })
  @ApiResponse({ status: 200, type: TaskResponseDto })
  async findOne(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<TaskResponseDto> {
    return this.taskService.findOne(userId, id);
  }

  @Get('tasks/:id/context')
  @ApiOperation({
    summary: 'Get execution context for a task',
    description:
      'Returns previous executions and results from referenced tasks',
  })
  @ApiResponse({ status: 200, type: TaskContextResponseDto })
  async getContext(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<TaskContextResponseDto> {
    return this.taskService.getContext(userId, id);
  }

  @Put('tasks/:id')
  @ApiOperation({ summary: 'Update a task' })
  @ApiResponse({ status: 200, type: TaskResponseDto })
  async update(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: UpdateTaskDto,
  ): Promise<TaskResponseDto> {
    return this.taskService.update(userId, id, dto);
  }

  @Delete('tasks/:id')
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Delete a task' })
  @ApiResponse({ status: 204, description: 'Task deleted' })
  async remove(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<void> {
    return this.taskService.remove(userId, id);
  }

  @Patch('tasks/:id/position')
  @ApiOperation({ summary: 'Change task position (order)' })
  @ApiResponse({ status: 200, type: TaskResponseDto })
  async changePosition(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
    @Body() dto: ChangePositionDto,
  ): Promise<TaskResponseDto> {
    return this.taskService.changePosition(userId, id, dto);
  }

  // State transition endpoints
  @Post('tasks/:id/execute')
  @ApiOperation({
    summary: 'Execute task with AI (TODO → IN_PROGRESS → IN_REVIEW)',
    description:
      'Requires agent to be assigned. Creates execution record and calls AI.',
  })
  @ApiResponse({ status: 200, type: ExecutionResponseDto })
  async execute(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<ExecutionResponseDto> {
    // First transition task to IN_PROGRESS
    await this.taskService.execute(userId, id);
    // Then create and run execution
    return this.executionService.executeTask(userId, id);
  }

  @Post('tasks/:id/approve')
  @ApiOperation({ summary: 'Approve task (IN_REVIEW → DONE)' })
  @ApiResponse({ status: 200, type: TaskResponseDto })
  async approve(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<TaskResponseDto> {
    return this.taskService.approve(userId, id);
  }

  @Post('tasks/:id/reject')
  @ApiOperation({
    summary: 'Reject task with feedback (IN_REVIEW → IN_PROGRESS)',
  })
  @ApiResponse({ status: 200, type: TaskResponseDto })
  async reject(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
    @Body() body: { feedback?: string },
  ): Promise<TaskResponseDto> {
    return this.taskService.reject(userId, id, body.feedback);
  }

  @Post('tasks/:id/cancel')
  @ApiOperation({ summary: 'Cancel task' })
  @ApiResponse({ status: 200, type: TaskResponseDto })
  async cancel(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<TaskResponseDto> {
    return this.taskService.cancel(userId, id);
  }

  @Post('tasks/:id/reopen')
  @ApiOperation({ summary: 'Reopen task (DONE/CANCELLED → TODO)' })
  @ApiResponse({ status: 200, type: TaskResponseDto })
  async reopen(
    @CurrentUserId() userId: string,
    @Param('id', ParseIntPipe) id: number,
  ): Promise<TaskResponseDto> {
    return this.taskService.reopen(userId, id);
  }
}
