## 1. 유저 토큰 발급 

### 이벤트 시퀀스 다이어그램
```mermaid
sequenceDiagram
    participant User
    participant API
    participant UserService
    participant QueueService
    participant TokenService
    participant Database

    User->>API: 토큰 발급 요청
    API->>UserService: 사용자 인증 (존재하는 회원 확인)
    UserService-->>API: 인증 결과
    alt 인증 성공 (존재하는 회원)
        API->>QueueService: 대기열 위치 요청
        QueueService->>Database: 현재 대기열 상태 조회
        Database-->>QueueService: 대기열 상태 반환
        QueueService-->>API: 대기열 위치 반환
        API->>TokenService: 토큰 생성 요청
        TokenService->>Database: 토큰 정보 저장
        Database-->>TokenService: 저장 완료
        TokenService-->>API: 생성된 토큰
        API-->>User: 토큰 및 대기열 정보 반환
    else 인증 실패 (존재하지 않는 회원)
        API-->>User: 오류 메시지 반환 (Not found User)
    end
```
### Description

유저가 콘서트 예약을 시도할 때, 토큰을 발급받습니다.

현재 대기열의 상태를 조회하고, 토큰 생성을 요청하여 DB 에 저장합니다.

생성된 토큰과 조회한 대기열의 상태 정보를 반환합니다. 


<br>

## 2. 유저 토큰을 통한 대기열 정보 조회

### 이벤트 시퀀스 다이어그램
```mermaid
sequenceDiagram
    participant User
    participant API
    participant TokenService
    participant QueueService
    participant Database

    User->>API: 대기열 정보 조회 요청 (토큰 포함)
    API->>TokenService: 토큰 유효성 검증
    TokenService->>Database: 토큰 정보 조회
    Database-->>TokenService: 토큰 정보
    TokenService-->>API: 검증 결과
    alt 토큰 유효
        API->>QueueService: 현재 대기열 상태 요청
        QueueService->>Database: 대기열 정보 조회
        Database-->>QueueService: 대기열 정보
        QueueService-->>API: 사용자의 현재 대기 상태
        API-->>User: 대기열 정보 (예상 대기 시간, 순서 등)
    else 찾을 수 없는 토큰
        API-->>User: 오류 메시지 반환 (Not found Token)
    end
```

### Description

토큰을 통해 대기열 정보를 조회합니다.

폴링으로 대기열을 확인하는 것을 전제합니다.


<br>

## 콘서트 예약 가능 날짜 목록 조회

### 이벤트 시퀀스 다이어그램
```mermaid
sequenceDiagram
    participant User
    participant API
    participant TokenService
    participant ConcertService
    participant Database

    User->>API: 콘서트 예약 가능 날짜 조회 요청 (콘서트 ID, 토큰)
    API->>TokenService: 토큰 유효성 검증
    TokenService->>Database: 토큰 정보 조회
    Database-->>TokenService: 토큰 정보
    TokenService-->>API: 검증 결과
    alt 유효한 토큰
        API->>ConcertService: 예약 가능 날짜 요청
        ConcertService->>Database: 콘서트 날짜 정보 조회
        Database-->>ConcertService: 날짜 정보
        ConcertService-->>API: 예약 가능 날짜 목록
        API-->>User: 예약 가능 날짜 목록 반환
    else 유효하지 않거나 찾을 수 없는 토큰
        API-->>User: 오류 메시지 반환
    end

```
### Description
토큰이 유효한지 검사합니다.

토큰이 유효하다면, 콘서트 ID 와 날짜로 콘서트 정보를 조회합니다.

해당 콘서트가 진행되는 날짜 목록을 조회합니다.

그 중, 예약이 가능한 날짜 목록을 사용자에게 반환합니다.

토큰이 유효하지 않다면 예외를 반환합니다.

<br>

## 좌석 정보 조회

### 이벤트 시퀀스 다이어그램
```mermaid
sequenceDiagram
    participant User
    participant API
    participant TokenService
    participant SeatService
    participant Database

    User->>API: 좌석 정보 조회 요청 (콘서트 ID, 날짜, 토큰)
    API->>TokenService: 토큰 유효성 검증
    TokenService->>Database: 토큰 정보 조회
    Database-->>TokenService: 토큰 정보
    TokenService-->>API: 검증 결과
    alt 유효한 토큰
        API->>SeatService: 예약 가능 좌석 요청 (콘서트 ID, 날짜)
        SeatService->>Database: 좌석 정보 조회 (콘서트 ID, 날짜, 1-50)
        Database-->>SeatService: 좌석 상태 정보
        SeatService-->>API: 예약 가능 좌석 목록
        API-->>User: 예약 가능 좌석 정보 반환 (1-50 중 가능한 좌석)
    else 유효하지 않거나 찾을 수 없는 토큰
        API-->>User: 오류 메시지 반환
    end

```
### Description
토큰 정보와 콘서트 ID, 날짜로 좌석 정보를 조회합니다.

토큰이 유효하다면, 해당 콘서트가 열리는 날짜의 예약 가능한 좌석을 조회합니다.

사용자에게 예약 가능한 좌석 정보를 반환합니다.

