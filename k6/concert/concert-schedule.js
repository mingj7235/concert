import http from 'k6/http';
import { check, sleep } from 'k6';
import { options, BASE_URL } from '../common/test-options.js';

export { options };

export default function () {
    const concertId = Math.floor(Math.random() * 5) + 1;

    const schedulesResponse = http.get(`${BASE_URL}/api/v1/concerts/${concertId}/schedules`, {
        headers: { 'QUEUE-TOKEN': `token` },
    });

    check(schedulesResponse, {
        'schedules status was 200': (r) => r.status === 200,
        // 'response has events': (r) => JSON.parse(r.body).events.length > 0,
    });

    sleep(1);
}