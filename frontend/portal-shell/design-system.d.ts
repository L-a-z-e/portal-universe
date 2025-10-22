declare module '@portal/design-system' {
  import type { DefineComponent } from 'vue';

  export interface ButtonProps {
    variant?: 'primary' | 'secondary' | 'outline';
    size?: 'sm' | 'md' | 'lg';
    disabled?: boolean;
  }

  export const Button: DefineComponent<ButtonProps>;
}

declare module '@portal/design-system/styles' {
  const styles: string;
  export default styles;
}