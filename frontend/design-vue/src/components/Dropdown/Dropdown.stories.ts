import type { Meta, StoryObj } from '@storybook/vue3';
import { Dropdown } from './index';
import { Button } from '../Button';

const meta: Meta<typeof Dropdown> = {
  title: 'Components/Navigation/Dropdown',
  component: Dropdown,
  tags: ['autodocs'],
  argTypes: {
    trigger: {
      control: 'select',
      options: ['click', 'hover'],
    },
    placement: {
      control: 'select',
      options: ['bottom', 'bottom-start', 'bottom-end', 'top', 'top-start', 'top-end'],
    },
    disabled: {
      control: 'boolean',
    },
    closeOnSelect: {
      control: 'boolean',
    },
  },
};

export default meta;
type Story = StoryObj<typeof Dropdown>;

const defaultItems = [
  { label: 'Edit', value: 'edit' },
  { label: 'Duplicate', value: 'duplicate' },
  { label: 'Archive', value: 'archive' },
  { divider: true, label: '' },
  { label: 'Delete', value: 'delete' },
];

export const Default: Story = {
  args: {
    items: defaultItems,
  },
};

export const WithCustomTrigger: Story = {
  render: () => ({
    components: { Dropdown, Button },
    setup() {
      const items = defaultItems;
      const handleSelect = (item: any) => {
        console.log('Selected:', item);
      };
      return { items, handleSelect };
    },
    template: `
      <Dropdown :items="items" @select="handleSelect">
        <template #trigger>
          <Button variant="primary">
            Actions
            <svg class="w-4 h-4 ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </Button>
        </template>
      </Dropdown>
    `,
  }),
};

export const Placement: Story = {
  render: () => ({
    components: { Dropdown, Button },
    setup() {
      const items = [
        { label: 'Option 1', value: '1' },
        { label: 'Option 2', value: '2' },
        { label: 'Option 3', value: '3' },
      ];
      return { items };
    },
    template: `
      <div class="flex gap-4 flex-wrap py-20">
        <Dropdown :items="items" placement="bottom-start">
          <template #trigger>
            <Button variant="secondary">bottom-start</Button>
          </template>
        </Dropdown>
        <Dropdown :items="items" placement="bottom">
          <template #trigger>
            <Button variant="secondary">bottom</Button>
          </template>
        </Dropdown>
        <Dropdown :items="items" placement="bottom-end">
          <template #trigger>
            <Button variant="secondary">bottom-end</Button>
          </template>
        </Dropdown>
      </div>
    `,
  }),
};

export const HoverTrigger: Story = {
  args: {
    items: defaultItems,
    trigger: 'hover',
  },
};

export const WithDisabledItems: Story = {
  render: () => ({
    components: { Dropdown },
    setup() {
      const items = [
        { label: 'View', value: 'view' },
        { label: 'Edit', value: 'edit', disabled: true },
        { label: 'Share', value: 'share' },
        { label: 'Delete', value: 'delete', disabled: true },
      ];
      return { items };
    },
    template: '<Dropdown :items="items" />',
  }),
};

export const Disabled: Story = {
  args: {
    items: defaultItems,
    disabled: true,
  },
};
