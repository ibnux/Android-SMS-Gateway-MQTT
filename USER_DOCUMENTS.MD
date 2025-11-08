# Android SMS Gateway MQTT - 코드 분석 문서

## 프로젝트 개요

이 리포지토리는 Android 기기를 SMS 게이트웨이로 전환하여 MQTT를 통해 원격으로 SMS를 송수신할 수 있는 애플리케이션입니다.

### 주요 기능

- MQTT를 통한 SMS 송신
- SMS 수신 후 웹서버로 전달
- SMS 전송/전달 상태 알림
- USSD 코드 실행 지원
- 듀얼 SIM 카드 지원
- SMS 전송 실패 시 최대 3회 재시도

### 기술 스택

- Java (Android)
- MQTT 프로토콜 (Eclipse Paho)
- Android API Level 24-33 (Android 7.0 ~ 13)
- Accessibility Service (USSD용)

---

## 핵심 컴포넌트 구조

### 1. MainActivity.java

**위치**: `app/src/main/java/com/ibnux/smsgatewaymqtt/MainActivity.java`

메인 UI 액티비티로 다음 기능을 제공합니다:

#### 주요 기능

- **권한 요청**: Dexter 라이브러리를 사용하여 SMS, 전화, 위치, 네트워크 등 필수 권한 요청
- **게이트웨이 ON/OFF 토글**: 상단 메뉴의 스위치로 서비스 제어
- **설정 관리**:
  - Device ID 변경 (MQTT Topic ID로 사용)
  - MQTT 서버 주소 설정
  - MQTT 인증 정보 (사용자명/비밀번호)
  - 웹훅 URL 설정 (수신 SMS POST 전송용)
- **로그 표시**: 파일 기반 로그 시스템으로 최근 활동 표시

#### SharedPreferences 설정

```
저장소: "pref"
- deviceID: MQTT Topic으로 사용
- mqtt_server: 기본값 "tcp://broker.hivemq.com:1883"
- mqtt_user, mqtt_pass: MQTT 인증
- urlPost: SMS 수신 시 POST할 웹서버 URL
- gateway_on: 게이트웨이 활성화 상태
```

### 2. BackgroundService.java

**위치**: `app/src/main/java/com/ibnux/smsgatewaymqtt/layanan/BackgroundService.java`

포그라운드 서비스로 실행되며 핵심 비즈니스 로직을 처리합니다.

#### MQTT 연결 관리

- 자동 재연결 기능
- 메시지 버퍼링 (연결 끊김 시 최대 100개)
- Device ID를 MQTT Topic으로 구독

#### MQTT 메시지 형식

```json
{
    "to": "전화번호",
    "sim": "1",
    "message": "메시지 내용"
}
```

#### 메시지 처리 로직

1. JSON 파싱
2. 중복 방지: MD5 해시로 15초 내 중복 메시지 차단
3. SMS 또는 USSD 전송

#### SMS 전송 결과 처리

- **sentReceiver**: 전송 성공/실패 감지, 실패 시 10초 후 재시도 (최대 3회)
- **deliveredReceiver**: 수신자 전달 확인
- 각 이벤트를 웹서버로 POST

#### WiFi Lock

- 구독 성공 시 WiFi 잠금으로 안정적인 연결 유지

### 3. SmsListener.java

**위치**: `app/src/main/java/com/ibnux/smsgatewaymqtt/layanan/SmsListener.java`

BroadcastReceiver로 SMS 수신을 감지하고 처리합니다.

#### 동작 방식

1. SMS 수신 감지
2. 발신자 번호, 메시지 내용, 타임스탬프 추출
3. 로그 기록
4. urlPost로 POST 전송

#### POST 파라미터

```
- number: 전화번호
- message: 메시지 내용
- type: received/sent/delivered/ussd
- timestamp: 타임스탬프
```

### 4. SimUtil.java

**위치**: `app/src/main/java/com/ibnux/smsgatewaymqtt/Utils/SimUtil.java`

