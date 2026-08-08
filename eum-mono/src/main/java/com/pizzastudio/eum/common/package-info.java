/**
 * 모든 모듈이 쓰는 공용 코드입니다. 열린 모듈로 선언해 하위 패키지까지 참조를 허용합니다(13.3).
 *
 * <p>공용에 무엇을 둘지는 정해 두어야 합니다. 업무 규칙이 여기로 들어오면 모듈을 나눈 뜻이
 * 없어집니다. 여기에는 기반 엔티티·공통 예외·페이지 요청 형식·설정만 둡니다.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.pizzastudio.eum.common;
