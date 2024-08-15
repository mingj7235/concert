# [외부 연동 API를 위한 Kafka와 Transactional Outbox Pattern 구현 보고서]


## 1. 개요
- 본 보고서는 기존 이벤트 기반으로 구현했던 외부 API 연동 (Slack 메세지 전송) 을 Apache Kafka 로 이관하면서 공부한 내용을 바탕으로 작성한다.
- 본 보고서는 결제 시스템에서 외부 API 연동을 위해 Apache Kafka를 사용한 이벤트 기반 아키텍처와 Transactional Outbox Pattern을 구현한 사례를 다룬다. 
- 이 접근 방식은 시스템의 확장성, 신뢰성, 그리고 데이터 일관성을 향상시키는 데 중점을 두고 있다.

<br>

## 2. 시스템 아키텍처 개요
Kafka 로 구현할 시스템은 다음과 같은 주요 컴포넌트로 구성할 계획이다.

- 결제 서비스 (Payment Service)
- Event Publisher
- Kafka Broker
- Event Listener
- Outbox 서비스
- Kafka Producer & Consumer
- 외부 API 연동 서비스 (Slack)

이 구조는 결제 처리부터 외부 시스템 통지까지의 전체 프로세스를 안정적으로 처리하는 것을 목표로 설계되었다.

<br>

## 3. 구현 상세
### 3.1 결제 프로세스

- 결제 프로세스는 PaymentManager 클래스에서 시작된다.
- 이 클래스는 결제 실행과 결과 저장을 담당한다.
```kotlin
@Component
class PaymentManager(
    private val paymentRepository: PaymentRepository,
    private val paymentHistoryRepository: PaymentHistoryRepository,
    @Qualifier("application") private val paymentEventPublisher: PaymentEventPublisher,
) {
    fun executeAndSaveHistory(
        user: User,
        requestReservations: List<Reservation>,
    ): List<Payment> {
        val payments = requestReservations.map { reservation ->
            runCatching {
                Payment(
                    user = user,
                    reservation = reservation,
                    amount = reservation.seat.seatPrice,
                    executedAt = LocalDateTime.now(),
                    paymentStatus = PaymentStatus.COMPLETED,
                ).let { paymentRepository.save(it) }
                    .also {
                        paymentEventPublisher.publishPaymentEvent(
                            PaymentEvent(it.id),
                        )
                    }
            }.getOrElse {
                // 실패 시 처리 로직
                Payment(
                    user = user,
                    reservation = reservation,
                    amount = reservation.seat.seatPrice,
                    executedAt = LocalDateTime.now(),
                    paymentStatus = PaymentStatus.FAILED,
                ).let { paymentRepository.save(it) }
                    .also {
                        paymentEventPublisher.publishPaymentEvent(
                            PaymentEvent(it.id),
                        )
                    }
            }
        }

        saveHistory(user, payments)

        return payments
    }
}
```
  
- 이 코드는 결제를 실행하고, 성공 또는 실패 여부에 관계없이 결과를 저장한다. 
- 주목할 점은 다음과 같다:
  - runCatching을 사용하여 예외 처리를 했다.
  - 결제 성공과 실패 모두에 대해 Payment 객체를 생성하고 저장한다.
  - 각 결제에 대해 이벤트를 발행한다. 이는 성공/실패 여부와 관계없이 이루어진다.
  - saveHistory 메소드를 통해 결제 이력을 별도로 저장한다.

<br>

### 3.2 이벤트 발행
- 결제 이벤트는 PaymentEventPublisher 인터페이스를 통해 발행된다.
```kotlin
interface PaymentEventPublisher {
    fun publishPaymentEvent(event: PaymentEvent)
}

@Component
@Qualifier("application")
class PaymentEventPublisherImpl(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : PaymentEventPublisher {
    override fun publishPaymentEvent(event: PaymentEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
```

- 이 구현은 스프링의 `ApplicationEventPublisher`를 사용하여 애플리케이션 내부에서 이벤트를 발행한다. 
- 이 방식의 장점은 다음과 같다:
  - 느슨한 결합: 이벤트 발행자와 구독자 간의 직접적인 의존성이 없다.
  - 확장성: 새로운 이벤트 구독자를 쉽게 추가할 수 있다.
  - 테스트 용이성: 단위 테스트 시 실제 이벤트 발행 없이 동작을 검증할 수 있다.