멀티 SIM 카드 SMS 전송 및 USSD 처리를 담당하는 핵심 유틸리티입니다.

#### SMS 전송 메커니즘

- Reflection을 사용하여 내부 `ISms` 서비스 접근
- SIM 0: "isms", SIM 1: "isms2" 서비스명 사용
- Fallback: SubscriptionManager API 사용
- 긴 메시지: `sendMultipartTextSMS()` 자동 분할 전송

#### USSD 큐 시스템

```
queueUssd(code, simNumber) → ussdDataList에 추가
    ↓
runUssd() → 순차 실행 (큐 방식)
    ↓
checkTimeout() → 60초 타임아웃 감지
```

#### 멀티 SIM 지원

- 다양한 제조사의 Intent Extra 키를 모두 시도
- TelecomManager API (Android M+) 사용
- 15가지 다른 SIM 슬롯 키 지원

### 5. UssdService.java

**위치**: `app/src/main/java/com/ibnux/smsgatewaymqtt/layanan/UssdService.java`

AccessibilityService를 활용한 USSD 응답 캡처 서비스입니다.

#### 동작 방식

1. AlertDialog의 텍스트 내용 캡처
2. GLOBAL_ACTION_BACK으로 자동 닫기
3. 결과를 웹서버로 POST
4. 다음 USSD 큐 실행

#### 제약사항

- 일부 기기에서는 다이얼로그가 자동으로 닫히지 않음
- Accessibility 권한 필요

### 6. Fungsi.java

**위치**: `app/src/main/java/com/ibnux/smsgatewaymqtt/Utils/Fungsi.java`

공통 유틸리티 함수 모음입니다.

#### 주요 기능

- **로그 시스템**: 파일 기반, 최대 110줄 유지 (오래된 로그 자동 삭제)
- **MD5 해싱**: 중복 메시지 감지용
- **캐시 정리**: 15초 이상 된 파일 자동 삭제
- **SIM 정보 조회**: `content://telephony/siminfo/` 접근
- **Accessibility 상태 확인**: USSD 서비스 활성화 여부

---

## 데이터 흐름

### SMS 송신 플로우

```
MQTT 메시지 수신
  ↓
BackgroundService.messageArrived()
  ↓
JSON 파싱 {"to", "sim", "message"}
  ↓
중복 체크 (MD5 + 15초 캐시)
  ↓
SimUtil.sendSMS() / sendMultipartTextSMS()
  ↓
Reflection으로 ISms 서비스 호출
  ↓
sentReceiver: POST to urlPost (type=sent)
  ↓
deliveredReceiver: POST to urlPost (type=delivered)
```

### SMS 수신 플로우

```
SMS 수신
  ↓
SmsListener.onReceive()
  ↓
메시지 정보 추출
  ↓
로그 기록
  ↓
sendPOST(urlPost, number, message, "received", timestamp)
```

### USSD 플로우

```
MQTT 메시지 수신 (to: "*123#")
  ↓
SimUtil.queueUssd()
  ↓
runUssd() → Intent.ACTION_CALL
  ↓
UssdService.onAccessibilityEvent()
  ↓
텍스트 캡처 + 다이얼로그 닫기
  ↓
sendPOST(urlPost, ussd, text, "ussd", timestamp)
```

---

## 보안 및 권한

### 필요한 권한 (AndroidManifest.xml)

- `SEND_SMS`, `RECEIVE_SMS`: SMS 송수신
- `CALL_PHONE`: USSD 실행
- `READ_PHONE_STATE`: SIM 정보 조회
- `RECEIVE_BOOT_COMPLETED`: 부팅 시 자동 시작
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: 백그라운드 실행 보장
- `BIND_ACCESSIBILITY_SERVICE`: USSD 응답 캡처

### 잠재적 보안 이슈

