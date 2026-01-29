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

    // X-User-Roles 파싱 (comma-separated: "ROLE_USER,ROLE_SELLER")
    const rolesHeader = request.headers['x-user-roles'];
    const roles: string[] = typeof rolesHeader === 'string'
      ? rolesHeader.split(',').map(r => r.trim()).filter(Boolean)
      : [];

    // X-User-Memberships 파싱 (Gateway에서 JSON 문자열로 전달: '{"shopping":"FREE","blog":"PREMIUM"}')
    const membershipsHeader = request.headers['x-user-memberships'];
    let memberships: Record<string, string> = {};
    if (typeof membershipsHeader === 'string') {
      try {
        memberships = JSON.parse(membershipsHeader);
      } catch {
        // fallback: ignore invalid JSON
      }
    }

    // Request에 user 정보 추가
    (request as RequestWithUser).user = {
      id: typeof userId === 'string' ? userId : userId[0],
      roles,
      memberships,
    };

    return true;
  }
}

export interface RequestWithUser extends Request {
  user: {
    id: string;
    roles: string[];
    memberships: Record<string, string>;
  };
}
