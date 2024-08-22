// available-concerts-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { options, BASE_URL } from '../common/test-options.js';

export { options };

export default function () {
    const concertsResponse = http.get(`${BASE_URL}/api/v1/concerts`, {
        headers: { 'QUEUE-TOKEN': `token` },
    });

    check(concertsResponse, {
        'concerts status was 200': (r) => r.status === 200,
        // 'response has concerts': (r) => JSON.parse(r.body).length > 0,
    });

    sleep(1);
}