import type { Meta, StoryObj } from '@storybook/vue3';
import { ref } from 'vue';
import { Tabs } from './index';

const meta: Meta<typeof Tabs> = {
  title: 'Components/Navigation/Tabs',
  component: Tabs,
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['default', 'pills', 'underline'],
    },
    size: {
      control: 'select',
      options: ['sm', 'md', 'lg'],
    },
    fullWidth: {
      control: 'boolean',
    },
  },
};

export default meta;
type Story = StoryObj<typeof Tabs>;

const defaultItems = [
  { label: 'Profile', value: 'profile' },
  { label: 'Settings', value: 'settings' },
  { label: 'Notifications', value: 'notifications' },
];

export const Default: Story = {
  render: () => ({
    components: { Tabs },
    setup() {
      const active = ref('profile');
      return { active, items: defaultItems };
    },
    template: '<Tabs v-model="active" :items="items" />',
  }),
};

export const Pills: Story = {
  render: () => ({
    components: { Tabs },
    setup() {
      const active = ref('profile');
      return { active, items: defaultItems };
    },
    template: '<Tabs v-model="active" :items="items" variant="pills" />',
  }),
};

export const Underline: Story = {
  render: () => ({
    components: { Tabs },
    setup() {
      const active = ref('profile');
      return { active, items: defaultItems };
    },
    template: '<Tabs v-model="active" :items="items" variant="underline" />',
  }),
};

export const AllVariants: Story = {
  render: () => ({
    components: { Tabs },
    setup() {
      const defaultTab = ref('profile');
      const pillsTab = ref('profile');
      const underlineTab = ref('profile');
      return { defaultTab, pillsTab, underlineTab, items: defaultItems };
    },
    template: `
      <div class="flex flex-col gap-8">
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Default</h3>
          <Tabs v-model="defaultTab" :items="items" variant="default" />
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Pills</h3>
          <Tabs v-model="pillsTab" :items="items" variant="pills" />
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Underline</h3>
          <Tabs v-model="underlineTab" :items="items" variant="underline" />
        </div>
      </div>
    `,
  }),
};

export const Sizes: Story = {
  render: () => ({
    components: { Tabs },
    setup() {
      const sm = ref('profile');
      const md = ref('profile');
      const lg = ref('profile');
      return { sm, md, lg, items: defaultItems };
    },
    template: `
      <div class="flex flex-col gap-6">
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Small</h3>
          <Tabs v-model="sm" :items="items" size="sm" />
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Medium</h3>
          <Tabs v-model="md" :items="items" size="md" />
        </div>
        <div>
          <h3 class="text-sm font-medium text-text-muted mb-2">Large</h3>
          <Tabs v-model="lg" :items="items" size="lg" />
        </div>
      </div>
    `,
  }),
};

export const FullWidth: Story = {
  render: () => ({
    components: { Tabs },
    setup() {
      const active = ref('profile');
      return { active, items: defaultItems };
    },
    template: '<Tabs v-model="active" :items="items" full-width />',
  }),
};

export const WithDisabledTab: Story = {
  render: () => ({
    components: { Tabs },
    setup() {
      const active = ref('profile');
      const items = [
        { label: 'Profile', value: 'profile' },
        { label: 'Settings', value: 'settings' },
        { label: 'Billing', value: 'billing', disabled: true },
        { label: 'Notifications', value: 'notifications' },
      ];
      return { active, items };
    },
    template: '<Tabs v-model="active" :items="items" />',
  }),
};
