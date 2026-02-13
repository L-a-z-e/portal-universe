import {
  ExceptionFilter,
  Catch,
  ArgumentsHost,
  HttpException,
  HttpStatus,
  Logger,
} from '@nestjs/common';
import { Request, Response } from 'express';
import { ApiResponse, FieldError } from '../dto/api-response.dto';
import { BusinessException } from './business.exception';

@Catch()
export class GlobalExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(GlobalExceptionFilter.name);

  catch(exception: unknown, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();
    const request = ctx.getRequest<Request>();

    let status = HttpStatus.INTERNAL_SERVER_ERROR;
    let code = 'C001';
    let message = 'Internal Server Error';
    let details: FieldError[] = [];

    if (exception instanceof BusinessException) {
      status = exception.getStatus();
      code = exception.code;
      message = exception.message;
    } else if (exception instanceof HttpException) {
      status = exception.getStatus();
      const exceptionResponse = exception.getResponse();

      if (typeof exceptionResponse === 'object' && exceptionResponse !== null) {
        const resp = exceptionResponse as Record<string, unknown>;

        // NestJS ValidationPipe returns { message: string[] } for validation errors
        if (status === HttpStatus.BAD_REQUEST && Array.isArray(resp.message)) {
          code = 'C002';
          message = 'Invalid Input Value';
          details = (resp.message as string[]).map((msg) => ({
            field: this.extractField(msg),
            message: msg,
          }));
        } else {
          message = (resp.message as string) || exception.message;
          code = `P${status}`;
        }
      } else {
        message = exception.message;
        code = `P${status}`;
      }
    } else if (exception instanceof Error) {
      message = exception.message;
      this.logger.error(
        `Unhandled exception: ${message}`,
        exception.stack,
        `${request.method} ${request.url}`,
      );
    }

    const errorResponse = ApiResponse.error(code, message, {
      path: request.url,
      details,
    });

    response.status(status).json(errorResponse);
  }

  private extractField(validationMessage: string): string {
    // NestJS class-validator messages often start with the property name
    // e.g., "title should not be empty", "price must be a positive number"
    const match = validationMessage.match(/^(\w+)\s/);
    return match?.[1] ?? 'unknown';
  }
}
