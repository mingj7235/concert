# MSA를 찍먹해보자.  - feat. Saga Pattern

# 🌱 0. 들어가며.

### MSA is not a silver bullet 🔫

한때, MSA를 선망하던 때가 있었다.
MSA 를 무조건 해야, MSA 경험이 있어야 제대로 된 개발자라는 착각을 하던 때가 있었다.
아무것도 모르고, MSA 를 마치 은탄환 마냥 모든 곳에서 지향해야 한다는 생각을 가졌었다.

조금씩 경력이 쌓이고 이것저것 만들다보니 _**'소프트웨어의 세계에는 정답이 없다는 것'**_ 을 알게 되었다.
개발자가 가지고 가야할 방향성은 정답을 찾는 것이 아니라,
_**'놓여있는 상황과 환경에서 가장 최선의 것을 찾아내고 그것의 근거를 확실히 하는 것이 중요하다는 것'**_ 을 점차 알게 되었다.

<br>

### 킹치만 MSA의 경험은 매우 중요하다.
MSA 가 모든 소프트웨어의 설계의 답이 될 수는 없지만, 그것을 아는것과 모르는 것은 천차 만별이다.
그리고 개인적으로 도대체 MSA로 어떻게 소프트웨어를 설계하고 구현하는지 너무 궁금했다.
경험이 없으니 아무리 글을 읽고, MSA 경험에 대해 영상을 봐도 잘 들어오지 않았다.
모놀로틱 구조에서 MSA 로 변환한 이야기를 들어도 '와..대단하다...' 라는 감탄만 나왔지, '그래서 .. 어떻게 했다는거야?ㅋㅋㅋㅋㅋ' 로 귀결이 되었다.

'경험이 없어서'였다.
'어떻게 시작할지 몰라서'였다.

MSA를 하려면 혼자 못한다.
이것 저것 서로 맞물려있는 Micro Service 들이 있어야 하고,
그것을 핸들링 하기 위한 여러가지 세부적인 기술들이 있어야 한다.

그러던 내게, 항해플러스 8주차 과제가 던져졌다.

<br>

# 🍎 1. 8주차 항해 회고

## 드디어 MSA라는 것을 해본다.

