# 항해플러스 백엔드 5주차 후기

# 🌱 0. 들어가며
### ❤️‍🔥 Chapter 2 가 끝났다.
마침내, 항해 플러스 백엔드 5기 과정의 Chapter2 가 끝났다. 👏🏻

Chapter 2가 끝났다는 것은, 항해를 하는 동안 만들고 완성해 나갈 서비스의 큰 뼈대가 완성되었다는 말이다.
내가 선택했던 서비스의 시나리오는 _**'콘서트 대기열 시스템'**_ 이었고,
이 서비스의 요구사항에 맞춰 Usecase 를 설계하고 개발하는 것이 이번 챕터의 목적이었다.

기간은 총 3주였고, 그 기간내에 과제를 완수하는 것은..
게다가 직장을 다니면서 모든 과제를 다 해내는 것은....

넘나, 넘나, 넘나리.. 힘든일이었다..🥹

>![](https://velog.velcdn.com/images/joshuara7235/post/a1a1dffa-0ee2-4238-9222-57e6abada7e6/image.png)
- 지난 3주간의 여정을 담은 PR들..
- 지금 회고를 쓰면서 다시 들어가서 보는데 뭔지 모를 뿌듯함이 몰려온다.


### 💎 Chapter 3 에서 내가 공부 할 것
Chapter2 를 통해 서비스의 요구사항을 개발하고 서버를 구축했다면,
Chapter3 에서는 _**이 어플리케이션을 실제로 서비스를 할 때 발생하는 여러 문제들을 대응하는 방법**_을 다룬다.

대표적인 이슈로는 **'대용량 트래픽'** 과 **'동시성 이슈'** 가 있다.
내가 항해플러스 백엔드 과정을 시작하기로 결심했던 큰 이유중 하나가 바로 이것에 대한 내용이었다.
이 두가지 이슈는 백엔드 개발자가 회사에서 서비스를 개발하고, 개선해나가면서 무조건 마주하게 되는 문제다.
그리고 이 두가지 이슈를 서버에서 제대로 처리하지 못한다면 예상치 못한 큰 장애와 손실을 가져다 줄 수 있다.
그렇기에, 백엔드 엔지니어는 이 부분에 대해 단단하고 견고한 훈련이 되어있어야한다.

하지만, 내게 저 부분에 있어서 어떻게 구현하는지, 어떻게 대응하는지, 어떻게 해결하는지 물어본다면..
자신있게 대답할 수 있을까..?
글쎄.. 🤔

이제 벡엔드 개발자로 만 3년차가 되었지만, 부끄럽게도 자신있게 대답을 하지 못했다.
그렇기에 더더욱 이번 Chapter를 통해 이 부분에 있어서 단단하고 자신있는 기술적 탁월함을 성취하고 싶다.
`Redis` 와 `Kafka` 를 사용한 대용량 트래픽과 동시성 제어에 대한 학습에 큰 기대를 가지고 있다.

<br>

# 🍇 1. 5주차 항해 회고

### 🤮 힘들었다. 하지만 해냈다.

![](https://velog.velcdn.com/images/joshuara7235/post/0f716979-18ee-41f0-9ca6-eadc0bd2399e/image.png)

상대적으로 4주차에 비해서는 과제의 양이 많지 않았다.

#### Step9. 구현해야할 Filter 와 Interceptor
내 어플리케이션에서 구현한 Filter 와 Interceptor 는 로깅을 위한 Filter 와 토큰 검증을 위한 Interceptor 였다.

>- `LoggingFilter`
```kotlin
@Component
class LoggingFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestWrapper = ContentCachingRequestWrapper(request)
        val responseWrapper = ContentCachingResponseWrapper(response)
        logger.info(getRequestLog(requestWrapper))
        filterChain.doFilter(request, responseWrapper)
        logger.info(getResponseLog(responseWrapper))
    }
    private fun getRequestLog(request: ContentCachingRequestWrapper): String {
        val requestBody = String(request.contentAsByteArray)
        return """
            |=== REQUEST ===
            |Method: ${request.method}
            |URL: ${request.requestURL}
            |Headers: ${getHeadersAsString(request)}
            |Body: $requestBody
            |================
            """.trimMargin()
    }
    private fun getResponseLog(response: ContentCachingResponseWrapper): String {
        val responseBody = String(response.contentAsByteArray)
        return """
            |=== RESPONSE ===
            |Status: ${response.status}
            |Headers: ${getHeadersAsString(response)}
            |Body: $responseBody
            |=================
            """.trimMargin()
    }
    private fun getHeadersAsString(request: HttpServletRequest): String =
        request.headerNames.toList().joinToString(", ") {
            "$it: ${request.getHeader(it)}"
        }
    private fun getHeadersAsString(response: HttpServletResponse): String =
        response.headerNames.joinToString(", ") {
            "$it: ${response.getHeader(it)}"
        }
}
```
- 처음에는 Interceptor 로 구현하려고 했었지만, 멘토링을 받은 후에 Filter 로 구현하는 것으로 변경했다.
- Request 와 Response 에 대한 로깅이므로, Servlet 컨테이너에 의해 관리되는 Filter 가 더 적합하다고 판단했다.
- 더군다나, Request 와 Response 는 Spring 과는 무관한 녀석이므로, Spring Context 외부에서 동작하는 Filter 에서 처리하는 것이 맞다고 판단했다.


>- `TokenInterceptor`
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
            val token = request.getHeader(QUEUE_TOKEN_HEADER)
            if (token == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "$QUEUE_TOKEN_HEADER is missing")
                return false
            }
            if (!isValidToken(token)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid $QUEUE_TOKEN_HEADER")
                return false
            }
            request.setAttribute(VALIDATED_TOKEN, token)
        }
        return true
    }
    private fun isValidToken(token: String): Boolean = jwtUtil.validateToken(token)
}
```
- 이 컴포넌트에서는 발급한 Jwt 토큰의 유효성을 검토한다.
- `@TokenRequired` 라는 어노테이션이 붙어있는 메서드에서 동작하도록 설계했다.


#### Step10. 정상적으로 구동되는 서버 완성 및 통합 테스트 작성

>![](https://velog.velcdn.com/images/joshuara7235/post/6547af43-cccd-44cd-8939-b1dda5e66b86/image.png)
- 126개의 단위 테스트 및 통합테스트를 작성했고, 모두 성공시켰다.

#### Step10.5 Github Actions 를 활용한 Docker push CI 작성

>![](https://velog.velcdn.com/images/joshuara7235/post/24397db0-abc0-4e2a-8d60-b0eceb16821c/image.png)
- `Github Actions Workflow` 를 통해 `Main` 브랜치에 push 되면 Docker 파일이 빌드되고 컨테이너 이미지가 `github container registry` 에 저장되도록 했다.




### 🏅 지금까지 All Pass !

>![](https://velog.velcdn.com/images/joshuara7235/post/01a27ed4-877d-48bc-915f-2ce84fa9bd10/image.png)
- Chapter2가 마무리 되는 현재까지 과제를 모두 통과했다.
- 더군다나 이번 과제에서는 따봉👍🏻 을 받았다. 기분이 매우좋으다 크크..


<br>


# 🍐 2. 딱 절반이 지난 지금, 나는 얼마나 성장했나.

### 👨🏻‍💻 코딩을 할 때 더 시간이 소요된다.
많은 고민 없이 하던 방식대로 손이 먼저 나가고 어느새 하나의 메서드를 완성하고 또 다른 작업을 진행했었다.
하지만, 항해를 시작하고 코딩을 할 때 멈칫 멈칫 할 때가 많다.

>"이렇게 짜면 테스트하기 어려울 수 있을 것 같은데..?"
"패키지 구조는 이게 맞나..?"
"이 메서드는 이 클래스의 책임이 아닌 것 같은데 어떻게 분리시키지?"
"이 도메인은 여기까지 관여를 하면 안될 것 같은데... 설계를 다시 고민해 봐야하나?"

고민 없는 코드는 좋은 코드가 될 수 없다.
단순히 기능 구현의 고민이 아닌, 코드의 품질을 위한 고민을 하기 시작했다.
내 코드가 나만 알아보고 돌아만 가서 끝나는 악취나는 녀석이 되지 않게끔 내 최선을 다해 고민하려는 스탠스를 가지게 되었다.

그리고, 경험치가 쌓이면 이 시간도 단축되고 당연하듯 코드를 쓰게 되겠지..🤗

<br>

### 🐦 객체가 서비스에서 숨쉬며 살아있다는 말이 조금은 이해가 되기 시작했다.
저번 주 회고에서도 언급했지만, 조영호님의 `객체지향의 사실과 오해`에서 읽었던 대목들이 눈에 들어오기 시작했다.

단순히 `@Service` 어노테이션 붙이고, 클래스명에 `xxxxService` 로 만들고, 로직을 작성하고...
나는 그냥 클래스들을 딱딱하고 기계적인 무언가, 정적인 무언가로 생각하며 코딩을 했던 것 같다.

객체들에게 책임을 부여하니까, 이 녀석들이 그 책임을 가지고 일을 하더라.
그리고 그 책임을 가진 녀석들에게 적당한 이름을 붙여주니까 코드가 더 보기 좋아지더라.

실제로 회사에서 코드를 그렇게 변경해봤다.
`xxxIssuer`, `xxxMaker`, `xxxManager`
`@Component` 를 붙이고, 각자의 책임을 부여하고 `xxxService` 에서 조립을 하니까 훨씬 코드가 명확해졌다.
그리고, 무엇보다 내 어플리케이션에서 작은 친구들이 각자 부여된 하나의 역할을 충실하게 수행하는 것을 보니 기분이 좋았다.



### 🧪 테스트코드가 조금은 익숙해졌다.
아, 정말 개인적으로 큰 성장이라고 생각한다.
테스트코드 한 줄도 짜지 못하고, `JUnit` 을 듣기만 해봤던 내가 테스트코드가 익숙해졌다고 글을 쓰고 있다니..
아직은 그래도 낯선 부분도 있고, 더 좋은 테스트코드를 짜기 위한 고민과 노력이 필요하겠지만, 항해의 반이 지난 지금, 테스트코드가 조금은 익숙해졌다.

<br>


# 3. 글을 마치며
이제 반이 지났다.
언제 이렇게 시간이 갔나 싶다.
매주 회고글을 쓸 때마다 느끼는건데, 항상 이말을 쓰는 것 같다. '시간 참 빠르다.'

이제 시작하는 Chapter3 도 늘 그랬듯, 성실하게 최선을 다해 임해서 내가 성취하고 싶었던 배움의 기쁨을 누리고 싶다.

이번 주도 화이팅 💪🏻


### 지난 회고 보러가기
1주차 회고 - [테스트코드를 모르던 내게 찾아온 TDD](https://velog.io/@joshuara7235/%ED%85%8C%EC%8A%A4%ED%8A%B8%EC%BD%94%EB%93%9C%EB%A5%BC-%EB%AA%A8%EB%A5%B4%EB%8D%98-%EB%82%B4%EA%B2%8C-%EC%B0%BE%EC%95%84%EC%98%A8-TDD)
2주차 회고 - [코딩에 정답을 찾지말자. 고민을 통해 더 나아짐을 시작하자.](https://velog.io/@joshuara7235/%EC%BD%94%EB%94%A9%EC%97%90-%EC%A0%95%EB%8B%B5%EC%9D%84-%EC%B0%BE%EC%A7%80%EB%A7%90%EC%9E%90.-%EA%B3%A0%EB%AF%BC%EC%9D%84-%ED%86%B5%ED%95%B4-%EB%8D%94-%EB%82%98%EC%95%84%EC%A7%90%EC%9D%84-%EC%8B%9C%EC%9E%91%ED%95%98%EC%9E%90)
3주차 회고 - [좋은 코드를 위해서는 좋은 설계가 우선되어야 한다.](https://velog.io/@joshuara7235/%EC%A2%8B%EC%9D%80-%EC%BD%94%EB%93%9C%EB%A5%BC-%EC%9C%84%ED%95%B4%EC%84%9C%EB%8A%94-%EC%A2%8B%EC%9D%80-%EC%84%A4%EA%B3%84%EA%B0%80-%EC%9A%B0%EC%84%A0%EB%90%98%EC%96%B4%EC%95%BC-%ED%95%9C%EB%8B%A4)
4주차 회고 - [어플리케이션은 완벽할 수 없다. 다만 완벽을 지향할 뿐.](https://velog.io/@joshuara7235/%EC%96%B4%ED%94%8C%EB%A6%AC%EC%BC%80%EC%9D%B4%EC%85%98%EC%9D%80-%EC%99%84%EB%B2%BD%ED%95%A0-%EC%88%98-%EC%97%86%EB%8B%A4.-%EB%8B%A4%EB%A7%8C-%EC%99%84%EB%B2%BD%EC%9D%84-%EC%A7%80%ED%96%A5%ED%95%A0-%EB%BF%90)