좌석은 1 ~ 50 으로 제한되어 있습니다.


<br>

## 좌석 예약

### 이벤트 시퀀스 다이어그램
```mermaid
sequenceDiagram
    participant User
    participant API
    participant TokenService
    participant ReservationService
    participant Database

    User->>API: 좌석 예약 요청 (콘서트 ID, 날짜, 좌석 번호, 토큰)
    API->>TokenService: 토큰 유효성 검증
    TokenService->>Database: 토큰 정보 조회
    Database-->>TokenService: 토큰 정보
    TokenService-->>API: 검증 결과
    alt 유효한 토큰
        API->>ReservationService: 좌석 예약 요청
        ReservationService->>Database: 좌석 상태 확인
        Database-->>ReservationService: 좌석 상태
        alt 좌석 예약 가능
            ReservationService->>Database: 예약 정보 저장, 선택한 좌석 상태 변경
            Database-->>ReservationService: 예약 정보 저장 완료
            ReservationService-->>API: 좌석 점유 성공, 좌석 ID 반환
                Note over User,API: 결제 프로세스 진행 (별도 시퀀스)
        else 좌석 예약 불가
            ReservationService-->>API: 예약 실패 (좌석 이미 예약됨)
            API-->>User: 예약 실패 (좌석 이미 예약됨)
        end
    else 유효하지 않거나 찾을 수 없는 토큰
        API-->>User: 오류 메시지 반환
    end
```

### Description
콘서트 ID, 날짜, 좌석 번호 정보로 좌석을 예약합니다.

좌석이 예약이 가능하다면 예약 정보를 저장하고, 좌석 상태를 점유상태로 변경합니다.

점유 후 결제 프로세스를 진행합니다.

결제 프로세스는 별도로 진행하며, 점유 후 5분이 지난다면 별도의 스케쥴러를 통해 점유를 해제합니다.

좌석이 예약 불가능하다면 예약 실패 메세지를 유저에게 반환합니다.

<br>

## 결제

### 이벤트 시퀀스 다이어그램
```mermaid
sequenceDiagram
    participant User
    participant API
    participant TokenService
    participant PaymentService
    participant ReservationService
    participant Database
    
    User->>API: 결제 요청 (예약 ID, 토큰)
    API->>TokenService: 토큰 유효성 검증
    TokenService->>Database: 토큰 정보 조회
    Database-->>TokenService: 토큰 정보
    TokenService-->>API: 검증 결과
    alt 토큰 유효
        API->>PaymentService: 결제 처리 요청
        PaymentService->>Database: 사용자 잔액 확인
        Database-->>PaymentService: 잔액 정보
        alt 잔액이 충분함
            PaymentService->>Database: 결제 처리 및 기록
            Database-->>PaymentService: 처리 완료
            PaymentService-->>API: 결제 성공 및 결제 내역 반환
            API->>ReservationService: 좌석 상태 업데이트 요청
            ReservationService->>Database: 좌석 상태 '결제 완료'로 변경
            Database-->>ReservationService: 업데이트 완료
            ReservationService-->>API: 좌석 상태 업데이트 성공
            API->>TokenService: 대기열 토큰 만료 요청
            TokenService->>Database: 토큰 만료 처리
            Database-->>TokenService: 만료 처리 완료
            TokenService-->>API: 토큰 만료 성공
        else 잔액이 부족함
            PaymentService-->>API: 결제 실패 (잔액 부족)
            API-->>User: 결제 실패 메시지 (잔액 부족)
        end
    else 유효하지 않거나 찾을 수 없는 토큰
        API-->>User: 오류 메시지 반환
    end
```
### Description
좌석 예약에 이어 결제를 진행합니다.

결제에 성공하면 좌석의 상태를 결제 완료로 변경합니다.

또한, 대기열 토큰을 만료 시킵니다.


<br>

## 잔액 충전

### 이벤트 시퀀스 다이어그램
```mermaid
sequenceDiagram
    participant User
    participant API
    participant BalanceService
    participant Database

    User->>API: 잔액 충전 요청 (유저 ID, 충전 금액)
    API->>BalanceService: 잔액 충전 요청
    BalanceService->>Database: 현재 잔액 조회
    Database-->>BalanceService: 현재 잔액
    BalanceService->>Database: 잔액 업데이트
    Database-->>BalanceService: 업데이트 완료
    BalanceService-->>API: 충전 성공, 총 잔액 반환
    API-->>User: 충전 성공 메시지, 총 잔액 정보

```

### Description
잔액 충전은 대기열 토큰 검증이 불필요합니다.

잔액 충전이 성공하면 총 잔액을 반환합니다.

<br>

## 잔액 조회
### 이벤트 시퀀스 다이어그램
```mermaid
sequenceDiagram
    participant User
    participant API
    participant BalanceService
    participant Database

    User->>API: 잔액 조회 요청 (유저 ID)
    API->>BalanceService: 잔액 조회 요청
    BalanceService->>Database: 잔액 정보 조회
    Database-->>BalanceService: 잔액 정보
    BalanceService-->>API: 현재 잔액 반환
    API-->>User: 현재 잔액 정보
```
### Description
유저의 현재 잔액을 조회합니다.











