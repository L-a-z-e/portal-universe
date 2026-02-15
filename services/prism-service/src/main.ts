import './instrumentation';

import { NestFactory, Reflector } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { AppModule } from './app.module';
import { GlobalExceptionFilter } from './common/filters/http-exception.filter';
import { ApiResponseInterceptor } from './common/interceptors/api-response.interceptor';
import { AuditInterceptor } from './common/interceptors/audit.interceptor';
import { JwtAuthGuard } from './common/guards/jwt-auth.guard';
import { winstonLogger } from './config/logger.config';

async function bootstrap() {
  const app = await NestFactory.create(AppModule, { logger: winstonLogger });

  // Global prefix
  app.setGlobalPrefix('api/v1');

  // Global pipes
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );

  // Global filters
  app.useGlobalFilters(new GlobalExceptionFilter());

  // Global interceptors
  app.useGlobalInterceptors(
    new AuditInterceptor(),
    new ApiResponseInterceptor(),
  );

  // Global guards
  const reflector = app.get(Reflector);
  app.useGlobalGuards(new JwtAuthGuard(reflector));

  // CORS - API Gateway 통해 접근 시 비활성화 (Gateway가 CORS 처리)
  // Standalone 모드에서만 활성화: ENABLE_CORS=true
  const configService = app.get(ConfigService);
  const corsEnabled = configService.get<boolean>('cors.enabled');
  if (corsEnabled) {
    const origins = configService.get<string[]>('cors.origins');
    const credentials = configService.get<boolean>('cors.credentials');
    app.enableCors({
      origin: origins,
      credentials,
    });
    winstonLogger.log('CORS enabled for standalone mode');
  }

  // Swagger
  const config = new DocumentBuilder()
    .setTitle('Prism Service')
    .setDescription('AI Orchestration Service API')
    .setVersion('1.0')
    .addBearerAuth()
    .build();
  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api-docs', app, document);

  const port = process.env.PORT || 8085;
  await app.listen(port);
  winstonLogger.log(`Prism service running on port ${port}`);
}
void bootstrap();
