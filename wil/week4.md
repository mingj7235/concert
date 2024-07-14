# 항해플러스 백엔드 4주차 후기

# 🌱 0. 들어가며

### ⏰ 시간아 조금만 느리게 가줘
시간이 참 빠르다.
날짜를 확인할 때 7월이라는 것도 낯설었는데 벌써 7월 중순이다.
항해도 시작한지 얼마 되지 않은 것 같은데 벌써 4주차를 마무리하고 5주차를 향해 가고 있다.

이번 주차 회고를 쓰기 전 지금까지 쓴 회고 글을 읽어봤다.
항해 여정을 통해 내가 무엇을 배웠고, 어떤걸 했는지 돌이켜보면 짧은 시간이지만 꽤나 성장한 것 처럼 느껴진다.

특히, 백엔드 엔지니어로서 요구사항을 분석하고 각각의 도메인의 관점에서 어떤 책임을 가지고 있는지 분석하고 고민하고 이해하는 경험을 했다고 생각한다. 그리고 어떤 방향성을 가지고 고민해야하는지를 조금씩 느껴나가는 것 같다.
새삼, 조영호님의 `객체지향의 사실과 오해` 에서 읽었던 내용들이 단순한 텍스트를 넘어 '아 이게 이런 느낌인건가?' 라는 생각도 들엇다.
이렇게 성장하는 거겠지..? 😋

항해가 아니었다면, 어떤 방향성을 가지고 백엔드 엔지니어로서의 역량을 기르고 공부해야할지 갈피를 잡지 못했을 것 같다.
진짜 너무 빡세고 가끔 포기해 버리고 싶기도 하지만, 매주 이렇게 회고를 할 때마다 느끼는 거지만 항해를 하기 정말 잘했다고 생각한다.


### 🔧 이번주에 주어진 과제들..

<div>
<img src="https://velog.velcdn.com/images/joshuara7235/post/348abc3d-5e70-49b8-bfb9-20f39b583007/image.png" width="80%" height="n%">
</div>


이번주는 간단히 말하면, 저번주에 설계했던 '콘서트 예약 시스템' 의 서버를 실제로 구축하는 것이었다.
STEP 7 은 Swagger 를 붙이는 것이라서 간단했지만, 실체는 STEP 8 였다.
비지니스 Usecase 를 개발하고 테스트를 작성하는 것.

`기능 개발의 완료` 라는 말이 이번주 내내 나에게 부담이 되어 눌렀다. 🫠


<br>

# 🍒 1. 4주차 항해 여정 회고

### 가장 빡셌던 이번 주..

<div>
<img src="https://velog.velcdn.com/images/joshuara7235/post/b99a8f04-1936-416c-a8ee-341dff7a29b0/image.png" width="80%" height="n%">
</div>


7월 12일 금요일 새벽 3시 10분.
마지막 과제 PR 을 올리고 바로 침대에 쓰러졌다.

이번주는 지금까지 항해 여정 중에서 가장 빡셌었다.
나만 그렇게 느낀 것이 아니라 항해를 함께 하는 대부분의 항해러들의 감상이 그랬다.



><div>
<img src="https://velog.velcdn.com/images/joshuara7235/post/2ff9cb3b-94a0-4de8-b4d8-138815d2e7ff/image.png" width="80%" height="n%">
</div>
- 윤XX 님의 이번주 감상....🫢


저번 주에 몸이 너무 안좋아져서 최대한 무리를 하지 않으려고 했지만
과제 제출 마지막 날인 목요일은 다음날 새벽3시까지 겨우 해서야 과제를 제출 할 수 있었다.


### 과제를 하며 챌린지 되었던 지점

**1. Token 을 발급하고 그 Token 을 어디서 어떻게 검증할 것인가? **

<div>
<img src="https://velog.velcdn.com/images/joshuara7235/post/ca263d44-9fa6-488f-b1e3-1d14eeaf55a8/image.png" width="80%" height="n%">
</div>


💡 고안해본 부분
- `Custom Annotation` 을 만든다.
- `Interceptor` 를 통해 Token 을 검증하도록 한다.
- `Resolver` 를 통해 검증된 Token 을 받아와서 비지니스 레이어에 전달하도록 한다.