<br>

### 3.3 Transactional Outbox Pattern
```kotlin
@Component
class PaymentEventListener(
    private val paymentEventOutBoxService: PaymentEventOutBoxService,
) {
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun saveEventOutBoxForPaymentCompleted(event: PaymentEvent) {
        paymentEventOutBoxService.saveEventOutBox(
            domainId = event.paymentId,
            eventStatus = EventStatus.INIT,
        )
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publishReservationEvent(event: PaymentEvent) {
        paymentEventOutBoxService.publishPaymentEvent(event)
    }
}
```
- 이 리스너의 동작 방식은 다음과 같다:
  - `saveEventOutBoxForPaymentCompleted`
    - 트랜잭션 커밋 직전에 Outbox에 이벤트를 저장한다. 이는 메인 트랜잭션의 일부로 실행되어 데이터 일관성을 보장한다.
  - `publishReservationEvent`
    - 트랜잭션 커밋 후에 비동기적으로 Kafka 이벤트를 발행한다. 
    - 이 단계에서 실패하더라도 Outbox에 저장된 이벤트 덕분에 데이터 손실을 방지할 수 있다.

<br>

### 3.4 Kafka 이벤트 처리
- Kafka 이벤트 발행은 PaymentEventKafkaProducer 클래스에서 처리된다.
```kotlin
@Component
@Qualifier("kafka")
class PaymentEventKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val paymentEventOutBoxManager: PaymentEventOutBoxManager,
) : PaymentEventPublisher {
    override fun publishPaymentEvent(event: PaymentEvent) {
        kafkaTemplate
            .send("payment-event", event.paymentId.toString())
            .whenComplete { _, error ->
                if (error == null) {
                    paymentEventOutBoxManager.updateEventStatus(event.paymentId, EventStatus.PUBLISHED)
                }
            }
    }
}
```

- 이 클래스의 주요 특징은 다음과 같다:

  - Kafka에 메시지를 전송한다.
  - 전송 성공 시 Outbox의 상태를 `PUBLISHED`로 업데이트한다.
  - 비동기 처리를 통해 시스템의 응답성을 향상시킨다.

### 3.5 외부 API 연동 (Slack 메시지 전송)
- Kafka 이벤트 소비 및 외부 API 호출은 `PaymentEventKafkaConsumer` 클래스에서 처리된다.

```kotlin
@Component
class PaymentEventKafkaConsumer(
    private val paymentService: PaymentService,
) {
    @Async
    @KafkaListener(topics = ["payment-event"], groupId = "payment-group")
    fun handleSendMessageKafkaEvent(paymentId: String) {
        paymentService.sendPaymentEventMessage(paymentId.toLong())
    }
}
```
- 이 컨슈머의 역할은 다음과 같다:
  - Kafka 이벤트를 비동기적으로 수신한다.
  - 수신한 이벤트를 바탕으로 paymentService를 통해 Slack 메시지를 전송한다.
  - 이벤트 처리와 외부 API 호출을 분리하여 시스템의 결합도를 낮춘다.

<br>

## 4. Transactional Outbox Pattern 심층 분석
- Transactional Outbox Pattern은 분산 시스템에서 데이터 일관성과 메시지의 신뢰성 있는 전달을 보장하기 위한 디자인 패턴이다. 
- 이번 과제를 수행하면서 공부한 이 패턴의 철학과 목적을 깊이 있게 살펴보자.
   
### 4.1 Transactional Outbox Pattern 의 철학
- Transactional Outbox Pattern의 핵심 철학은 "최종적 일관성(Eventual Consistency)"과 "메시지 전달 보장(Guaranteed Message Delivery)"이다. 
- 이는 다음과 같은 분산 시스템의 핵심 과제를 해결하고자 한다:

