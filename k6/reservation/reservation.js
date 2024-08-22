import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { options, BASE_URL } from '../common/test-options.js';

export { options };

const users = new SharedArray('users', function () {
    return Array.from({ length: 5 }, (_, i) => i + 1);
});

const concerts = new SharedArray('concerts', function () {
    return Array.from({ length: 5 }, (_, i) => i + 1);
});

const schedules = new SharedArray('schedules', function () {
    return Array.from({ length: 28 }, (_, i) => i + 1);
});

// Helper function to get random seats
function getRandomSeats(count) {
    const seats = new Set();
    while (seats.size < count) {
        seats.add(Math.floor(Math.random() * 672) + 1);
    }
    return Array.from(seats);
}

export default function () {
    const userId = randomItem(users);
    const concertId = randomItem(concerts);
    const scheduleId = randomItem(schedules);
    const seatIds = getRandomSeats(Math.floor(Math.random() * 4) + 1);  // 1 to 4 random seats

    const payload = JSON.stringify({
        userId: userId,
        concertId: concertId,
        scheduleId: scheduleId,
        seatIds: seatIds,
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'QUEUE-TOKEN': 'token',
        },
    };

    const response = http.post(`${BASE_URL}/api/v1/reservations`, payload, params);

    check(response, {
        'status is 200': (r) => r.status === 200,
    });

    sleep(1);
}