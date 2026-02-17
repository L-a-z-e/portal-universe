export const sidebarBase = [
  'fixed inset-y-0 left-0 z-40',
  'flex flex-col',
  'bg-bg-sidebar',
  'border-r border-border-default',
  'transition-all duration-normal ease-linear-ease',
].join(' ');

export const sidebarNavItem = {
  base: [
    'flex items-center gap-3 px-3 py-2 rounded-lg',
    'text-sm font-medium',
    'transition-all duration-fast ease-linear-ease',
    'cursor-pointer select-none',
  ].join(' '),

  active: [
    'bg-brand-primary/10',
    'text-brand-primary',
    'border border-brand-primary/20',
  ].join(' '),

  inactive: [
    'text-text-meta',
    'border border-transparent',
    'hover:text-text-heading hover:bg-white/5',
    'light:hover:bg-black/5',
  ].join(' '),
};

export const sidebarDivider = 'border-t border-border-default mx-3 my-2';

export const sidebarLogo = [
  'flex items-center gap-3 px-4 py-5',
  'text-text-heading font-semibold text-lg',
].join(' ');
