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
import { BusinessException, PrismErrorCode } from './business.exception';

@Catch()
export class GlobalExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(GlobalExceptionFilter.name);

  catch(exception: unknown, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();
    const request = ctx.getRequest<Request>();

    let status = HttpStatus.INTERNAL_SERVER_ERROR;
    let code: string = PrismErrorCode.INTERNAL_ERROR;
    let message = 'Internal Server Error';
    let details: FieldError[] = [];

    if (exception instanceof BusinessException) {
      status = exception.getStatus();
      code = exception.code;
      message = exception.message;
    } else if (exception instanceof HttpException) {
      status = exception.getStatus();
      code = this.resolveHttpErrorCode(status);
      const exceptionResponse = exception.getResponse();

      if (typeof exceptionResponse === 'object' && exceptionResponse !== null) {
        const resp = exceptionResponse as Record<string, unknown>;

        if (status === HttpStatus.BAD_REQUEST && Array.isArray(resp.message)) {
          code = PrismErrorCode.VALIDATION_ERROR;
          message = 'Invalid Input Value';
          details = (resp.message as string[]).map((msg) => ({
            field: this.extractField(msg),
            message: msg,
          }));
        } else {
          message = (resp.message as string) || exception.message;
        }
      } else {
        message = exception.message;
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

  private resolveHttpErrorCode(status: number): string {
    switch (status) {
      case HttpStatus.UNAUTHORIZED:
        return PrismErrorCode.UNAUTHORIZED;
      case HttpStatus.FORBIDDEN:
        return PrismErrorCode.FORBIDDEN;
      case HttpStatus.NOT_FOUND:
        return PrismErrorCode.NOT_FOUND;
      case HttpStatus.BAD_REQUEST:
        return PrismErrorCode.INVALID_REQUEST;
      default:
        return PrismErrorCode.INTERNAL_ERROR;
    }
  }

  private extractField(validationMessage: string): string {
    const match = validationMessage.match(/^(\w+)\s/);
    return match?.[1] ?? 'unknown';
  }
}