```kotlin
@Component
class TokenInterceptor(
    private val jwtUtil: JwtUtil,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler is HandlerMethod) {
            val requireToken =
                handler.hasMethodAnnotation(TokenRequired::class.java) ||
                    handler.beanType.isAnnotationPresent(TokenRequired::class.java)

            if (!requireToken) {
                return true
            }

            val token = request.getHeader("QUEUE-TOKEN")
            if (token == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "QUEUE-TOKEN is missing")
                return false
            }

            if (!isValidToken(token)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid QUEUE-TOKEN")
                return false
            }

            request.setAttribute("validatedToken", token)
        }
        return true
    }
    
@Component
class ValidatedTokenResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean = parameter.hasParameterAnnotation(ValidatedToken::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? = webRequest.getAttribute("validatedToken", RequestAttributes.SCOPE_REQUEST)
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TokenRequired

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValidatedToken

```

`@TokenRequired` 는 토큰이 헤더에 있는지 확인하고, 유효한지 검증하도록 한다.
`@ValidatedToken` 은 검증된 토큰을 꺼내와서 비지니스 레이어에 전달하도록 한다.


 ```kotlin
커스텀 어노테이션 실제 사용 예시
	/**
     * 콘서트 예약 가능 날짜 목록을 조회한다.
     */
    @TokenRequired
    @GetMapping("/concerts/{concertId}/schedules")
    fun getConcertSchedules(
        @ValidatedToken token: String,
        @PathVariable concertId: Long,
    ): ConcertResponse.Schedule =
        ConcertResponse.Schedule.from(
            concertService.getConcertSchedules(
                token = token,
                concertId = concertId,
            ),
        )
```
- `@TokenRequired` 가 붙어 있으므로, 만료되지 않고 유효한 토큰이 헤더에 있는지 확인한다.
- `@ValidatedToken` 을 통해 꺼내온 token 값을 서비스레이어에 전달한다.
    - 서비스레이어에서 해당 token 으로 대기열을 찾고, 그 대기열을 검증한다.


**2. 대기열의 상태를 어떻게 관리할 것인가?**

대기열의 상태를 어떻게 관리할지 고민을 하다가 스케쥴러에게 지속적인 검증과 상태 변경을 위임했다.

>```kotlin
/**
* 스케쥴러를 통해 queue 의 Proccessing 상태인 상태의 queue 개수를 유지시킨다.
  */
  @Transactional
  fun maintainProcessingCount() {
  val neededToUpdateCount = ALLOWED_MAX_SIZE - queueManager.countByQueueStatus(QueueStatus.PROCESSING)
  if (neededToUpdateCount > 0) {
  queueManager.updateStatus(
  queueIds = queueManager.getNeededUpdateToProcessingIdsFromWaiting(neededToUpdateCount),
  queueStatus = QueueStatus.PROCESSING,
  )
  }
  }
  // 스케쥴러
  @Component
  class QueueScheduler(
  private val queueService: QueueService,
  ) {
  @Scheduled(fixedRate = 60000)
  fun maintainProcessingCount() {
  queueService.maintainProcessingCount()
  }
  }
```
- Proccessing 상태인 대기열의 개수를 1분마다 확인해서 그 수를 유지시킨다.
```

**3. 예약과 좌석의 상태를 어떻게 관리할 것인가?**

<div>
<img src="https://velog.velcdn.com/images/joshuara7235/post/11695403-1b0d-4621-beaf-a1853dedc885/image.png" width="80%" height="n%">
</div>

```kotlin
    /**
     * 결제를 진행한다.
     * 1. reservation 의 user 와, payment 를 요청하는 user 가 일치하는지 검증
     * 2. payment 수행하고 paymentHistory 에 저장
     * 3. reservation 상태 변경
     * 4. 토큰의 상태 변경 -> completed
     */
    @Transactional
    fun executePayment(
        token: String,
        userId: Long,
        reservationIds: List<Long>,
    ): List<PaymentServiceDto.Result> {
        val user = userManager.findById(userId)
        val requestReservations = reservationManager.findAllById(reservationIds)

        // 결제 요청을 시도하는 user 와 예악한 목록의 user 가 일치하는지 확인한다.
        if (requestReservations.any { it.user.id != userId }) {
            throw PaymentException.InvalidRequest()
        }

        // 결제를 한다.
        val executedPayments =
            paymentManager.execute(
                user,
                requestReservations,
            )

        // 결제 내역을 저장한다.
        paymentManager.saveHistory(user, executedPayments)

        // reservation 상태를 PAYMENT_COMPLETED 로 변경한다.
        reservationManager.complete(requestReservations)

        // queue 상태를 COMPLETED 로 변경한다.
        val queue = queueManager.findByToken(token)
        queueManager.updateStatus(queue, QueueStatus.COMPLETED)

        // 결과를 반환한다.
        return executedPayments.map {
            PaymentServiceDto.Result(
                paymentId = it.id,
                amount = it.amount,
                paymentStatus = it.paymentStatus,
            )
        }
    }
