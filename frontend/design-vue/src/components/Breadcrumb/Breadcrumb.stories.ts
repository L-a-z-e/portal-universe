import type { Meta, StoryObj } from '@storybook/vue3';
import { Breadcrumb } from './index';

const meta: Meta<typeof Breadcrumb> = {
  title: 'Components/Navigation/Breadcrumb',
  component: Breadcrumb,
  tags: ['autodocs'],
  argTypes: {
    separator: {
      control: 'text',
    },
    size: {
      control: 'select',
      options: ['sm', 'md', 'lg'],
    },
    maxItems: {
      control: { type: 'number', min: 2, max: 10 },
    },
  },
};

export default meta;
type Story = StoryObj<typeof Breadcrumb>;

const defaultItems = [
  { label: 'Home', href: '/' },
  { label: 'Products', href: '/products' },
  { label: 'Electronics', href: '/products/electronics' },
  { label: 'Smartphones' },
];

export const Default: Story = {
  args: {
    items: defaultItems,
  },
};

export const CustomSeparator: Story = {
  args: {
    items: defaultItems,
    separator: '>',
  },
};

export const ChevronSeparator: Story = {
  render: () => ({
    components: { Breadcrumb },
    setup() {
      return { items: defaultItems };
    },
    template: `
      <Breadcrumb :items="items">
        <template #separator>
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </template>
      </Breadcrumb>
    `,
  }),
};

export const Sizes: Story = {
  render: () => ({
    components: { Breadcrumb },
    setup() {
      return { items: defaultItems };
    },
    template: `
      <div class="flex flex-col gap-6">
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Small</h3>
          <Breadcrumb :items="items" size="sm" />
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Medium</h3>
          <Breadcrumb :items="items" size="md" />
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Large</h3>
          <Breadcrumb :items="items" size="lg" />
        </div>
      </div>
    `,
  }),
};

export const Collapsed: Story = {
  render: () => ({
    components: { Breadcrumb },
    setup() {
      const longItems = [
        { label: 'Home', href: '/' },
        { label: 'Products', href: '/products' },
        { label: 'Electronics', href: '/products/electronics' },
        { label: 'Computers', href: '/products/electronics/computers' },
        { label: 'Laptops', href: '/products/electronics/computers/laptops' },
        { label: 'Gaming Laptops' },
      ];
      return { items: longItems };
    },
    template: '<Breadcrumb :items="items" :max-items="3" />',
  }),
};

export const Simple: Story = {
  args: {
    items: [
      { label: 'Home', href: '/' },
      { label: 'Dashboard' },
    ],
  },
};