#### 1. 데이터베이스 트랜잭션과 메시징 시스템 간의 불일치 해소
- 문제: 데이터베이스 업데이트와 메시지 발행이 별도의 시스템에서 일어나므로, 둘 중 하나만 성공하고 다른 하나는 실패할 수 있다.
- 해결: Outbox 테이블을 사용하여 메시지를 데이터베이스 트랜잭션의 일부로 저장함으로써 두 작업을 원자적으로 만든다.


#### 2. 시스템 장애 시 메시지 손실 방지
- 문제: 메시지 브로커로 직접 메시지를 보내다가 실패하면, 해당 메시지가 영구적으로 손실될 수 있다.
- 해결: 메시지를 먼저 데이터베이스에 저장하고, 이후에 발행함으로써 장애 상황에서도 메시지를 복구할 수 있다.


#### 3. 비즈니스 로직과 메시징 로직의 분리
- 문제: 비즈니스 로직과 메시지 발행 로직이 섞여 있으면 코드의 복잡도가 증가하고 유지보수가 어려워진다.
- 해결: Outbox 패턴을 통해 메시지 발행을 별도의 프로세스로 분리하여 관심사를 명확히 구분한다.

<br>

### 4.2 Transactional Outbox Pattern의 목적

#### 1. 데이터 일관성 보장
- 메인 트랜잭션과 메시지 저장이 원자적으로 이루어진다.
- 이를 통해 데이터베이스 상태와 발행된 메시지 간의 불일치를 방지한다.


#### 2. 메시지 손실 방지
- 모든 메시지가 데이터베이스에 안전하게 저장된다.
- 시스템 장애나 네트워크 문제로 인한 메시지 손실을 막을 수 있다.


#### 3. 순서 보장
- 메시지의 순서를 유지할 수 있다.
- 데이터베이스에 저장된 순서대로 메시지를 발행함으로써 이벤트의 시간적 순서를 보존한다.


#### 4.재시도 메커니즘
- 실패한 메시지에 대한 안정적인 재시도가 가능하다.
- Outbox 테이블을 주기적으로 폴링하여 미발행 메시지를 재시도할 수 있다.


#### 5.시스템 복잡도 관리:
- 비즈니스 로직과 메시징 로직을 분리하여 시스템의 복잡도를 낮춘다.
- 각 컴포넌트의 책임을 명확히 하여 유지보수성을 향상시킨다.

<br>

### 4.3 Concert - PaymentService 에서의 적용
#### 1. 결제 트랜잭션 실행 중 Outbox 테이블에 이벤트 저장
```kotlin
@Component
class PaymentEventListener(
    private val paymentEventOutBoxService: PaymentEventOutBoxService,
) {
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun saveEventOutBoxForPaymentCompleted(event: PaymentEvent) {
        paymentEventOutBoxService.saveEventOutBox(
            domainId = event.paymentId,
            eventStatus = EventStatus.INIT,
        )
    }
    // ...
}
```
- 이 코드는 결제 트랜잭션이 커밋되기 직전에 실행된다. 
- `@TransactionalEventListener` 어노테이션과 `TransactionPhase.BEFORE_COMMIT`을 사용하여 트랜잭션의 일부로 Outbox에 이벤트를 저장한다. 
- 이렇게 함으로써 결제 데이터 저장과 이벤트 저장이 원자적으로 이루어진다.

<br>

#### 2. 트랜잭션 커밋 후 비동기로 Kafka에 메시지 발행
```kotlin
@Component
class PaymentEventListener(
    private val paymentEventOutBoxService: PaymentEventOutBoxService,
) {
    // ...
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publishReservationEvent(event: PaymentEvent) {
        paymentEventOutBoxService.publishPaymentEvent(event)
    }
}
```
- 트랜잭션이 성공적으로 커밋된 후, `@Async` 어노테이션을 사용하여 비동기적으로 Kafka에 메시지를 발행한다. 
- 이는 메인 트랜잭션의 성능에 영향을 주지 않으면서도 위에서 언급한 이 패턴의 철학중 하나인 최종적 일관성(Eventual Consistency)을 보장한다.

<br>

