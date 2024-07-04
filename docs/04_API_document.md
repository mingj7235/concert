# 콘서트 예약 시스템 API 문서

## 목차
1. [대기열 토큰 발급](#1-대기열-토큰-발급)
2. [대기열 토큰 조회](#2-대기열-토큰-조회)
3. [대기열 상태 조회](#3-대기열-상태-조회)
4. [콘서트 일정 조회](#4-콘서트-일정-조회)
5. [콘서트 좌석 조회](#5-콘서트-좌석-조회)
6. [좌석 예약](#6-좌석-예약)
7. [결제 실행](#7-결제-실행)
8. [잔액 충전](#8-잔액-충전)
9. [잔액 조회](#9-잔액-조회)

## 1. 대기열 토큰 발급

### Description
- 대기열에 사용자를 추가하고 대기열 토큰을 반환합니다.

### Request

- **URL**: `/v1/queue-tokens/users/{userId}`
- **Method**: POST
- **URL Params**:
    - `userId`: Long (사용자 ID)

### Response

```json
{
  "tokenId": 1,
  "createdAt": "2024-07-04T10:00:00",
  "expiredAt": "2024-07-04T10:10:00"
}
```

### Error
```json
{
  "code": 404,
  "message": "user not found"
}
```

<br>

## 2. 대기열 토큰 조회

### Description
- 사용자의 발급된 대기열 토큰 정보를 조회합니다.

### Request

- **URL**: `/v1/queue-tokens/users/{userId}`
- **Method**: GET
- **URL Params**:
  - `userId`: Long (사용자 ID)

### Response

```json
{
  "tokenId": 1,
  "createdAt": "2024-07-04T10:00:00",
  "expiredAt": "2024-07-04T10:10:00"
}
```

### Error
```json
{
  "code": 404,
  "message": "user not found"
}
```

<br>

## 3. 대기열 상태 조회

### Description
- 사용자의 대기열 상태를 조회합니다.
- 클라리언트단에서 폴링으로 현재 대기열 상태를 조회하기 위해 사용됩니다.

### Request

- **URL**: `/v1/queue-status/users/{userId}`
- **Method**: GET
- **URL Params**:
  - `userId`: Long (사용자 ID)

- **Headers**:
  - `TOKEN-ID`: Long (대기열 토큰 ID)

### Response

```json
{
  "queueId": 1,
  "joinAt": "2024-07-04T10:00:00",
  "status": "WAITING",
  "remainingWaitListCount": 10
}
```

### Error
```json
{
  "code": 404,
  "message": "user not found"
}
```

<br>

## 4. 콘서트 일정 조회

### Description
- 특정 콘서트의 예약 가능한 일정을 조회합니다.

### Request

- **URL**: `/v1/concerts/{concertId}/schedules`
- **Method**: GET
- **URL Params**:
  - `concertId`: Long (콘서트 ID)
- **Headers**:
  - `TOKEN-ID`: Long (대기열 토큰 ID)

### Response

```json
{
  "concertId": 1,
  "events": [
    {
      "scheduleId": 1,
      "concertAt": "2024-08-04T10:00:00",
      "reservationAt": "2024-07-19T10:00:00"
    },
    {
      "scheduleId": 2,
      "concertAt": "2024-09-04T10:00:00",
      "reservationAt": "2024-07-24T10:00:00"
    }
  ]
}
```

### Error
```json
{
  "code": 401,
  "message": "invalid token"
}
```
```json
{
  "code": 404,
  "message": "user not found"
}
```
```json
{
  "code": 404,
  "message": "concert not found"
}
```

<br>

## 5. 콘서트 좌석 조회

### Description
- 특정 콘서트 일정의 좌석 정보를 조회합니다.

### Request

- **URL**: `/v1/concerts/{concertId}/schedules/{scheduleId}/seats`
- **Method**: GET
- **URL Params**:
  - `concertId`: Long (콘서트 ID)
  - `scheduleId`: Long (일정 ID)
- **Headers**:
  - `TOKEN-ID`: Long (대기열 토큰 ID)

### Response

```json
{
  "concertId": 1,
  "concertAt": "2024-08-04T10:00:00",
  "seats": [
    {
      "seatId": 1,
      "seatNumber": 20,
      "seatStatus": "AVAILABLE",
      "seatPrice": 10000
    },
    {
      "seatId": 2,
      "seatNumber": 21,
      "seatStatus": "UNAVAILABLE",
      "seatPrice": 10000
    }
  ]
}
```

### Error
```json
{
  "code": 401,
  "message": "invalid token"
}
```
```json
{
  "code": 404,
  "message": "user not found"
}
```
```json
{
  "code": 404,
  "message": "concert not found"
}
```
```json
{
  "code": 404,
  "message": "schedule not found"
}
```

<br>

## 6. 좌석 예약

### Description
- 콘서트 좌석을 예약합니다.
- 콘서트와 날짜를 선택한 이후에 진행되므로, 다른 콘서트나 다른 일정을 동시에 예약하는 것은 불가능합니다.
- 좌석을 여러개 예약할 수 있습니다.

### Request

- **URL**: `/v1/reservations`
- **Method**: POST
- **Headers**:
  - `TOKEN-ID`: Long (대기열 토큰 ID)
  - `Content-Type`: application/json


- **Body**:
```json
{
  "userId": 1,
  "concertId": 1,
  "scheduleId": 1,
  "seatIds": [10, 11]
}
```

### Response

```json
{
  "reservationId": 1,
  "concertId": 1,
  "concertName": "콘서트",
  "concertAt": "2024-08-04T10:00:00",
  "seats": [
    {
      "seatNumber": 10,
      "price": 10000
    },
    {
      "seatNumber": 11,
      "price": 15000
    }
  ],
  "totalPrice": 25000,
  "reservationStatus": "PAYMENT_PENDING"
}
```

### Error
```json
{
  "code": 401,
  "message": "invalid token"
}
```
```json
{
  "code": 404,
  "message": "user not found"
}
```
```json
{
  "code": 404,
  "message": "concert not found"
}
```
```json
{
  "code": 404,
  "message": "schedule not found"
}
```
```json
{
  "code": 404,
  "message": "seat not found"
}
```
```json
{
  "code": 400,
  "message": "reservation failed"
}
```

<br>

## 7. 결제 실행

### Description
- 예약에 대한 결제를 진행합니다.
- 예약을 5분내에 결제하지 않으면 결제할 수 없습니다.

### Request

- **URL**: `/v1/payments/users/{userId}`
- **Method**: POST
- **URL Params**:
  - `userId`: Long (사용자 ID)
- **Headers**:
  - `TOKEN-ID`: Long (대기열 토큰 ID)
  - `Content-Type`: application/json


- **Body**:
```json
{
  "reservationId": 1
}
```

### Response
```json
{
  "paymentId": 1,
  "amount": 30000,
  "paymentStatus": "COMPLETED"
}
```

### Error
```json
{
  "code": 401,
  "message": "invalid token"
}
```
```json
{
  "code": 404,
  "message": "user not found"
}
```
```json
{
  "code": 404,
  "message": "reservation not found"
}
```
```json
{
  "code": 400,
  "message": "not enough balance"
}
```
```json
{
  "code": 500,
  "message": "payment failed"
}
```

<br>

## 8. 잔액 충전

### Description
- 사용자의 잔액을 충전합니다.

### Request

- **URL**: `/v1/balance/users/{userId}/recharge`
- **Method**: POST
- **URL Params**:
  - `userId`: Long (사용자 ID)
- **Headers**:
  - `Content-Type`: application/json


- **Body**:
```json
{
  "amount": 50000
}
```

### Response
```json
{
  "userId": 1,
  "currentAmount": 40000
}
```

### Error

```json
{
  "code": 404,
  "message": "user not found"
}
```
```json
{
  "code": 400,
  "message": "invalid recharge amount"
}
```
```json
{
  "code": 500,
  "message": "recharge failed"
}
```

## 9. 잔액 조회

### Description
- 사용자의 현재 잔액을 조회합니다.

### Request

- **URL**: `/v1/balance/users/{userId}`
- **Method**: GET
- **URL Params**:
  - `userId`: Long (사용자 ID)

### Response
```json
{
  "userId": 1,
  "currentAmount": 40000
}
```

### Error

```json
{
  "code": 404,
  "message": "user not found"
}
```