import { test, expect } from '@playwright/test';
import { defaultTestUser } from '../fixtures/auth';

const AUTH_SERVER_URL = 'http://localhost:8081';

async function getAccessToken(username: string, password: string) {
    const response = await fetch(`${AUTH_SERVER_URL}/oauth2/token`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({
            'grant_type': 'password',
            'username': username,
            'password': password,
            'client_id': 'portal-client',
        }),
    });
    const json = await response.json();
    if (response.status !== 200) {
        console.error(`Failed to get access token for ${username}: ${json.error_description}`);
        throw new Error(`Failed to get access token for ${username}`);
    }
    return json.access_token;
}

test.describe('Role-based Authorization', () => {
    test('USER should not access /api/v1/admin', async ({ request }) => {
        const accessToken = await getAccessToken(defaultTestUser.email, defaultTestUser.password);
        const response = await request.get(`${AUTH_SERVER_URL}/api/v1/admin`, {
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        expect(response.status()).toBe(403);
    });

    test('ADMIN should access /api/v1/admin', async ({ request }) => {
        const accessToken = await getAccessToken(defaultTestUser.email, defaultTestUser.password);
        const response = await request.get(`${AUTH_SERVER_URL}/api/v1/admin`, {
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        expect(response.status()).toBe(200);
    });
});