![](https://velog.velcdn.com/images/joshuara7235/post/cf9582f1-e931-4e67-b55c-c26b65706f34/image.png)

이번 8주차의 요구사항은 다음과 같았다.

>**Step 15**
- Index 를 통한 쿼리 성능 개선

- 사실, Step 15 의 내용은 Index 에 대한 이해만 있다면 어렵지 않았다.
- 게다가 현재 만들고 있는 Concert 서비스는 복잡한 쿼리가 많지 않아서 Index 적용도 많지 않아 보였다.


>**Step 16**
- 현재 개발된 '트랜잭션 범위' 에 대해 제대로 이해할 것
- MSA형태로 서비스를 분리한다면 어떻게 분리될지 설계할 것
- '트랜잭션 처리' 의 한계와 해결방안에 대한 설계 문서를 작성할 것

- 이번 포스팅은 Step 16 의 내용을 중점으로 정리할 예정이다.
- MSA 관점에서 현재 모놀로틱한 형태로 만들어져 있는 서비스를 설계해 보는 것.


<br>

## 기존 서비스 아키텍처 구조 분석

### 1> 아키텍처 개요
- 현재 `'PaymentService'` 시스템은 모놀리식 구조로, 다음과 같은 주요 컴포넌트로 구성되어 있다.

>**PaymentService**
- 결제 처리의 핵심 로직을 담당하는 서비스
- 사용자 인증, 예약 확인, 결제 처리, 대기열 관리, 콘서트 상태 업데이트 등 전반적인 결제 프로세스를 조율한다. (Facade 의 역할)

- `PaymentService` 는 아래의 컴포넌트들을 DI 받는다.


>**UserManager**
- 사용자 정보 관리 담당
- 사용자 인증, 계정 잔액 확인 등의 기능을 수행한다.

>**ReservationManager**
- 예약 정보 관리 담당
- 예약 생성, 조회, 상태 업데이트 등의 기능 수행한다.

>**PaymentManager**
- 실제 결제 처리 및 결제 내역 관리 담당
- 결제 실행, 결제 내역 저장, 결제 취소 등의 기능을 수행한다.

>**QueueManager**
- 대기열 관리 담당
- Redis를 사용하여 대기열 토큰 관리, 대기열 상태 업데이트 등의 기능을 수행한다.

>**ConcertManager**
- 콘서트 정보 관리 담당
- 콘서트 조회, 좌석 상태 확인, 콘서트 상태 업데이트 등의 기능 수행한다.

>**ConcertCacheManager**
- 콘서트 정보의 캐시 관리 담당
- 콘서트 정보 캐싱, 캐시 무효화 등의 기능 수행한다.

<br>

### 2> 기존 트랜잭션 관리

```kotlin
@Transactional
fun executePayment(token: String, userId: Long, reservationIds: List<Long>): List<PaymentServiceDto.Result> {
// 1. 사용자 및 예약 정보 조회
// 2. 결제 실행 및 결제 내역 저장
// 3. 예약 상태 업데이트
// 4. 대기열 토큰 처리
// 5. 콘서트 상태 업데이트
    ...
}
```

- 현재 시스템의 핵심 트랜잭션은 `PaymentService` 의 `executePayment` 메서드에서 관리되고 있다.
- 이 메서드는 `@Transactional` 어노테이션을 통해 하나의 큰 트랜잭션으로 처리되고 있다.
- 이러한 트랜잭션 관리는 아래와 같은 문제를 가져올 수 있다.

>**기존 트랜잭션 관리의 문제점**
1) 긴 트랜잭션으로 인한 성능 저하 가능성
- 하나의 트랜잭션 내에서 여러 복잡한 작업이 수행되므로, 트랜잭션의 지속 시간이 길어질 수 있다.
- 긴 트랜잭션은 데이터베이스 연결을 오랫동안 점유하게 되어, 전체 시스템의 처리량을 저하시킬 수 있다.
>
2) 여러 서비스 간의 강한 결합
- 하나의 트랜잭션 내에서 여러 서비스(User, Reservation, Payment, Queue, Concert)가 밀접하게 연관되어 있다.
- 이러한 강한 결합은 개별 서비스의 독립적인 변경이나 확장을 어렵게 만든다.
- 한 서비스의 변경이 다른 서비스에 영향을 미칠 가능성이 높아, 시스템 유지보수의 복잡성이 증가한다.
>
3) Redis 작업 포함으로 인한 분산 트랜잭션 문제
- Redis를 사용한 대기열 처리가 동일한 트랜잭션 내에 포함되어 있어, 분산 트랜잭션 문제가 발생할 수 있다.
- 관계형 데이터베이스와 Redis 간의 트랜잭션 일관성을 보장하기 어려워, 데이터 불일치가 발생할 가능성이 있다.
- 네트워크 지연이나 Redis 서버 장애 시, 전체 트랜잭션이 실패할 위험이 있다.
>
4) 단일 실패 지점(Single Point of Failure) 존재
- 모든 주요 로직이 하나의 서비스에 집중되어 있어, 이 서비스에 문제가 발생하면 전체 결제 시스템이 마비될 수 있다.
- 부분적인 기능 장애가 전체 시스템의 장애로 확대될 가능성이 높다.
>
5) 개별 서비스의 독립적 확장 어려움
- 모든 기능이 하나의 서비스에 통합되어 있어, 특정 기능만을 선택적으로 확장하기 어렵다.
- 시스템의 일부분에 부하가 집중되더라도, 전체 시스템을 스케일아웃해야 하는 비효율성이 존재한다.
- 각 기능별로 다른 확장 전략을 적용하기 어려워, 리소스 활용의 최적화가 제한된다.


<br>

## MSA 관점으로 설계하기
### 1> MSA로의 전환 - 트랜잭션의 분리

- 위의 `PaymentService` 에서 DI 받았던 각각의 도메인 레벨의 Manager 들을 모두 Service 로 분리한다.
- 각각의 `Service` 들의 트랜잭션을 분리한다.