#### 3. Kafka 발행 성공 시 Outbox 상태 업데이트
```kotlin
@Component
@Qualifier("kafka")
class PaymentEventKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val paymentEventOutBoxManager: PaymentEventOutBoxManager,
) : PaymentEventPublisher {
    override fun publishPaymentEvent(event: PaymentEvent) {
        kafkaTemplate
            .send("payment-event", event.paymentId.toString())
            .whenComplete { _, error ->
                if (error == null) {
                    paymentEventOutBoxManager.updateEventStatus(event.paymentId, EventStatus.PUBLISHED)
                }
            }
    }
}
```
- Kafka에 메시지를 성공적으로 발행한 후, Outbox의 상태를 `PUBLISHED`로 업데이트한다. 
- 이는 `whenComplete` 콜백을 사용하여 비동기적으로 처리된다. 
- 이렇게 함으로써 어떤 메시지가 성공적으로 발행되었는지 추적할 수 있다.

<br>

#### 4. 스케쥴러를 통해 주기적으로 실패한 이벤트 재시도 및 오래된 이벤트 정리
```kotlin
@Component
class PaymentEventScheduler(
    private val paymentEventOutBoxService: PaymentEventOutBoxService,
) {
    @Scheduled(fixedRate = 60000)
    fun retryFailedPaymentEvent() {
        logger.info("Retry Failed Payment Event Scheduler Executed")
        paymentEventOutBoxService.retryFailedPaymentEvent()
    }
    
    @Scheduled(fixedRate = 60000)
    fun deletePublishedPaymentEvent() {
        logger.info("Delete Publish Payment Event Scheduler Executed")
        paymentEventOutBoxService.deletePublishedPaymentEvent()
    }
}

@Service
class PaymentEventOutBoxService(
    private val paymentEventOutBoxManager: PaymentEventOutBoxManager,
    @Qualifier("kafka") private val paymentEventPublisher: PaymentEventPublisher,
) {
    // ...
    fun retryFailedPaymentEvent() {
        paymentEventOutBoxManager.retryFailedPaymentEvent().forEach {
            paymentEventPublisher.publishPaymentEvent(PaymentEvent(it.paymentId))
        }
    }

    fun deletePublishedPaymentEvent() {
        paymentEventOutBoxManager.deletePublishedPaymentEvent()
    }
}
```
- 스케쥴러를 통해 주기적으로 실행되어 실패한 이벤트를 재시도하고, 오래된 이벤트를 정리한다. 
- `retryFailedPaymentEvent` 메소드는 일정 시간이 지나도 `INIT `상태인 이벤트를 찾아 재발행을 시도한다. 
- `deletePublishedPaymentEvent` 메소드는 발행 후 일정 시간이 지난 이벤트를 삭제하여 Outbox 테이블의 크기를 관리한다.
  - 다만, 이 방법은 도리어 DB의 문제를 가져올 수 있으므로 좀 더 신중한 고민이 필요하다.

<br>

### 4.4 Transactional Outbox Pattern 을 통해 얻은 이점
- 데이터 일관성: 결제 데이터와 이벤트 데이터가 항상 일치한다.
- 안정성: 시스템 장애 상황에서도 메시지 손실을 방지할 수 있다.
- 성능: 메시지 발행이 비동기적으로 이루어져 메인 트랜잭션의 성능에 영향을 주지 않는다.
- 확장성: Kafka를 사용함으로써 대량의 메시지 처리가 가능하다.

이러한 접근 방식을 통해 시스템의 안정성과 확장성을 크게 향상시킬 수 있었다. 특히 대규모 트래픽 상황에서도 메시지의 안정적인 전달과 데이터 일관성을 보장할 수 있게 되었다.

<br>


## 5. 결론
- 이번 과제를 통해 Kafka와 Transactional Outbox Pattern을 사용하여 안정적이고 확장 가능한 이벤트 기반 아키텍처를 구현했다. 
- 이를 통해 다음과 같은 이점을 얻을 수 있었다
  - 높은 신뢰성: 메시지 손실 없이 안정적인 이벤트 처리
  - 확장성: Kafka를 통한 대규모 이벤트 처리 가능
  - 데이터 일관성: Transactional Outbox Pattern을 통한 데이터 일관성 보장
  - 시스템 분리: 결제 처리와 외부 시스템 사용의 분리

    