import { forwardRef, type HTMLAttributes, type ReactNode, createElement } from 'react';
import type { ContainerProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface ContainerComponentProps
  extends ContainerProps,
    HTMLAttributes<HTMLElement> {
  children?: ReactNode;
}

const maxWidthClasses: Record<NonNullable<ContainerProps['maxWidth']>, string> = {
  sm: 'max-w-screen-sm',
  md: 'max-w-screen-md',
  lg: 'max-w-screen-lg',
  xl: 'max-w-screen-xl',
  '2xl': 'max-w-screen-2xl',
  full: 'max-w-full',
};

const paddingClasses: Record<NonNullable<ContainerProps['padding']>, string> = {
  none: 'px-0',
  sm: 'px-4',
  md: 'px-6',
  lg: 'px-8',
};

export const Container = forwardRef<HTMLElement, ContainerComponentProps>(
  (
    {
      maxWidth = 'lg',
      centered = true,
      padding = 'md',
      as = 'div',
      className,
      children,
      ...props
    },
    ref
  ) => {
    return createElement(
      as,
      {
        ref,
        className: cn(
          'w-full',
          maxWidthClasses[maxWidth],
          paddingClasses[padding],
          centered && 'mx-auto',
          className
        ),
        ...props,
      },
      children
    );
  }
);

Container.displayName = 'Container';

export default Container;
