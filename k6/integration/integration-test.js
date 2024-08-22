import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { options, BASE_URL } from '../common/test-options.js';

export { options };

const users = new SharedArray('users', function () {
    return Array.from({ length: 1000 }, (_, i) => i + 1);
});

const concerts = new SharedArray('concerts', function () {
    return Array.from({ length: 5 }, (_, i) => i + 1);
});

const schedules = new SharedArray('schedules', function () {
    return Array.from({ length: 28 }, (_, i) => i + 1);
});

function getRandomSeats(count) {
    const seats = new Set();
    while (seats.size < count) {
        seats.add(Math.floor(Math.random() * 672) + 1);
    }
    return Array.from(seats);
}

export default function () {
    const userId = randomItem(users);

    // Step 1: 토큰을 얻는다.
    let queueResponse = http.post(`${BASE_URL}/api/v1/queue/users/${userId}`);
    check(queueResponse, { 'Queue token received': (r) => r.status === 200 });
    let queueToken = JSON.parse(queueResponse.body).token;

    sleep(1);

    // Step 2: 콘서트가 예약 가능한지 확인한다.
    let availableConcerts = JSON.parse(concertsResponse.body).filter(concert => concert.available);
    if (availableConcerts.length === 0) {
        console.log('No available concerts');
        return;
    }

    let selectedConcert = randomItem(availableConcerts);
    let concertId = selectedConcert.id;

    sleep(1);

    // Step 3: Check concert schedules availability
    let schedulesResponse = http.get(`${BASE_URL}/api/v1/concerts/${concertId}/schedules`, {
        headers: { 'QUEUE-TOKEN': queueToken },
    });
    check(schedulesResponse, { 'Concert schedules checked': (r) => r.status === 200 });

    let availableSchedules = JSON.parse(schedulesResponse.body).filter(schedule => schedule.available);
    if (availableSchedules.length === 0) {
        console.log('No available schedules for concert ' + concertId);
        return;
    }

    let selectedSchedule = randomItem(availableSchedules);
    let scheduleId = selectedSchedule.id;

    sleep(1);

    // Step 4: Check seat availability
    let seatsResponse = http.get(`${BASE_URL}/api/v1/concerts/${concertId}/schedules/${scheduleId}/seats`, {
        headers: { 'QUEUE-TOKEN': queueToken },
    });
    check(seatsResponse, { 'Seats availability checked': (r) => r.status === 200 });

    let availableSeats = JSON.parse(seatsResponse.body).filter(seat => seat.available);
    if (availableSeats.length === 0) {
        console.log('No available seats for concert ' + concertId + ' and schedule ' + scheduleId);
        return;
    }

    // Step 3: 예약을 시도한다.
    const seatIds = getRandomSeats(Math.floor(Math.random() * 4) + 1);  // 1 to 4 random seats
    let reservationPayload = JSON.stringify({
        userId: userId,
        concertId: concertId,
        scheduleId: scheduleId,
        seatIds: seatIds,
    });

    let reservationResponse = http.post(`${BASE_URL}/api/v1/reservations`, reservationPayload, {
        headers: {
            'Content-Type': 'application/json',
            'QUEUE-TOKEN': queueToken,
        },
    });
    check(reservationResponse, { 'Reservation made': (r) => r.status === 200 });

    let reservationIds = JSON.parse(reservationResponse.body).map(res => res.id);

    sleep(1);

    // Step 4: Make payment
    let paymentPayload = JSON.stringify({
        reservationIds: reservationIds,
    });

    let paymentResponse = http.post(`${BASE_URL}/api/v1/payment/payments/users/${userId}`, paymentPayload, {
        headers: {
            'Content-Type': 'application/json',
            'QUEUE-TOKEN': queueToken,
        },
    });
    check(paymentResponse, { 'Payment completed': (r) => r.status === 200 });

    sleep(1);
}