각 서비스별로 트랜잭션을 분리하여 관리함으로써, 전체 시스템의 결합도를 낮추고 개별 서비스의 자율성을 높인다.

>1) Payment Transaction
- 범위: 결제 실행 및 결제 내역 저장
```kotlin
@Transactional
fun executePayment(userId: Long, amount: BigDecimal): Payment {
// 결제 로직 실행
// 결제 내역 저장
}
```

>2) Reservation Transaction
- 범위: 예약 상태 업데이트
```kotlin
@Transactional
fun updateReservationStatus(reservationId: Long, status: ReservationStatus) {
// 예약 상태 업데이트 로직
}
```

> 3) Concert Transaction
- 범위: 콘서트 상태 업데이트
```kotlin
@Transactional
fun updateConcertStatus(concertId: Long, status: ConcertStatus) {
    // 콘서트 상태 업데이트 로직
}
```

이러한 트랜잭션 분리를 통해 각 서비스는 자체적인 데이터 일관성을 유지하면서, 전체 시스템의 유연성과 확장성을 향상시킬 수 있다.

<br>

### 2> MSA 로의 전환 - 이벤트 기반 아키텍처 도입

- 이번 주차에서는 일단, Spring 이 제공하는 `ApplicationEventPublisher`와 `@TransactionalEventListener` 를 이용하여 이벤트 기반 아키텍쳐로 전환했다.
- 다음 주차에서는 `Kafka` 를 사용하여 전환할 예정이다.


#### 2-1 > 이벤트를 정의한다.
- 이벤트 기반 아키텍쳐에서 가장 먼저 해야할 것은, 이벤트를 정의하는 것이다.
- 어떤 이벤트를 발행하고 소비할 것인지 설계가 필요하다.

```kotlin
// 예시 
data class PaymentCompletedEvent(val paymentId: Long, val reservationId: Long)
data class ReservationUpdatedEvent(val reservationId: Long, val status: ReservationStatus)
...

```
- 위의 예시는 '결제 완료' 이벤트와 '예약 변경' 에 대한 이벤트다.

#### 2-2> 이벤트를 발행한다.
- Spring 이 제공하는 `ApplicationEventPublisher` 를 통해 이벤트를 발행한다.

```kotlin
@Service
class PaymentService(private val eventPublisher: ApplicationEventPublisher) {
    @Transactional
    fun processPayment(paymentDetails: PaymentDetails) {
        // 결제 처리 로직...
        val payment = executePayment(paymentDetails)
        eventPublisher.publishEvent(PaymentCompletedEvent(payment.id, payment.reservationId))
    }
}
```
- 위와 같이 결제 처리 로직을 수행한 후에, `eventPublisher` 를 통해 '결제 완료 이벤트' 를 발행한다.

#### 2-3> 이벤트를 구독한다.
- `@TransactionalEventListener` 를 통해서 이벤트를 구독한다.

```kotlin
@Service
class ReservationService {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentCompleted(event: PaymentCompletedEvent) {
        // 예약 상태 업데이트 로직
        updateReservationStatus(event.reservationId, ReservationStatus.PAID)
    }
}
```
- `TransactionPhase.AFTER_COMMIT` 는 해당 이벤트의 트랜잭션 커밋 이후에 로직을 수행하겠다는 것이다.
- 즉, '결제 완료' 이벤트가 정상적으로 커밋이 된 후에, '예약 상태 업데이트 로직'을 수행하겠다는 것을 의미한다.
- 이후, 다시 publisher 를 통해서 '예약 상태 업데이트' 이벤트를 발행하고, 그 이벤트를 구독하는 listener 를 통해 그 이후 로직을 처리할 수 있다.


<br>

### 3> MSA 아키텍쳐에서 트랜잭션을 관리하자 - Saga Pattern

#### 3-1> Saga 란?

나는 처음에 Saga Pattern 이라고 해서 어떤 의미의 첫글자만을 따서 만든 약어인줄 알았다.
하지만, 말 그대로 Saga, 즉 '서사' 라는 의미였다.

