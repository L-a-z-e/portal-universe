import type { Meta, StoryObj } from '@storybook/react';
import { Alert } from './Alert';

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
    children: 'This is an informational message.',
  },
};

export const Success: Story = {
  args: {
    variant: 'success',
    title: 'Success',
    children: 'Your changes have been saved successfully.',
  },
};

export const Warning: Story = {
  args: {
    variant: 'warning',
    title: 'Warning',
    children: 'Please review your input before proceeding.',
  },
};

export const Error: Story = {
  args: {
    variant: 'error',
    title: 'Error',
    children: 'An error occurred while processing your request.',
  },
};

export const AllVariants: Story = {
  render: () => (
    <div className="flex flex-col gap-4">
      <Alert variant="info" title="Information">
        This is an informational message.
      </Alert>
      <Alert variant="success" title="Success">
        Operation completed successfully.
      </Alert>
      <Alert variant="warning" title="Warning">
        Please proceed with caution.
      </Alert>
      <Alert variant="error" title="Error">
        Something went wrong.
      </Alert>
    </div>
  ),
};

export const Dismissible: Story = {
  args: {
    variant: 'info',
    dismissible: true,
    children: 'Click the X to dismiss this alert.',
  },
};

export const Bordered: Story = {
  render: () => (
    <div className="flex flex-col gap-4">
      <Alert variant="info" bordered>
        Info with border
      </Alert>
      <Alert variant="success" bordered>
        Success with border
      </Alert>
      <Alert variant="warning" bordered>
        Warning with border
      </Alert>
      <Alert variant="error" bordered>
        Error with border
      </Alert>
    </div>
  ),
};

export const WithoutIcon: Story = {
  args: {
    variant: 'info',
    showIcon: false,
    title: 'No Icon',
    children: 'This alert has no icon.',
  },
};
