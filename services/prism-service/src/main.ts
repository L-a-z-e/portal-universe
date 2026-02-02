import { NestFactory, Reflector } from '@nestjs/core';
import { Logger, ValidationPipe } from '@nestjs/common';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { AppModule } from './app.module';
import { GlobalExceptionFilter } from './common/filters/http-exception.filter';
import { ApiResponseInterceptor } from './common/interceptors/api-response.interceptor';
import { JwtAuthGuard } from './common/guards/jwt-auth.guard';

const logger = new Logger('Bootstrap');

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

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
  app.useGlobalInterceptors(new ApiResponseInterceptor());

  // Global guards
  const reflector = app.get(Reflector);
  app.useGlobalGuards(new JwtAuthGuard(reflector));

  // CORS - API Gateway 통해 접근 시 비활성화 (Gateway가 CORS 처리)
  // Standalone 모드에서만 활성화: ENABLE_CORS=true
  if (process.env.ENABLE_CORS === 'true') {
    app.enableCors({
      origin: process.env.CORS_ORIGINS?.split(',') || [
        'http://localhost:30000',
        'http://localhost:30003',
      ],
      credentials: true,
    });
    logger.log('CORS enabled for standalone mode');
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
  logger.log(`Prism service running on port ${port}`);
}
void bootstrap();
