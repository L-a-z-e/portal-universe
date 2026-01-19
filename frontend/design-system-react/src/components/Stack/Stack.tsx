import { forwardRef, type HTMLAttributes, type ReactNode, createElement } from 'react';
import type { StackProps } from '@portal/design-types';
import { cn } from '../../utils/cn';

export interface StackComponentProps
  extends StackProps,
    HTMLAttributes<HTMLElement> {
  children?: ReactNode;
}

const gapClasses: Record<NonNullable<StackProps['gap']>, string> = {
  none: 'gap-0',
  xs: 'gap-1',
  sm: 'gap-2',
  md: 'gap-4',
  lg: 'gap-6',
  xl: 'gap-8',
  '2xl': 'gap-12',
};

const alignClasses: Record<NonNullable<StackProps['align']>, string> = {
  start: 'items-start',
  center: 'items-center',
  end: 'items-end',
  stretch: 'items-stretch',
  baseline: 'items-baseline',
};

const justifyClasses: Record<NonNullable<StackProps['justify']>, string> = {
  start: 'justify-start',
  center: 'justify-center',
  end: 'justify-end',
  between: 'justify-between',
  around: 'justify-around',
  evenly: 'justify-evenly',
};

export const Stack = forwardRef<HTMLElement, StackComponentProps>(
  (
    {
      direction = 'vertical',
      gap = 'md',
      align = 'stretch',
      justify = 'start',
      wrap = false,
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
          'flex',
          direction === 'vertical' ? 'flex-col' : 'flex-row',
          gapClasses[gap],
          alignClasses[align],
          justifyClasses[justify],
          wrap && 'flex-wrap',
          className
        ),
        ...props,
      },
      children
    );
  }
);

Stack.displayName = 'Stack';

export default Stack;
