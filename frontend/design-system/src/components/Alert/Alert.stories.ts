import type { Meta, StoryObj } from '@storybook/vue3';
import { Alert } from './index';

const meta: Meta<typeof Alert> = {
  title: 'Components/Feedback/Alert',
  component: Alert,
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['info', 'success', 'warning', 'error'],
    },
    dismissible: {
      control: 'boolean',
    },
    showIcon: {
      control: 'boolean',
    },
    bordered: {
      control: 'boolean',
    },
  },
};

export default meta;
type Story = StoryObj<typeof Alert>;

export const Info: Story = {
  args: {
    variant: 'info',
    title: 'Information',
  },
  render: (args) => ({
    components: { Alert },
    setup() {
      return { args };
    },
    template: '<Alert v-bind="args">This is an informational message.</Alert>',
  }),
};

export const Success: Story = {
  args: {
    variant: 'success',
    title: 'Success',
  },
  render: (args) => ({
    components: { Alert },
    setup() {
      return { args };
    },
    template: '<Alert v-bind="args">Your changes have been saved successfully.</Alert>',
  }),
};

export const Warning: Story = {
  args: {
    variant: 'warning',
    title: 'Warning',
  },
  render: (args) => ({
    components: { Alert },
    setup() {
      return { args };
    },
    template: '<Alert v-bind="args">Please review your input before proceeding.</Alert>',
  }),
};

export const Error: Story = {
  args: {
    variant: 'error',
    title: 'Error',
  },
  render: (args) => ({
    components: { Alert },
    setup() {
      return { args };
    },
    template: '<Alert v-bind="args">An error occurred while processing your request.</Alert>',
  }),
};

export const AllVariants: Story = {
  render: () => ({
    components: { Alert },
    template: `
      <div class="flex flex-col gap-4">
        <Alert variant="info" title="Information">This is an informational message.</Alert>
        <Alert variant="success" title="Success">Operation completed successfully.</Alert>
        <Alert variant="warning" title="Warning">Please proceed with caution.</Alert>
        <Alert variant="error" title="Error">Something went wrong.</Alert>
      </div>
    `,
  }),
};

export const Dismissible: Story = {
  args: {
    variant: 'info',
    dismissible: true,
  },
  render: (args) => ({
    components: { Alert },
    setup() {
      return { args };
    },
    template: '<Alert v-bind="args">Click the X to dismiss this alert.</Alert>',
  }),
};

export const Bordered: Story = {
  render: () => ({
    components: { Alert },
    template: `
      <div class="flex flex-col gap-4">
        <Alert variant="info" bordered>Info with border</Alert>
        <Alert variant="success" bordered>Success with border</Alert>
        <Alert variant="warning" bordered>Warning with border</Alert>
        <Alert variant="error" bordered>Error with border</Alert>
      </div>
    `,
  }),
};

export const WithoutIcon: Story = {
  args: {
    variant: 'info',
    showIcon: false,
    title: 'No Icon',
  },
  render: (args) => ({
    components: { Alert },
    setup() {
      return { args };
    },
    template: '<Alert v-bind="args">This alert has no icon.</Alert>',
  }),
};
