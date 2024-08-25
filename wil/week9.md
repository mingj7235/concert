# Kafka 를 찍먹해보자. - feat. Transactional Outbox Pattern

# 🌱 0. 들어가며.

### 고--오--급 기술 Kafka

어느 개발자 단톡방이었나,
누군가 이렇게 말했던 것이 기억난다.

>"Kafka 와 같은 고--오--급 기술을 사용하실 줄 알면 연봉도 고--오--급이 된답니다"

물론, 장난섞인 이야기였겠지만 개인적으로 Kafka 라는 기술의 장벽이 크게 느껴졌었다.
그래서 그러다보니 kafka 가 '고--오--급 기술' 이라는 말에 '그렇군!' 했던 것 같다.
이름은 너무도 잘 알고 있었던 그 녀석 Kafka.
하지만 뭔가 해보려고하면 이것저것 부가적으로 세팅하고 준비하고 공부할 것들이 많아보였던 Kafka.

그리고, 드디어 항해플러스 과제를 통해 그놈의 Kafka 를 직접 만져보게 되었다.

<br>

# 🍇 1. 9주차 항해 회고

### 그래서 이번 과제는 무엇을 해야하는고..? 🧐

![](https://velog.velcdn.com/images/joshuara7235/post/392c5840-c433-4632-ba0b-3026022d4afb/image.png)

9주차의 요구사항은 다음과 같았다.

>**Step 17**
- Kafka 설치
- consumer, producer 를 구현하고 잘 동작하는지 테스트할 것

위 요구사항은 어렵지 않아보였다.
Kafka 와 Zookeeper 를 Docker 로 띄운 후, 테스트 consumer 와 producer 를 구현하고 잘 응답이 오는지만 확인하면 되어 보였다.

>**Step 18**
- Transactional Outbox Pattern 을 사용해서 카프카를 통해 메세지를 발행할 것.
- 발행 실패에 대한 대비를 할 것.

이번 과제의 핵심은 바로 'Transactional Outbox Pattern' 을 통해 Kafka 를 사용한 이벤트 기반의 서비스를 구현하는 것이었다.

맛있어 보였다. 😋

나는 Kafka 를 통해 결제가 완료된 이후의 외부 메세지 API (Slack) 을 보내도록 로직을 만들어볼 생각이다.

<br>

### 💎 결제 프로세스 로직 - 핵심 비지니스 로직

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

- 결제 프로세스는 `PaymentManager` 클래스에서 시작된다.
- 이 클래스는 결제 실행과 결과 저장을 담당한다.

- 이 코드는 결제를 실행하고, 성공 또는 실패 여부에 관계없이 결과를 저장한다.

>runCatching을 사용하여 예외 처리를 했다.
결제 성공과 실패 모두에 대해 Payment 객체를 생성하고 저장한다.
각 결제에 대해 이벤트를 발행한다. 이는 성공/실패 여부와 관계없이 이루어진다.
saveHistory 메소드를 통해 결제 이력을 별도로 저장한다.

<br>

### 💌 결제 이벤트를 발행해보자. - '나 결제 완료 되었어!'

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
- 결제 이벤트는 `PaymentEventPublisher` 인터페이스를 통해 발행된다.
- 이 구현은 스프링의 `ApplicationEventPublisher`를 사용하여 애플리케이션 내부에서 이벤트를 발행한다.

<br>

### 🔑 Transactional Outbox Pattern

Transactional Outbox Pattern (이하 TOP) 을 사용한 코드 구현을 하기 전에, TOP가 무엇인지 알아보자.
TOP는 분산 시스템에서 **_데이터 일관성과 메시지의 신뢰성 있는 전달을 보장하기 위한_** 하나의 디자인 패턴이다.
핵심 철학은 **"최종적 일관성(Eventual Consistency)"**과 "**메시지 전달 보장(Guaranteed Message Delivery)"**이다.

> **[TOP 을 통해 해결하고자 하는 것]**
> 1. 데이터베이스 트랜잭션과 메시징 시스템 간의 불일치 해소
- 문제: 데이터베이스 업데이트와 메시지 발행이 별도의 시스템에서 일어나므로, 둘 중 하나만 성공하고 다른 하나는 실패할 수 있다.
- 해결: Outbox 테이블을 사용하여 메시지를 데이터베이스 트랜잭션의 일부로 저장함으로써 두 작업을 원자적으로 만든다.
> 2. 시스템 장애 시 메시지 손실 방지
- 문제: 메시지 브로커로 직접 메시지를 보내다가 실패하면, 해당 메시지가 영구적으로 손실될 수 있다.
- 해결: 메시지를 먼저 데이터베이스에 저장하고, 이후에 발행함으로써 장애 상황에서도 메시지를 복구할 수 있다.
> 3. 비즈니스 로직과 메시징 로직의 분리
- 문제: 비즈니스 로직과 메시지 발행 로직이 섞여 있으면 코드의 복잡도가 증가하고 유지보수가 어려워진다.
- 해결: Outbox 패턴을 통해 메시지 발행을 별도의 프로세스로 분리하여 관심사를 명확히 구분한다.

<br>

즉, TOP 의 목적은 다음과 같다.

>**1.데이터 일관성 보장**
- 메인 트랜잭션과 메시지 저장이 원자적으로 이루어진다.
- 이를 통해 데이터베이스 상태와 발행된 메시지 간의 불일치를 방지한다.

>**2. 메시지 손실 방지**
- 모든 메시지가 데이터베이스에 안전하게 저장된다.
- 시스템 장애나 네트워크 문제로 인한 메시지 손실을 막을 수 있다.

>**3. 순서 보장**
- 메시지의 순서를 유지할 수 있다.
- 데이터베이스에 저장된 순서대로 메시지를 발행함으로써 이벤트의 시간적 순서를 보존한다.

>**4.재시도 메커니즘**
- 실패한 메시지에 대한 안정적인 재시도가 가능하다.
- Outbox 테이블을 주기적으로 폴링하여 미발행 메시지를 재시도할 수 있다.

>**5.시스템 복잡도 관리**
- 비즈니스 로직과 메시징 로직을 분리하여 시스템의 복잡도를 낮춘다.
- 각 컴포넌트의 책임을 명확히 하여 유지보수성을 향상시킨다.

<br>

그럼 이제, TOP 를 적용하여 코드를 작성해보자.

#### 1> 결제 트랜잭션 실행 중 Outbox 테이블에 이벤트를 저장한다.
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

#### 2> 트랜잭션 커밋 후 비동기로 kafka 에게 메세지를 발행한다.
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

#### 3> Kafka 발행 성공 시 Outbox 의 상태를 업데이트 한다.
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

#### 4> 스케쥴러를 통해 주기적으로 실패한 이벤트를 재시도하고, 오래된 이벤트를 정리하도록 한다.

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
- `retryFailedPaymentEvent` 메소드는 일정 시간이 지나도 `INIT` 상태인 이벤트를 찾아 재발행을 시도한다.
- `deletePublishedPaymentEvent` 메소드는 발행 후 일정 시간이 지난 이벤트를 삭제하여 Outbox 테이블의 크기를 관리한다.
    - 다만, 이 방법은 도리어 DB의 문제를 가져올 수 있으므로 좀 더 신중한 고민이 필요하다.

<br>

### 💡 Transactional Outbox Pattern 을 통해 얻은 이점
- **데이터 일관성**: 결제 데이터와 이벤트 데이터가 항상 일치한다.
- **안정성**: 시스템 장애 상황에서도 메시지 손실을 방지할 수 있다.
- **성능**: 메시지 발행이 비동기적으로 이루어져 메인 트랜잭션의 성능에 영향을 주지 않는다.
- **확장성**: Kafka를 사용함으로써 대량의 메시지 처리가 가능하다.

이러한 접근 방식을 통해 시스템의 안정성과 확장성을 크게 향상시킬 수 있었다. 특히 대규모 트래픽 상황에서도 메시지의 안정적인 전달과 데이터 일관성을 보장할 수 있게 되었다.

<br>

# 🎊 2. 드디어 블랙 벳지를..!

![](https://velog.velcdn.com/images/joshuara7235/post/8b35e41c-128b-48ef-914f-4f02e8a7ff1c/image.png)

9주차까지 모든 과제 ALL PASS!

항해플러스 과정을 통해 얻을 수 있는 가장 높은 등급인 블랙 벳지를 이번 과제를 모두 통과함으로써 받게 되었다. 만세! 🎊
처음 시작을 하면서 목표로 했던 가장 최고 등급을 보다 일찍 받게 되어 뿌듯했다.

다음 포스팅은 항해 전체 후기와 회고, 그리고 수료식을 하며 느낀 점을 적어볼 생각이다.

### 지난 회고 보러가기
1주차 회고 - [테스트코드를 모르던 내게 찾아온 TDD](https://velog.io/@joshuara7235/%ED%85%8C%EC%8A%A4%ED%8A%B8%EC%BD%94%EB%93%9C%EB%A5%BC-%EB%AA%A8%EB%A5%B4%EB%8D%98-%EB%82%B4%EA%B2%8C-%EC%B0%BE%EC%95%84%EC%98%A8-TDD)
2주차 회고 - [코딩에 정답을 찾지말자. 고민을 통해 더 나아짐을 시작하자.](https://velog.io/@joshuara7235/%EC%BD%94%EB%94%A9%EC%97%90-%EC%A0%95%EB%8B%B5%EC%9D%84-%EC%B0%BE%EC%A7%80%EB%A7%90%EC%9E%90.-%EA%B3%A0%EB%AF%BC%EC%9D%84-%ED%86%B5%ED%95%B4-%EB%8D%94-%EB%82%98%EC%95%84%EC%A7%90%EC%9D%84-%EC%8B%9C%EC%9E%91%ED%95%98%EC%9E%90)
3주차 회고 - [좋은 코드를 위해서는 좋은 설계가 우선되어야 한다.](https://velog.io/@joshuara7235/%EC%A2%8B%EC%9D%80-%EC%BD%94%EB%93%9C%EB%A5%BC-%EC%9C%84%ED%95%B4%EC%84%9C%EB%8A%94-%EC%A2%8B%EC%9D%80-%EC%84%A4%EA%B3%84%EA%B0%80-%EC%9A%B0%EC%84%A0%EB%90%98%EC%96%B4%EC%95%BC-%ED%95%9C%EB%8B%A4)
4주차 회고 - [어플리케이션은 완벽할 수 없다. 다만 완벽을 지향할 뿐.](https://velog.io/@joshuara7235/%EC%96%B4%ED%94%8C%EB%A6%AC%EC%BC%80%EC%9D%B4%EC%85%98%EC%9D%80-%EC%99%84%EB%B2%BD%ED%95%A0-%EC%88%98-%EC%97%86%EB%8B%A4.-%EB%8B%A4%EB%A7%8C-%EC%99%84%EB%B2%BD%EC%9D%84-%EC%A7%80%ED%96%A5%ED%95%A0-%EB%BF%90)
5주차 회고 - [항해의 중간지점, 나는 얼마나 성장했나.](https://velog.io/@joshuara7235/%ED%95%AD%ED%95%B4%EC%9D%98-%EC%A4%91%EA%B0%84%EC%A7%80%EC%A0%90-%EB%82%98%EB%8A%94-%EC%96%BC%EB%A7%88%EB%82%98-%EC%84%B1%EC%9E%A5%ED%96%88%EB%82%98)
6주차 회고 - [동시성 문제를 극복해보자 - (feat. DB 락과 Redis 분산락)](https://velog.io/@joshuara7235/%EB%8F%99%EC%8B%9C%EC%84%B1-%EB%AC%B8%EC%A0%9C%EB%A5%BC-%EA%B7%B9%EB%B3%B5%ED%95%B4%EB%B3%B4%EC%9E%90-feat-DB-%EB%9D%BD%EA%B3%BC-Redis-%EB%B6%84%EC%82%B0%EB%9D%BD)
7주차 회고 - [대량의 트래픽이 몰려올 때 나는 어떻게 해야하나? - (feat. Cache, 대기열 구현)](https://velog.io/@joshuara7235/%EB%8C%80%EB%9F%89%EC%9D%98-%ED%8A%B8%EB%9E%98%ED%94%BD%EC%9D%B4-%EB%AA%B0%EB%A0%A4%EC%98%AC-%EB%95%8C-%EB%82%98%EB%8A%94-%EC%96%B4%EB%96%BB%EA%B2%8C-%ED%95%B4%EC%95%BC%ED%95%98%EB%82%98-feat.-Cache-%EB%8C%80%EA%B8%B0%EC%97%B4-%EA%B5%AC%ED%98%84)
8주차 회고 - [MSA를 찍먹해보자. - feat. Saga Pattern](https://velog.io/@joshuara7235/MSA%EB%A5%BC-%EC%B0%8D%EB%A8%B9%ED%95%B4%EB%B3%B4%EC%9E%90.-feat.-Saga-Pattern)


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




