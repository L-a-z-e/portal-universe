import {
  registerDecorator,
  ValidationOptions,
  ValidatorConstraint,
  ValidatorConstraintInterface,
} from 'class-validator';

const XSS_PATTERNS = [
  /<script\b[^>]*>/i,
  /javascript:/i,
  /on\w+\s*=/i,
  /data:\s*text\/html/i,
  /vbscript:/i,
  /expression\s*\(/i,
];

@ValidatorConstraint({ name: 'noXss', async: false })
export class NoXssConstraint implements ValidatorConstraintInterface {
  validate(value: unknown): boolean {
    if (typeof value !== 'string') return true;
    return !XSS_PATTERNS.some((pattern) => pattern.test(value));
  }

  defaultMessage(): string {
    return 'Input contains potentially dangerous content';
  }
}

export function NoXss(options?: ValidationOptions) {
  return function (object: object, propertyName: string) {
    registerDecorator({
      target: object.constructor,
      propertyName,
      options,
      validator: NoXssConstraint,
    });
  };
}
