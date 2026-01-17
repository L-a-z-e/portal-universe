import { test, expect } from '@playwright/test';

const AUTH_SERVER_URL = 'http://localhost:8081'; // Assuming auth-service is running on port 8081

async function getAccessToken(username, password) {
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
    if (response.status() !== 200) {
        console.error(`Failed to get access token for ${username}: ${json.error_description}`);
        throw new Error(`Failed to get access token for ${username}`);
    }
    return json.access_token;
}

test.describe('Role-based Authorization', () => {
    test('USER should not access /api/admin', async ({ request }) => {
        const accessToken = await getAccessToken('finaltest@example.com', 'password123');
        const response = await request.get(`${AUTH_SERVER_URL}/api/admin`, {
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        expect(response.status()).toBe(403);
    });

    test('ADMIN should access /api/admin', async ({ request }) => {
        const accessToken = await getAccessToken('test@example.com', 'password123');
        const response = await request.get(`${AUTH_SERVER_URL}/api/admin`, {
            headers: {
                'Authorization': `Bearer ${accessToken}`,
            },
        });
        expect(response.status()).toBe(200);
    });
});
