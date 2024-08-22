import http from 'k6/http';
import { check, sleep } from 'k6';
import { options, BASE_URL } from '../common/test-options.js';

export { options };

const TEST_USER_IDS = [1, 2, 3, 4, 5];  // 테스트 데이터로 생성한 사용자 ID

export default function () {
    const userId = TEST_USER_IDS[Math.floor(Math.random() * TEST_USER_IDS.length)];

    const rechargePayload = JSON.stringify({
        amount: Math.floor(Math.random() * 10000) + 1000,
    });

    const rechargeRes = http.post(`${BASE_URL}/api/v1/balance/users/${userId}/recharge`, rechargePayload, {
        headers: { 'Content-Type': 'application/json' },
    });

    check(rechargeRes, {
        'recharge status is 200': (r) => r.status === 200,
    });

    sleep(1);
}