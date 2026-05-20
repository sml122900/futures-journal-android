# 이력서 소재 모음 (PAR 형식)

> PAR = Problem(문제) → Action(행동) → Result(결과)

---

## 템플릿

### Emergency Brake 오버레이 트리거 메시지 개선 (2026-05-21)
- **Problem**: 오버레이 화면에서 `symbol × sizeMultiplier배` 형식만 표시돼 사용자가 어떤 룰이 위반됐는지 파악 불가
- **Action**: `buildTriggerText()` 함수로 3단계 fallback 구현 — 새 서버 발동 메시지 → 타입별 한글 레이블 → 구 사이즈 배수. `EmergencyPayload`에 `triggerMessages` 필드 추가
- **Result**: 오버레이에서 위반된 룰을 구체적 문장으로 표시. 구 서버 호환 유지

### [프로젝트/기능명]

**Problem**
> 어떤 문제가 있었는가? 배경/맥락 포함.

**Action**
> 어떤 기술/방법으로 해결했는가? 내가 직접 한 것만.

**Result**
> 수치나 결과. "~% 개선", "~일 단축", "~명 사용" 등.

---

## 소재 목록

---

### 1. Android 오버레이 기반 실시간 매매 경고 시스템 (2026-05-19)

**Problem**
> 모바일 트레이더가 비트겟 앱을 사용 중 충동적으로 과도한 사이즈를 진입하는 문제가 반복됐다. 브라우저 확장(크롬)은 데스크톱에서만 동작하고, 모바일에서는 앱 전환 중 경고를 무시하기 쉬웠다.

**Action**
> Android `SYSTEM_ALERT_WINDOW` 권한과 `WindowManager.TYPE_APPLICATION_OVERLAY`를 활용해 어떤 앱이 포그라운드에 있더라도 즉시 풀스크린 경고를 강제 표시하는 시스템을 설계·구현했다. Firebase FCM high-priority push → `FcmService.onMessageReceived` → `OverlayService` 시작 → `WindowManager.addView`로 오버레이 삽입하는 end-to-end 파이프라인을 단독 개발했다.

**Result**
> 서버가 위험 매매를 감지한 뒤 약 1~3초 내에 비트겟 화면 위로 경고 오버레이가 표시된다. 사용자가 앱을 강제 종료하지 않는 한 회피 불가.

---

### 2. 강제 타이핑 검증으로 충동 행동 차단 UX 설계 (2026-05-19)

**Problem**
> 단순 "확인" 버튼은 사용자가 경고를 읽지 않고 바로 닫는 문제가 있었다. 경고가 있어도 무시하고 진행하는 패턴을 차단할 방법이 필요했다.

**Action**
> 경고를 닫으려면 "원칙을 지켜야 살아남는다" 같은 문장을 정확히 타이핑해야만 버튼이 활성화되도록 구현했다. 카운트다운(60초) 동안은 입력 자체를 비활성화해 즉각적인 닫기를 원천 차단했다. `TextWatcher`로 실시간 문자 일치 여부를 판별하고, 타이핑에 소요된 시간(`durationSeconds`)을 서버에 기록해 행동 통계에 활용했다.

**Result**
> 인지적 마찰(cognitive friction) 기법 적용 — 사용자가 경고 내용을 의식적으로 인지하지 않으면 진행 불가능한 구조. 취소/진행/소요 시간 데이터가 서버에 누적되어 향후 효과 분석 가능.

---

### 3. FCM + Firebase Admin SDK 기반 서버-모바일 긴급 알림 파이프라인 (2026-05-19)

**Problem**
> 기존 Emergency Brake는 텔레그램 봇 알림만 지원했다. 텔레그램을 설치하지 않거나 알림을 꺼둔 사용자에게는 경고가 전달되지 않았고, 모바일 앱 위에 직접 개입하는 수단이 없었다.

**Action**
> Next.js 서버에 Firebase Admin SDK(`firebase-admin`)를 도입해 `sendEmergencyPush` 함수를 구현했다. Prisma에 `DeviceRegistration` 모델을 추가하고, 기존 `emergency-scheduler.ts`의 텔레그램 발송 로직 뒤에 FCM 발송을 병렬로 추가했다. Android 앱은 `FcmService`에서 data message를 수신해 `EmergencyPayload`를 파싱 후 `OverlayService`를 시작한다.

**Result**
> 텔레그램 없이도 Android 기기에 직접 개입하는 경로 확보. Level 2 이상 위험 시 텔레그램 + FCM 동시 발송으로 이중 경보 체계 구성.

