import { Module, forwardRef } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Task } from './task.entity';
import { Board } from '../board/board.entity';
import { Agent } from '../agent/agent.entity';
import { Execution } from '../execution/execution.entity';
import { TaskController } from './task.controller';
import { TaskService } from './task.service';
import { ExecutionModule } from '../execution/execution.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([Task, Board, Agent, Execution]),
    forwardRef(() => ExecutionModule),
  ],
  controllers: [TaskController],
  providers: [TaskService],
  exports: [TaskService],
})
export class TaskModule {}
