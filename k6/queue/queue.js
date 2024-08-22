import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { options, BASE_URL } from '../common/test-options.js';

const users = new SharedArray('users', function () {
    return Array.from({ length: 1000 }, (_, i) => i + 1);  // Assuming 1000 users
});

export default function () {
    const userId = randomItem(users);

    const response = http.post(`${BASE_URL}/api/v1/queue/users/${userId}`);

    check(response, {
        'status is 200': (r) => r.status === 200,
        'response has token': (r) => {
            const body = JSON.parse(r.body);
        },
    });

    console.log(`User ${userId} received token: ${JSON.parse(response.body).token}`);

    sleep(1);
}