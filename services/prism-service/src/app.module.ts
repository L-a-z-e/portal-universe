import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import configuration from './config/configuration';
import { HealthModule } from './health/health.module';
import { CommonModule } from './common/common.module';
import { ProviderModule } from './modules/provider/provider.module';
import { AgentModule } from './modules/agent/agent.module';
import { BoardModule } from './modules/board/board.module';
import { TaskModule } from './modules/task/task.module';
import { AIModule } from './modules/ai/ai.module';
import { ExecutionModule } from './modules/execution/execution.module';
import { EventModule } from './modules/event/event.module';
import { SseModule } from './modules/sse/sse.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      load: [configuration],
      envFilePath: `.env.${process.env.NODE_ENV || 'local'}`,
    }),
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      useFactory: (configService: ConfigService) => ({
        type: 'postgres',
        host: configService.get('database.host'),
        port: configService.get('database.port'),
        username: configService.get('database.username'),
        password: configService.get('database.password'),
        database: configService.get('database.database'),
        entities: [__dirname + '/**/*.entity{.ts,.js}'],
        synchronize: configService.get('database.synchronize'),
        logging: configService.get('database.logging'),
      }),
      inject: [ConfigService],
    }),
    CommonModule,
    HealthModule,
    ProviderModule,
    AgentModule,
    BoardModule,
    TaskModule,
    AIModule,
    ExecutionModule,
    EventModule,
    SseModule,
  ],
})
export class AppModule {}
