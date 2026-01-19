import type { Meta, StoryObj } from '@storybook/vue3';
import { Toast, ToastContainer } from './index';
import { useToast } from '../../composables/useToast';
import { Button } from '../Button';

const meta: Meta<typeof Toast> = {
  title: 'Components/Feedback/Toast',
  component: Toast,
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['info', 'success', 'warning', 'error'],
    },
    dismissible: {
      control: 'boolean',
    },
  },
};

export default meta;
type Story = StoryObj<typeof Toast>;

export const Info: Story = {
  args: {
    id: 'info-toast',
    variant: 'info',
    title: 'Information',
    message: 'This is an informational toast message.',
    duration: 0,
  },
};

export const Success: Story = {
  args: {
    id: 'success-toast',
    variant: 'success',
    title: 'Success',
    message: 'Your changes have been saved successfully.',
    duration: 0,
  },
};

export const Warning: Story = {
  args: {
    id: 'warning-toast',
    variant: 'warning',
    title: 'Warning',
    message: 'Please review your input before proceeding.',
    duration: 0,
  },
};

export const Error: Story = {
  args: {
    id: 'error-toast',
    variant: 'error',
    title: 'Error',
    message: 'An error occurred while processing your request.',
    duration: 0,
  },
};

export const WithAction: Story = {
  args: {
    id: 'action-toast',
    variant: 'info',
    message: 'Your session will expire soon.',
    duration: 0,
    action: {
      label: 'Extend Session',
      onClick: () => console.log('Session extended'),
    },
  },
};

export const Interactive: Story = {
  render: () => ({
    components: { ToastContainer, Button },
    setup() {
      const { success, error, warning, info, clear } = useToast();

      const showSuccess = () => {
        success('Operation completed successfully!', { title: 'Success' });
      };

      const showError = () => {
        error('Something went wrong. Please try again.', { title: 'Error' });
      };

      const showWarning = () => {
        warning('Your session will expire in 5 minutes.', { title: 'Warning' });
      };

      const showInfo = () => {
        info('New updates are available.', { title: 'Info' });
      };

      return { showSuccess, showError, showWarning, showInfo, clear };
    },
    template: `
      <div>
        <ToastContainer position="top-right" />
        <div class="flex flex-wrap gap-4">
          <Button @click="showSuccess" variant="primary">Show Success</Button>
          <Button @click="showError" variant="secondary">Show Error</Button>
          <Button @click="showWarning" variant="secondary">Show Warning</Button>
          <Button @click="showInfo" variant="secondary">Show Info</Button>
          <Button @click="clear" variant="outline">Clear All</Button>
        </div>
      </div>
    `,
  }),
};

export const AllVariants: Story = {
  render: () => ({
    components: { Toast },
    template: `
      <div class="flex flex-col gap-4 max-w-sm">
        <Toast
          id="1"
          variant="info"
          title="Information"
          message="This is an info toast."
          :duration="0"
        />
        <Toast
          id="2"
          variant="success"
          title="Success"
          message="This is a success toast."
          :duration="0"
        />
        <Toast
          id="3"
          variant="warning"
          title="Warning"
          message="This is a warning toast."
          :duration="0"
        />
        <Toast
          id="4"
          variant="error"
          title="Error"
          message="This is an error toast."
          :duration="0"
        />
      </div>
    `,
  }),
};