1. **HTTP 허용**: `android:usesCleartextTraffic="true"` - HTTPS 사용 권장
2. **Reflection 사용**: 내부 API 접근으로 Android 버전별 호환성 이슈 가능
3. **인증 부재**: MQTT 메시지에 인증 메커니즘 없음 (누구나 Device ID만 알면 SMS 전송 가능)
4. **SharedPreferences**: 민감 정보(MQTT 비밀번호)가 평문 저장

---

## 아키텍처 특징

### 장점

- ✅ MQTT 기반으로 저대역폭에서 효율적
- ✅ 중복 메시지 방지 로직 (MD5 + 타임스탬프)
- ✅ SMS 전송 재시도 메커니즘 (최대 3회)
- ✅ 포그라운드 서비스로 안정적인 백그라운드 실행
- ✅ WiFi Lock으로 연결 안정성 확보
- ✅ 멀티 SIM 지원 (다양한 제조사 호환)

### 개선 가능 영역

- ⚠️ AsyncTask 사용 (Deprecated) → Coroutine/RxJava 전환 권장
- ⚠️ 하드코딩된 값들 (타임아웃, 재시도 횟수 등)
- ⚠️ 로그 파일 크기 제한 없음 (110줄만 체크)
- ⚠️ USSD 응답 처리의 `if (true)` 조건 (UssdService.java:93)
- ⚠️ 예외 처리가 대부분 printStackTrace()만 수행

---

## 의존성 라이브러리

**파일**: `app/build.gradle`

```gradle
- Eclipse Paho MQTT v3: 1.1.0 (MQTT 클라이언트)
- Dexter: 6.2.3 (권한 요청 라이브러리)
- Requery: 1.6.0 (사용되지 않는 것으로 보임)
- AndroidX Work: 2.8.1 (사용되지 않는 것으로 보임)
- SwipeRefreshLayout: 1.1.0
```

---

## 설정 가이드

### 1. MQTT 브로커 설정

- 기본값: HiveMQ 공개 브로커 (`tcp://broker.hivemq.com:1883`)
- 사용자 정의 브로커 사용 가능

### 2. Device ID 설정

- UUID 또는 전화번호 사용
- MQTT Topic으로 활용

### 3. Webhook URL 설정

- SMS 수신/전송 상태를 받을 서버 엔드포인트 지정
- POST 방식으로 데이터 전송

### 4. USSD 설정

- Accessibility 설정에서 서비스 활성화 필요
- 일부 기기에서는 제대로 작동하지 않을 수 있음

### 5. 배터리 최적화 제외

- 안정적인 백그라운드 실행을 위해 배터리 최적화 제외 필요

---

## PHP 백엔드 예제

**위치**: `php-gateway` 폴더

서버측 예제 코드가 포함되어 있어 SMS 수신/전송 상태를 처리할 수 있습니다.

---

## 사용 사례

이 프로젝트는 다음과 같은 용도로 활용 가능합니다:

- IoT 환경에서 SMS 원격 제어
- 2FA SMS 자동 처리
- SMS 기반 알림 시스템
- 원격 모니터링 및 제어 시스템
- 통신사 USSD 명령 자동화

---

## 빌드 방법

1. Android Studio에서 프로젝트 열기
2. 처음 빌드 시 MyObjectBox 에러 발생 가능 (정상)
3. 한 번 더 빌드하면 ObjectBox 코드 자동 생성
4. 상세 내용: [ObjectBox 공식 문서](https://docs.objectbox.io/getting-started#generate-objectbox-code)

---

## 라이선스

Apache License 2.0

### 허용사항
- ✓ 상업적 사용
- ✓ 배포
- ✓ 수정
- ✓ 특허 사용
- ✓ 개인적 사용

### 조건
- 라이선스 및 저작권 고지
- 변경사항 명시

### 제한사항
- 책임 없음
- 상표권 사용 불가
- 보증 없음

---

## 작성자

Created by Ibnu Maksum (2020-2023)

- GitHub: https://github.com/ibnux/Android-SMS-Gateway-MQTT
- Firebase Push 버전: https://github.com/ibnux/Android-SMS-Gateway/
