import { Module, forwardRef } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Execution } from './execution.entity';
import { Task } from '../task/task.entity';
import { ExecutionController } from './execution.controller';
import { ExecutionService } from './execution.service';
import { TaskModule } from '../task/task.module';
import { AgentModule } from '../agent/agent.module';
import { AIModule } from '../ai/ai.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([Execution, Task]),
    forwardRef(() => TaskModule),
    AgentModule,
    AIModule,
  ],
  controllers: [ExecutionController],
  providers: [ExecutionService],
  exports: [ExecutionService],
})
export class ExecutionModule {}
