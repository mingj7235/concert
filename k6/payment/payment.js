import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { options, BASE_URL } from '../common/test-options.js';

export { options };

const users = new SharedArray('users', function () {
    return Array.from({ length: 5 }, (_, i) => i + 1);
});

const reservationIds = new SharedArray('reservationIds', function () {
    return Array.from({ length: 1000 }, (_, i) => i + 1);
});

function getRandomReservationIds(count) {
    const selectedIds = new Set();
    while (selectedIds.size < count) {
        selectedIds.add(randomItem(reservationIds));
    }
    return Array.from(selectedIds);
}

export default function () {
    const userId = randomItem(users);
    const selectedReservationIds = getRandomReservationIds(Math.floor(Math.random() * 3) + 1);

    const payload = JSON.stringify({
        reservationIds: selectedReservationIds,
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'QUEUE-TOKEN': 'token',
        },
    };

    const response = http.post(`${BASE_URL}/api/v1/payment/payments/users/${userId}`, payload, params);

    check(response, {
        'status is 200': (r) => r.status === 200,
        'response has payment results': (r) => {
            const body = JSON.parse(r.body);
            return Array.isArray(body) && body.length > 0;
        },
    });

    sleep(1);
}