```
- 위의 플로우로 결제를 진행했다.
- 결제가 성공적으로 진행되면, 대기열의 상태를 Completed 로 변환 시키고, 스케쥴러를 통해 Processing 상태의 대기열의 수를 유지시키도록 한다.



### 💎4주차 과제도 PASS !

<div>
<img src="https://velog.velcdn.com/images/joshuara7235/post/c06df9f2-c497-49d2-a15d-2a8441b65e36/image.png" width="80%" height="n%">
</div>

이번주까지 모든 과제를 통과했다.
개인적으로 이번 주 과제가 정말 빡셌고 할 일들이 많았기 때문에 통과했다는 것에 더 큰 쾌감이 있었다.

<br>


# 🍎 2. 어플리케이션은 완벽할 수 없다. 다만 완벽을 지향할 뿐.

### 실제 로직을 구현하면서 깨달은 것들

이번주는 저번주 설계한 내용을 바탕으로 실제 비지니스 로직을 구현을 했다.
처음 로직을 구현하고 나서 주어진 조건내에서 완벽하게 구현했다고 생각했다.
그렇게 밤늦게 코딩을하고 자고 일어나면 다시 그 로직의 빈틈이 보이고, 실패 테스트를 작성해보니 완벽하지 않다는 것을 발견했다.
이런 플로우가 각각의 기능개발을 진행하면서 동일하게 몇번이고 반복되었다.

특히, 이번 과제의 '대기열' 관련 로직이 그랬다.
처음 생각에는 구현을 하면서 이 정도면 요구사항대로 잘 구현한 것 같았는데, 다음날 또 관점과 생각이 달라지고 빈틈이 보여서 여러번 수정을 거듭했다.

이러한 과정을 반복하면서 깨달은 점은 '어플리케이션은 완벽할 수 없다.' 는 것이었다.
로직은 완벽할 수 없다.
혹여 그 당시 어플리케이션이 완벽하더라도, _**서비스가 커져가고 발전해나가는 이상 변경은 불가피하다.**_

백엔드 개발자로서 중요한 역량중 하나는 **요구사항을 잘 분석하는 것**이었다.
이것이 저번주 설계를 해보면서 느끼고 깨달은 것이었다.
그렇다면, 그 요구사항의 분석에 기반해서 만든 로직을 서비스의 성장과 변화에 따라 주기적으로 관리하고 변경을 하는 것도 큰 역량이라고 생각이 들었다.
그리고 그러기 위해서는 변경에 유연하도록 확장성있게 처음 설계를 잘 하는 것이 중요하다고 생각이 들었다.

끊임없이 **내가 짠 코드와 어플리케이션에 책임을 가지고 고민**해야한다.
그리고 그러기 위해서는 내 코드를 누가 읽어도 이해하기 쉽고 합리적으로 만들어야한다.
그렇기 때문에 더더욱 좋은 설계, 좋은 아키텍쳐, 께끗한 코드에 대해 강조하고 중요하다고 하는 것이구나 라고 생각했다.

개발자가 만든 어플리케이션은 결코 완벽할 수 없다.
만들어 놓은 그대로 영원할 수 없다.
서비스는 진화하고, 요구사항은 변경된다.

만들 때 완벽을 지향하면서 만들지만, 혹여 완벽하다고 생각하는 오만함으로 코드의 변경을 두려워하거나 거부해서는 안된다.
_**'내 코드를 사랑하지 말라'**_는 말이 그래서 나왔구나 라고도 생각이 들었다.


<br>



# 🍭 3. 토요지식회 - 항해에서 내 이야기를 나누다.

<div style="display: flex; justify-content: space-between;">
<img src="https://velog.velcdn.com/images/joshuara7235/post/340ba109-0d7f-4644-a1d3-76307d1617a9/image.jpg" width="48%" height="n%">
<img src="https://velog.velcdn.com/images/joshuara7235/post/0c73ebef-8344-441d-acb1-fbd7c8f16f13/image.jpg" width="48%" height="n%">
</div>

이번 항해 정기 모임에서 내 이야기를 나누게 되었다.
개인적으로 개발자 커뮤니티에서 인사이트를 나누게되어 참 기쁘고 영광스러웠다.
예전에 동문 개발자 모임에서 발표했던 ['법대생이었던 내가 일어나보니 개발자가 된 건에 대하여'](https://velog.io/@joshuara7235/%EB%B2%95%EB%8C%80%EC%83%9D%EC%9D%B4%EC%97%88%EB%8D%98-%EB%82%B4%EA%B0%80-%EC%9D%BC%EC%96%B4%EB%82%98%EB%B3%B4%EB%8B%88-%EA%B0%9C%EB%B0%9C%EC%9E%90%EA%B0%80-%EB%90%9C-%EA%B1%B4%EC%97%90-%EB%8C%80%ED%95%98%EC%97%AC) 라는 주제로 발표를 했다.

법대생 출신에 30대에 개발자가 된 내 삽질 인생 이야기..
발표는 언제나 떨리지만, 내 이야기를 개발자들에게 나누고, 그들에게 조금이라도 인사이트를 줄 수 있다는 것이 기뻤다.


# 🙏🏻 4. 글을 마치며

이번 한 주는 정말 뿌듯했다.
지금까지 완수했던 과제중에 가장 힘들었고 내용이 많았었고, 그것들을 모두 해내었다.
그리고 항해에서 내 이야기까지 나눌 수 있었던 이번 주.

다음주는 드디어 Chapter2 가 마무리 된다.
이번주의 과제 주제인 Logging, Exception Handling 까지 잘 마무리해서 훌륭한 서버를 구축하고 싶다.

이번 한 주도 화이팅!!

### 지난 회고 보러가기
1주차 회고 - [테스트코드를 모르던 내게 찾아온 TDD](https://velog.io/@joshuara7235/%ED%85%8C%EC%8A%A4%ED%8A%B8%EC%BD%94%EB%93%9C%EB%A5%BC-%EB%AA%A8%EB%A5%B4%EB%8D%98-%EB%82%B4%EA%B2%8C-%EC%B0%BE%EC%95%84%EC%98%A8-TDD)

2주차 회고 - [코딩에 정답을 찾지말자. 고민을 통해 더 나아짐을 시작하자.](https://velog.io/@joshuara7235/%EC%BD%94%EB%94%A9%EC%97%90-%EC%A0%95%EB%8B%B5%EC%9D%84-%EC%B0%BE%EC%A7%80%EB%A7%90%EC%9E%90.-%EA%B3%A0%EB%AF%BC%EC%9D%84-%ED%86%B5%ED%95%B4-%EB%8D%94-%EB%82%98%EC%95%84%EC%A7%90%EC%9D%84-%EC%8B%9C%EC%9E%91%ED%95%98%EC%9E%90)

3주차 회고 - [좋은 코드를 위해서는 좋은 설계가 우선되어야 한다.](https://velog.io/@joshuara7235/%EC%A2%8B%EC%9D%80-%EC%BD%94%EB%93%9C%EB%A5%BC-%EC%9C%84%ED%95%B4%EC%84%9C%EB%8A%94-%EC%A2%8B%EC%9D%80-%EC%84%A4%EA%B3%84%EA%B0%80-%EC%9A%B0%EC%84%A0%EB%90%98%EC%96%B4%EC%95%BC-%ED%95%9C%EB%8B%A4)


















