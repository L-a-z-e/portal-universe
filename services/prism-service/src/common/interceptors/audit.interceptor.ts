import {
  CallHandler,
  ExecutionContext,
  Injectable,
  Logger,
  NestInterceptor,
} from '@nestjs/common';
import { Observable, tap, catchError } from 'rxjs';
import { RequestWithUser } from '../guards/jwt-auth.guard';

@Injectable()
export class AuditInterceptor implements NestInterceptor {
  private readonly logger = new Logger('AuditLog');

  intercept(context: ExecutionContext, next: CallHandler): Observable<unknown> {
    const request = context.switchToHttp().getRequest<RequestWithUser>();
    const { method, url } = request;
    const userId = request.user?.id ?? 'anonymous';
    const start = Date.now();

    return next.handle().pipe(
      tap(() => {
        const duration = Date.now() - start;
        if (this.shouldAudit(method)) {
          this.logger.log({
            userId,
            method,
            path: url,
            duration,
            status: 'success',
          });
        }
      }),
      catchError((err: Error) => {
        const duration = Date.now() - start;
        this.logger.warn({
          userId,
          method,
          path: url,
          duration,
          status: 'error',
          error: err.message,
        });
        throw err;
      }),
    );
  }

  private shouldAudit(method: string): boolean {
    return ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method);
  }
}