---

### 4. Android 앱 ↔ Next.js 서버 인증 체계 독자 설계 (2026-05-19)

**Problem**
> 기존 서버는 NextAuth 세션 쿠키 기반 인증만 지원했다. Android 앱은 브라우저 세션이 없으므로 기존 인증 체계를 그대로 사용할 수 없었다.

**Action**
> Prisma `User` 모델에 `androidAuthToken` 필드를 추가하고, 웹에서 `GET /api/users/android-token`으로 랜덤 32바이트 토큰을 발급받아 Android 앱에 입력하는 흐름을 설계했다. Android 앱은 Retrofit 헤더에 `Authorization: Bearer <token>`을 삽입하고, 각 API 라우트에서 해당 토큰으로 유저를 조회해 인증한다.

**Result**
> NextAuth에 의존하지 않는 독립적인 모바일 인증 체계 구축. 토큰 재발급(`POST /api/users/android-token`) 기능으로 보안 사고 시 즉각 무효화 가능.

---

### 6. FCM 백그라운드 수신 버그 진단 및 수정 (2026-05-19)

**Problem**
> Galaxy S26 실기기 테스트에서 FCM 메시지는 수신되나 OverlayService가 시작되지 않았다. 앱이 백그라운드 상태일 때 경고 오버레이가 전혀 뜨지 않아 핵심 기능이 무력화된 상태였다.

**Action**
> `adb logcat`으로 디바이스 로그를 분석해 `"Notification Channel requested (emergency) has not been created"` 메시지를 포착했다. Firebase 공식 동작 명세를 확인한 결과, 서버 측 `fcm-sender.ts`에서 `data`와 `android.notification`을 동시에 발송하면 앱 백그라운드 시 시스템이 `onMessageReceived`를 건너뛰고 직접 알림 트레이에 표시한다는 것을 확인했다. `android.notification` 블록을 제거해 순수 data message로 변경했다.

**Result**
> 앱 포그라운드/백그라운드/화면 꺼짐 모든 상태에서 FCM 수신 즉시 `onMessageReceived` → `OverlayService` → 풀스크린 오버레이 파이프라인이 정상 작동. 실기기 E2E 테스트 통과.

---

### 7. 사용자 행동 개입 UX 고도화 — 역동적 카운트다운 + 개인화 멘트 (2026-05-19)

**Problem**
> 고정된 멘트와 단조로운 카운트다운으로는 사용자가 경고를 형식적으로 통과하는 문제가 있었다. 카운트다운 중에도 버튼이 보여 즉각 닫으려는 충동을 유발했고, 완료 후 흐름도 "진행/취소" 이분법이라 심리적 무게감이 부족했다.

**Action**
> 카운트다운 단계에서 버튼을 완전히 제거하고, 120sp 대형 숫자에 매 초 1.3배 펄스 애니메이션과 잔여 시간에 따른 노랑→주황→빨강 색상 전환을 적용했다. 타이핑 완료 후에는 "본인과의 약속을 지키십시오." 문구와 단일 "확인" 버튼만 제공해 심리적 마찰을 극대화했다. `OverlayPrefs`(SharedPreferences)로 멘트 13종과 카운트다운 시간을 기기 로컬에 저장하고, SettingsActivity에서 멘트 추가/삭제/선택이 가능하도록 설계했다.

**Result**
> 카운트다운 → 타이핑 → 확인의 3단계 강제 의식(ritual) 구조 완성. 멘트 개인화로 "내가 선택한 다짐"이라는 심리적 소유감 부여. 기본 멘트 삭제 시 `hidden_defaults` Set으로 관리해 앱 재설치 시 복원 가능한 안전한 삭제 구현.

---

### 5. EncryptedSharedPreferences로 모바일 인증 토큰 보안 저장 (2026-05-19)

**Problem**
> Android 기기 분실 또는 루팅 시 일반 `SharedPreferences`에 저장된 인증 토큰이 평문으로 노출될 수 있었다.

**Action**
> Android Security-Crypto 라이브러리의 `EncryptedSharedPreferences`를 적용했다. `MasterKey.KeyScheme.AES256_GCM`으로 마스터 키를 생성하고, 키는 `AES256_SIV`, 값은 `AES256_GCM`으로 각각 암호화해 저장하도록 `TokenStore` 싱글톤을 구현했다.

**Result**
> 기기 저장소에 인증 토큰이 암호화된 형태로만 존재. Android Keystore 시스템과 연동되어 앱 외부에서 복호화 불가.
