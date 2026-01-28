import {
  Injectable,
  CanActivate,
  ExecutionContext,
  UnauthorizedException,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { Request } from 'express';

export const IS_PUBLIC_KEY = 'isPublic';

@Injectable()
export class JwtAuthGuard implements CanActivate {
  constructor(private reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const isPublic = this.reflector.getAllAndOverride<boolean>(IS_PUBLIC_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);

    if (isPublic) {
      return true;
    }

    const request = context.switchToHttp().getRequest<Request>();

    // API Gateway에서 전달하는 X-User-Id 헤더 확인
    const userId = request.headers['x-user-id'];

    if (!userId) {
      throw new UnauthorizedException('User not authenticated');
    }

    // Request에 user 정보 추가 (userId는 UUID 문자열)
    (request as RequestWithUser).user = {
      id: typeof userId === 'string' ? userId : userId[0],
    };

    return true;
  }
}

export interface RequestWithUser extends Request {
  user: {
    id: string;
  };
}
