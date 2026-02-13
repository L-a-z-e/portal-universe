import {
  registerDecorator,
  ValidationOptions,
  ValidatorConstraint,
  ValidatorConstraintInterface,
} from 'class-validator';

const SQL_INJECTION_PATTERNS = [
  /(\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|UNION)\b\s)/i,
  /(--|;)\s*(SELECT|INSERT|UPDATE|DELETE|DROP)/i,
  /'\s*(OR|AND)\s+'.*'=/i,
  /'\s*(OR|AND)\s+\d+=\d+/i,
];

@ValidatorConstraint({ name: 'noSqlInjection', async: false })
export class NoSqlInjectionConstraint implements ValidatorConstraintInterface {
  validate(value: unknown): boolean {
    if (typeof value !== 'string') return true;
    return !SQL_INJECTION_PATTERNS.some((pattern) => pattern.test(value));
  }

  defaultMessage(): string {
    return 'Input contains potentially dangerous SQL content';
  }
}

export function NoSqlInjection(options?: ValidationOptions) {
  return function (object: object, propertyName: string) {
    registerDecorator({
      target: object.constructor,
      propertyName,
      options,
      validator: NoSqlInjectionConstraint,
    });
  };
}
