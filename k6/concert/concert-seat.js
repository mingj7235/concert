import http from 'k6/http';
import { check, sleep } from 'k6';
import { options, BASE_URL } from '../common/test-options.js';

export { options };

export default function () {
    const concertId = Math.floor(Math.random() * 5) + 1;
    const scheduleId = Math.floor(Math.random() * 28) + 1;

    const seatsResponse = http.get(`${BASE_URL}/api/v1/concerts/${concertId}/schedules/${scheduleId}/seats`, {
        headers: { 'QUEUE-TOKEN': `token` },
    });

    check(seatsResponse, {
        'seats status was 200': (r) => r.status === 200,
        // 'response has seats': (r) => JSON.parse(r.body).seats.length > 0,
    });

    sleep(1);
}