import { JwtAuthGuard, IS_PUBLIC_KEY, RequestWithUser } from './jwt-auth.guard';
import { Reflector } from '@nestjs/core';
import { ExecutionContext, UnauthorizedException } from '@nestjs/common';

describe('JwtAuthGuard', () => {
  let guard: JwtAuthGuard;
  let reflector: jest.Mocked<Reflector>;

  function createMockContext(
    headers: Record<string, string | undefined> = {},
  ): {
    context: ExecutionContext;
    request: Partial<RequestWithUser>;
  } {
    const request: Partial<RequestWithUser> = {
      headers: headers as any,
    };
    const context = {
      getHandler: jest.fn(),
      getClass: jest.fn(),
      switchToHttp: jest.fn().mockReturnValue({
        getRequest: jest.fn().mockReturnValue(request),
      }),
    } as unknown as ExecutionContext;
    return { context, request };
  }

  beforeEach(() => {
    reflector = {
      getAllAndOverride: jest.fn().mockReturnValue(false),
    } as unknown as jest.Mocked<Reflector>;
    guard = new JwtAuthGuard(reflector);
  });

  it('should allow request with valid X-User-Id header', () => {
    const { context, request } = createMockContext({
      'x-user-id': 'user-123',
    });

    const result = guard.canActivate(context);

    expect(result).toBe(true);
    expect((request as RequestWithUser).user.id).toBe('user-123');
  });

  it('should throw UnauthorizedException without X-User-Id header', () => {
    const { context } = createMockContext({});

    expect(() => guard.canActivate(context)).toThrow(UnauthorizedException);
  });

  it('should bypass auth when @Public() decorator is present', () => {
    reflector.getAllAndOverride.mockReturnValue(true);
    const { context } = createMockContext({}); // no user id

    const result = guard.canActivate(context);

    expect(result).toBe(true);
    expect(reflector.getAllAndOverride).toHaveBeenCalledWith(IS_PUBLIC_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);
  });

  it('should parse roles from comma-separated X-User-Roles header', () => {
    const { context, request } = createMockContext({
      'x-user-id': 'user-123',
      'x-user-roles': 'ROLE_USER,ROLE_ADMIN',
    });

    guard.canActivate(context);

    expect((request as RequestWithUser).user.roles).toEqual([
      'ROLE_USER',
      'ROLE_ADMIN',
    ]);
  });

  it('should parse roles with spaces (trimmed)', () => {
    const { context, request } = createMockContext({
      'x-user-id': 'user-123',
      'x-user-roles': ' ROLE_USER , ROLE_SELLER ',
    });

    guard.canActivate(context);

    expect((request as RequestWithUser).user.roles).toEqual([
      'ROLE_USER',
      'ROLE_SELLER',
    ]);
  });

  it('should set empty roles when X-User-Roles is not provided', () => {
    const { context, request } = createMockContext({
      'x-user-id': 'user-123',
    });

    guard.canActivate(context);

    expect((request as RequestWithUser).user.roles).toEqual([]);
  });

  it('should parse memberships from X-User-Memberships JSON header', () => {
    const { context, request } = createMockContext({
      'x-user-id': 'user-123',
      'x-user-memberships': '{"shopping":"FREE","blog":"PREMIUM"}',
    });

    guard.canActivate(context);

    expect((request as RequestWithUser).user.memberships).toEqual({
      shopping: 'FREE',
      blog: 'PREMIUM',
    });
  });

  it('should set empty memberships for invalid JSON', () => {
    const { context, request } = createMockContext({
      'x-user-id': 'user-123',
      'x-user-memberships': 'invalid-json',
    });

    guard.canActivate(context);

    expect((request as RequestWithUser).user.memberships).toEqual({});
  });

  it('should set empty memberships when header is not provided', () => {
    const { context, request } = createMockContext({
      'x-user-id': 'user-123',
    });

    guard.canActivate(context);

    expect((request as RequestWithUser).user.memberships).toEqual({});
  });

  it('should attach complete user object to request', () => {
    const { context, request } = createMockContext({
      'x-user-id': 'user-456',
      'x-user-roles': 'ROLE_USER',
      'x-user-memberships': '{"prism":"PRO"}',
    });

    guard.canActivate(context);

    const user = (request as RequestWithUser).user;
    expect(user).toEqual({
      id: 'user-456',
      roles: ['ROLE_USER'],
      memberships: { prism: 'PRO' },
    });
  });
});
