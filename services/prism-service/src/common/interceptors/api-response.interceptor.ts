import {
  Injectable,
  NestInterceptor,
  ExecutionContext,
  CallHandler,
} from '@nestjs/common';
import { Response } from 'express';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse } from '../dto/api-response.dto';

@Injectable()
export class ApiResponseInterceptor<T> implements NestInterceptor<
  T,
  ApiResponse<T> | T
> {
  intercept(
    context: ExecutionContext,
    next: CallHandler<T>,
  ): Observable<ApiResponse<T> | T> {
    return next.handle().pipe(
      map((data: T) => {
        // SSE 응답은 변환하지 않음
        const response = context.switchToHttp().getResponse<Response>();
        const contentType = response.getHeader('Content-Type');
        if (
          typeof contentType === 'string' &&
          contentType.includes('text/event-stream')
        ) {
          return data;
        }

        // 이미 ApiResponse 형태면 그대로 반환
        if (data && typeof data === 'object' && 'success' in data) {
          return data;
        }

        return ApiResponse.success(data);
      }),
    );
  }
}
