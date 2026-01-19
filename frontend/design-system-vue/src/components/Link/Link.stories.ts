import type { Meta, StoryObj } from '@storybook/vue3';
import { Link } from './index';

const meta: Meta<typeof Link> = {
  title: 'Components/Navigation/Link',
  component: Link,
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['default', 'primary', 'muted', 'underline'],
    },
    size: {
      control: 'select',
      options: ['sm', 'md', 'lg'],
    },
    external: {
      control: 'boolean',
    },
    disabled: {
      control: 'boolean',
    },
  },
};

export default meta;
type Story = StoryObj<typeof Link>;

export const Default: Story = {
  args: {
    href: '#',
  },
  render: (args) => ({
    components: { Link },
    setup() {
      return { args };
    },
    template: '<Link v-bind="args">Default Link</Link>',
  }),
};

export const Variants: Story = {
  render: () => ({
    components: { Link },
    template: `
      <div class="flex flex-col gap-4">
        <Link href="#" variant="default">Default variant</Link>
        <Link href="#" variant="primary">Primary variant</Link>
        <Link href="#" variant="muted">Muted variant</Link>
        <Link href="#" variant="underline">Underline variant</Link>
      </div>
    `,
  }),
};

export const Sizes: Story = {
  render: () => ({
    components: { Link },
    template: `
      <div class="flex items-center gap-4">
        <Link href="#" size="sm">Small Link</Link>
        <Link href="#" size="md">Medium Link</Link>
        <Link href="#" size="lg">Large Link</Link>
      </div>
    `,
  }),
};

export const External: Story = {
  args: {
    href: 'https://example.com',
    external: true,
    target: '_blank',
  },
  render: (args) => ({
    components: { Link },
    setup() {
      return { args };
    },
    template: '<Link v-bind="args">External Link</Link>',
  }),
};

export const Disabled: Story = {
  args: {
    href: '#',
    disabled: true,
  },
  render: (args) => ({
    components: { Link },
    setup() {
      return { args };
    },
    template: '<Link v-bind="args">Disabled Link</Link>',
  }),
};

export const InParagraph: Story = {
  render: () => ({
    components: { Link },
    template: `
      <p class="text-text-body">
        This is a paragraph with a <Link href="#">link inside</Link> it.
        You can also have <Link href="#" variant="primary">primary links</Link> or
        <Link href="https://example.com" external target="_blank">external links</Link>.
      </p>
    `,
  }),
};