아, 얼마나 낭만이있는가?
처음 Saga Pattern 의 의미를 알게 되었을 때 뭐랄까, 참 이 소프트웨어 세상은 멋지다 라는 생각이 문득 들었다. ㅎㅎ

내가 만드는 코드 한줄 한줄이 모여서 위대한 서사를 만든다.
이 Saga 에 대해 재미있게 쓴 글이 있어서 그 글을 공유해본다.
👉🏻 [MSA 사가 패턴에서 '사가'라는 용어에 대해서 ](https://upcurvewave.tistory.com/715)

<br>

#### 3-2> Saga Pattern 이란?

Saga 패턴은 마이크로서비스 아키텍처에서 분산 트랜잭션을 관리하기 위한 효과적인 방법이다.
이 패턴의 핵심 개념은 다음과 같다.

- **로컬 트랜잭션 시퀀스**: 하나의 큰 트랜잭션을 여러 개의 작은 로컬 트랜잭션으로 분할한다.
- **보상 트랜잭션**: 각 단계에서 실패가 발생할 경우, 이전 단계들의 변경사항을 취소하는 보상 트랜잭션을 실행한다.
- **이벤트 기반 통신**: 서비스 간 통신은 이벤트를 통해 이루어진다.

<br>

#### 3-3> 그럼, 이제 진짜 구현해보자

**이벤트 정의**
- 각 이벤트는 특정 비지니스 프로세스 단계를 나타낸다.
- 그리고 이벤트 기반의 아키텍쳐에서 이 이벤트들은 서비스간 통신의 기반이 된다.

```kotlin
data class PaymentInitiatedEvent(val userId: Long, val reservationIds: List<Long>)
data class PaymentCompletedEvent(val paymentId: Long, val reservationIds: List<Long>)
data class PaymentFailedEvent(val reservationIds: List<Long>)
data class ReservationsCompletedEvent(val reservationIds: List<Long>)
data class QueueCompletedEvent(val token: String)
data class ConcertStatusUpdatedEvent(val concertId: Long)
data class SagaCompletedEvent(val reservationIds: List<Long>)
data class SagaFailedEvent(val step: SagaStep, val reservationIds: List<Long>)

enum class SagaStep {
    PAYMENT, RESERVATION, QUEUE, CONCERT
}
```

- `PaymentInitiatedEvent`: 결제 프로세스의 시작을 알린다. 사용자 ID와 예약 ID 목록을 포함하여 결제 서비스에 필요한 정보를 전달하도록 한다.
- `PaymentCompletedEvent`: 결제가 성공적으로 완료되었음을 알린다. 생성된 결제 ID와 관련 예약 ID 목록을 포함한다.
- `PaymentFailedEvent`: 결제 실패 시 발생하며, 실패한 예약 ID 목록을 포함한다. 이를 통해 다른 서비스들이 적절한 보상 트랜잭션을 실행할 수 있다.
- `ReservationsCompletedEvent`: 예약 상태가 성공적으로 업데이트되었음을 알린다. 이는 결제 완료 후 예약 상태를 '결제 완료'로 변경한 후 발생한다.
- `QueueCompletedEvent`: 대기열 처리가 완료되었음을 알린다. 처리된 대기열의 토큰을 포함하여 다음 단계(콘서트 상태 업데이트)로 진행할 수 있게 한다.
- `ConcertStatusUpdatedEvent`: 콘서트 상태가 업데이트되었음을 알린다. 이는 전체 Saga 프로세스의 마지막 단계를 나타낸다.
- `SagaCompletedEvent`: 전체 Saga 프로세스가 성공적으로 완료되었음을 알린다.
- `SagaFailedEvent`: Saga 프로세스 중 어느 단계에서 실패가 발생했는지를 알린다. 실패한 단계(SagaStep)와 관련 예약 ID 목록을 포함하여 적절한 보상 트랜잭션을 실행할 수 있도록 한다.

<br>

**쪼개진 서비스들을 이벤트 기반으로 구현하기**


1. PaymentService
```kotlin
@Service
class PaymentService(
    private val eventPublisher: ApplicationEventPublisher,
    private val paymentManager: PaymentManager,
) {
    @Transactional
    fun processPayment(userId: Long, reservationIds: List<Long>) {
        try {
            // 결제 로직 실행
            val payment = paymentManager.executePayment(userId, reservationIds)
            eventPublisher.publishEvent(PaymentCompletedEvent(payment.id, reservationIds))
        } catch (e: Exception) {
            eventPublisher.publishEvent(PaymentFailedEvent(reservationIds))
            throw e
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun handlePaymentFailed(event: PaymentFailedEvent) {
        // 결제 실패 처리 로직
    }
}
```
- `processPayment` 메소드는 실제 결제 처리를 수행하고, 성공 또는 실패에 따라 적절한 이벤트를 발행하도록 한다.
- 트랜잭션 내에서 결제가 수행되며, 실패 시 예외를 발생시켜 트랜잭션이 롤백되도록 한다.
- `handlePaymentFailed` 메소드는 결제 실패 시 추가적인 처리(예: 로깅, 알림 등)를 수행할 수 있도록 한다.

<br>

2. ReservationService

```kotlin
@Service
class ReservationService(
    private val eventPublisher: ApplicationEventPublisher,
    private val reservationManager: ReservationManager,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentCompleted(event: PaymentCompletedEvent) {
        completeReservations(event.reservationIds)
        eventPublisher.publishEvent(ReservationsCompletedEvent(event.reservationIds))
    }

    @Transactional
    fun completeReservations(reservationIds: List<Long>) {
        // 예약 상태 업데이트 로직
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun handleReservationFailed(event: SagaFailedEvent) {
        if (event.step == SagaStep.RESERVATION) {
            // 예약 실패 처리 로직
        }
    }
}
```
- `handlePaymentCompleted` 메소드는 결제 완료 이벤트를 수신하여 예약 상태를 업데이트하고, 예약 완료 이벤트를 발행한다.
- `completeReservations` 메소드는 실제 예약 상태 업데이트 로직을 수행한다.
- `handleReservationFailed` 메소드는 예약 단계에서 실패가 발생했을 때의 처리 로직을 구현한다.


<br>

3. QueueService

```kotlin
@Service
class QueueService(private val eventPublisher: ApplicationEventPublisher) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleReservationsCompleted(event: ReservationsCompletedEvent) {
        completeQueue(event.reservationIds)
        eventPublisher.publishEvent(QueueCompletedEvent(getTokenFromReservations(event.reservationIds)))
    }

    private fun completeQueue(reservationIds: List<Long>) {
        // Redis 대기열 처리 로직
    }
}
```
- `@Async` 어노테이션을 사용하여 비동기적으로 대기열 처리를 수행한다.
- 예약 완료 이벤트를 수신하여 대기열 처리를 수행하고, 처리 완료 후 대기열 완료 이벤트를 발행한다.
- `Redis`를 사용한 대기열 처리 로직은 `completeQueue` 메소드에서 구현된다.

<br>

4. ConcertService
```kotlin
@Service
class ConcertService(private val eventPublisher: ApplicationEventPublisher) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleQueueCompleted(event: QueueCompletedEvent) {
        updateConcertStatus(event.token)
        eventPublisher.publishEvent(ConcertStatusUpdatedEvent(getConcertIdFromToken(event.token)))
    }

    @Transactional
    fun updateConcertStatus(token: String) {
        // 콘서트 상태 업데이트 로직
    }
}
```
- 대기열 완료 이벤트를 수신하여 콘서트 상태를 업데이트한다.
- 상태 업데이트 후 콘서트 상태 업데이트 완료 이벤트를 발행한다


위와 같은 느낌으로 Service 로직을 분리 시키고, 이벤트를 발행하고 구독하도록 Service 로직을 구현하도록 했다.
다만, `eventPublisher` 는 각각의 도메인에 맞게끔 세부적으로 쪼개서 구현해야한다.

<br>
**보상 트랜잭션**
- 각 단계에서 실패 시, 이전 단계들의 작업을 취소하는 보상 트랜잭션을 구현해야한다.

1. Payment 보상 - 결제 취소 및 환불 처리
```kotlin
@Service
class PaymentService(private val paymentRepository: PaymentRepository) {
    ...
    @Transactional
    fun compensatePayment(paymentId: Long) {
        val payment = paymentRepository.findById(paymentId).orElseThrow()
        payment.status = PaymentStatus.CANCELLED
        payment.refundedAt = LocalDateTime.now()
        paymentRepository.save(payment)
        // 실제 결제 취소 API 호출 (외부 결제 시스템이 있다면..!)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun handlePaymentFailed(event: PaymentFailedEvent) {
        event.reservationIds.forEach { reservationId ->
            val payment = paymentRepository.findByReservationId(reservationId)
            payment?.let { compensatePayment(it.id) }
        }
    }
    ...
}
```

2. Reservation 보상 - 예약 상태를 취소로 변경
```kotlin
@Service
class ReservationService(private val reservationRepository: ReservationRepository) {
    ...
    @Transactional
    fun compensateReservation(reservationId: Long) {
        val reservation = reservationRepository.findById(reservationId).orElseThrow()
        reservation.status = ReservationStatus.CANCELLED
        reservationRepository.save(reservation)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun handleReservationFailed(event: SagaFailedEvent) {
        if (event.step == SagaStep.RESERVATION) {
            event.reservationIds.forEach { reservationId ->
                compensateReservation(reservationId)
            }
        }
    }
    ...
}
```

3. Queue 보상 - 대기열 토큰을 다시 활성 상태로 원복
```kotlin
@Service
class QueueService(private val redisTemplate: RedisTemplate<String, String>) {
    ...
    @Transactional
    fun compensateQueue(token: String) {
        // Redis에서 토큰 상태를 원복
        redisTemplate.opsForValue().set("queue:$token", QueueStatus.PROCESSING.name)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun handleQueueFailed(event: SagaFailedEvent) {
        if (event.step == SagaStep.QUEUE) {
            val token = getTokenFromReservations(event.reservationIds)
            compensateQueue(token)
        }
    }
}
```

4. Concert 보상 - 콘서트 상태 원복
```kotlin
@Service
class ConcertService(private val concertRepository: ConcertRepository) {
    ...
    @Transactional
    fun compensateConcertStatus(concertId: Long) {
        val concert = concertRepository.findById(concertId).orElseThrow()
        // 이전 상태로 롤백 (AVAILABLE로 변경)
        concert.status = ConcertStatus.AVAILABLE
        concertRepository.save(concert)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    fun handleConcertUpdateFailed(event: SagaFailedEvent) {
        if (event.step == SagaStep.CONCERT) {
            val concertId = getConcertIdFromReservations(event.reservationIds)
            compensateConcertStatus(concertId)
        }
    }
}
```

각 보상 트랜잭션은 해당 도메인 서비스에 위치하며, @TransactionalEventListener를 사용하여 실패 이벤트를 감지하고 처리한다.
이렇게 함으로써 각 서비스는 자신의 도메인에 대한 책임을 유지하면서도 전체 Saga 프로세스의 일관성을 보장할 수 있도록 한다.



<br>

**PaymentSagaOrchestrator**

- 이제, 쪼개진 서비스들을 엮어서 위대한 '서사 (Saga)' 를 구현할 `Orchestrator` 를 만들어보자

```kotlin
@Service
class PaymentSagaOrchestrator(private val eventPublisher: ApplicationEventPublisher) {
  fun startSaga(userId: Long, reservationIds: List<Long>) {
    eventPublisher.publishEvent(PaymentInitiatedEvent(userId, reservationIds))
  }

  @EventListener
  fun onSagaCompleted(event: ConcertStatusUpdatedEvent) {
    // Saga 완료 처리 로직
  }

  @EventListener
  fun onSagaFailed(event: SagaFailedEvent) {
    when (event.step) {
      SagaStep.PAYMENT -> {
        // 결제 실패 시, 추가 보상 트랜잭션 불필요
      }
      SagaStep.RESERVATION -> {
        // 예약 실패 시, 결제 취소
        eventPublisher.publishEvent(PaymentFailedEvent(event.reservationIds))
      }
      SagaStep.QUEUE -> {
        // 대기열 처리 실패 시, 예약 및 결제 취소
        eventPublisher.publishEvent(SagaFailedEvent(SagaStep.RESERVATION, event.reservationIds))
      }
      SagaStep.CONCERT -> {
        // 콘서트 상태 업데이트 실패 시, 대기열, 예약, 결제 취소
        eventPublisher.publishEvent(SagaFailedEvent(SagaStep.QUEUE, event.reservationIds))
      }
    }
  }
}

```
`PaymentSagaOrchestrator` 의 주요 흐름
- Saga 시작: `startSaga` 메소드를 통해 전체 프로세스를 시작한다. 결제 시작 이벤트를 발행하여 첫 단계를 트리거한다.

- Saga 완료 처리: `onSagaCompleted` 메소드는 콘서트 상태 업데이트가 완료되면 호출되어 전체 Saga 프로세스의 성공적인 완료를 처리하도록 한다.

- 실패 처리 및 보상 트랜잭션
    - `onSagaFailed` 메소드는 각 단계에서 발생할 수 있는 실패를 처리한다. 실패 지점에 따라 적절한 보상 트랜잭션을 시작한다.

<br>

#### 3-4 > Saga Pattern 으로 구현한 로직의 흐름 정리

>
- 사용자가 결제를 시작하면 `PaymentSagaOrchestrator.startSaga`가 호출된다.
  ⬇️
- `PaymentService`가 결제를 처리하고 결과 이벤트를 발행한다.
  ⬇️
- 결제 성공 시, `ReservationService가` 예약 상태를 업데이트한다.
  ⬇️
- 예약 업데이트 성공 시, `QueueService`가 대기열을 처리한다.
  ⬇️
- 대기열 처리 성공 시, `ConcertService`가 콘서트 상태를 업데이트한다.
  ⬇️
- 각 단계에서 실패가 발생하면 `PaymentSagaOrchestrator.onSagaFailed`가 호출되어 적절한 보상 트랜잭션을 시작한다.
  ⬇️
- 모든 단계가 성공적으로 완료되면 `PaymentSagaOrchestrator.onSagaCompleted`가 호출되어 최종 처리를 수행한다.


<br>

# 🙏🏻 2. 글을 마치며

8주차 과정은 정말 많은 것을 고민하고, 분석했다.
코드를 작성하는 것을 넘어, 내가 만들었던 기존 서비스를 MSA 관점에서 바라보고, 분석하고, 설계를 해봤다.
이러한 과정은 배우고 공부하는 과정에서 정말 필요하고 중요하다.
다음 주제는 이렇게 이벤트 기반으로 만든 어플리케이션을 Kafka 를 통해 관심사를 분리하는 것이다.

계속 성장을 향해, 더 좋은 개발자를 향해 정진. 또 정진.


### 지난 회고 보러가기
1주차 회고 - [테스트코드를 모르던 내게 찾아온 TDD](https://velog.io/@joshuara7235/%ED%85%8C%EC%8A%A4%ED%8A%B8%EC%BD%94%EB%93%9C%EB%A5%BC-%EB%AA%A8%EB%A5%B4%EB%8D%98-%EB%82%B4%EA%B2%8C-%EC%B0%BE%EC%95%84%EC%98%A8-TDD)

2주차 회고 - [코딩에 정답을 찾지말자. 고민을 통해 더 나아짐을 시작하자.](https://velog.io/@joshuara7235/%EC%BD%94%EB%94%A9%EC%97%90-%EC%A0%95%EB%8B%B5%EC%9D%84-%EC%B0%BE%EC%A7%80%EB%A7%90%EC%9E%90.-%EA%B3%A0%EB%AF%BC%EC%9D%84-%ED%86%B5%ED%95%B4-%EB%8D%94-%EB%82%98%EC%95%84%EC%A7%90%EC%9D%84-%EC%8B%9C%EC%9E%91%ED%95%98%EC%9E%90)

3주차 회고 - [좋은 코드를 위해서는 좋은 설계가 우선되어야 한다.](https://velog.io/@joshuara7235/%EC%A2%8B%EC%9D%80-%EC%BD%94%EB%93%9C%EB%A5%BC-%EC%9C%84%ED%95%B4%EC%84%9C%EB%8A%94-%EC%A2%8B%EC%9D%80-%EC%84%A4%EA%B3%84%EA%B0%80-%EC%9A%B0%EC%84%A0%EB%90%98%EC%96%B4%EC%95%BC-%ED%95%9C%EB%8B%A4)

4주차 회고 - [어플리케이션은 완벽할 수 없다. 다만 완벽을 지향할 뿐.](https://velog.io/@joshuara7235/%EC%96%B4%ED%94%8C%EB%A6%AC%EC%BC%80%EC%9D%B4%EC%85%98%EC%9D%80-%EC%99%84%EB%B2%BD%ED%95%A0-%EC%88%98-%EC%97%86%EB%8B%A4.-%EB%8B%A4%EB%A7%8C-%EC%99%84%EB%B2%BD%EC%9D%84-%EC%A7%80%ED%96%A5%ED%95%A0-%EB%BF%90)

5주차 회고 - [항해의 중간지점, 나는 얼마나 성장했나.](https://velog.io/@joshuara7235/%ED%95%AD%ED%95%B4%EC%9D%98-%EC%A4%91%EA%B0%84%EC%A7%80%EC%A0%90-%EB%82%98%EB%8A%94-%EC%96%BC%EB%A7%88%EB%82%98-%EC%84%B1%EC%9E%A5%ED%96%88%EB%82%98)

6주차 회고 - [동시성 문제를 극복해보자 - (feat. DB 락과 Redis 분산락)](https://velog.io/@joshuara7235/%EB%8F%99%EC%8B%9C%EC%84%B1-%EB%AC%B8%EC%A0%9C%EB%A5%BC-%EA%B7%B9%EB%B3%B5%ED%95%B4%EB%B3%B4%EC%9E%90-feat-DB-%EB%9D%BD%EA%B3%BC-Redis-%EB%B6%84%EC%82%B0%EB%9D%BD)

7주차 회고 - [대량의 트래픽이 몰려올 때 나는 어떻게 해야하나? - (feat. Cache, 대기열 구현)](https://velog.io/@joshuara7235/%EB%8C%80%EB%9F%89%EC%9D%98-%ED%8A%B8%EB%9E%98%ED%94%BD%EC%9D%B4-%EB%AA%B0%EB%A0%A4%EC%98%AC-%EB%95%8C-%EB%82%98%EB%8A%94-%EC%96%B4%EB%96%BB%EA%B2%8C-%ED%95%B4%EC%95%BC%ED%95%98%EB%82%98-feat.-Cache-%EB%8C%80%EA%B8%B0%EC%97%B4-%EA%B5%AC%ED%98%84)


### 항해에 관심이 있으시다구요?

항해플러스에서 벌써 백엔드 6기 모집이 시작된다고해요. ~~(내가 벌써 선배..?)~~
제 회고글을 모두 읽어 보신 분들은 잘 아시겠지만, 이 과정을 통해 정말 많은 것을 누리고, 배우고, 경험하고, 느끼고 있습니다.

솔직히 말씀드리면, 이 과정은 마냥 즐겁지는 않아요.
고통스럽고, 힘들고, 많이 지칩니다. 😔

더군다나 직장을 다니면서 병행한다면 잠을 포기하고 시간을 많이 갈아 넣어야해요.
하지만, 지금 열심히 항해중인 제가 감히 자신있게 말씀드리자면, 이 과정을 통해 지금까지 경험하지 못했던 압축된 성장을 경험할 수 있습니다.

혹시, 관심이 있으시다면 [지원하실 때](https://hhplus-hub.oopy.io/) 추천인 코드(**HHPGS0893**)를 작성해주신다면 할인이 된다고 해요 ㅎㅎ
고민되시는 분은, 댓글로 달아주시면 커피챗을 통해 이야기 해도 좋을 것 같습니다.

성장을 위해 시간을 쏟을 준비가 되신 주니어 분들에게 정말 진심을 다해 추천합니다.








