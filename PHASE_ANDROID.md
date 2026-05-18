# Phase Android — Emergency Brake 안드로이드 앱

> Futures Journal 안드로이드 네이티브 앱. 비트겟 모바일 앱 위에 강제 오버레이로 경고 표시. Claude Code 자동 진행용 계획서.

---

## 중요 사전 안내

**이 작업은 별도 프로젝트입니다.**

- 기존 `futures-journal` 프로젝트와 분리
- 새 폴더 `futures-journal-android` 생성
- Kotlin + Android SDK 사용
- 기존 메인 서버 API를 호출하는 클라이언트 역할

**필수 도구**

- Android Studio (최신 버전)
- Java JDK 17 이상
- Android SDK API 34 이상

**미설치 시 작업 중단 필요.** 설치되어 있는지 먼저 확인 필요.

---

## 자동 진행 규칙

1. 아래 단계를 순서대로 진행
2. 각 단계 끝나면 빌드 확인 (`./gradlew build`)
3. 각 단계 완료 시 변경/생성 파일 목록을 1~2줄로 요약
4. 단계 완료 시 진행 상태 표 업데이트

## 멈추고 사용자에게 물어봐야 하는 경우

- Android Studio 미설치
- Firebase 프로젝트 생성 필요 (FCM용)
- 구글 플레이 개발자 계정 등록 필요 ($25 결제)
- 권한 설계가 모호한 경우

---

## 기능 개요

비트겟 모바일 앱에서 큰 사이즈 매매가 발생하면, 우리 서버가 감지 후 FCM 푸시를 보내고, 안드로이드 앱이 "다른 앱 위에 표시" 권한으로 비트겟 화면 위에 강제 경고를 띄운다. 사용자는 강제 타이핑으로만 경고를 닫을 수 있다.

### 타깃 시나리오

```
[비트겟 모바일 앱에서 평소 4배 사이즈 진입 → 진입 완료]
       ↓ (3초 후 우리 서버가 자동갱신으로 감지)
[메인 서버: emergency-check 로직 실행]
       ↓ (위험 감지)
[FCM 푸시 전송 (high priority)]
       ↓ (1~2초)
[안드로이드 앱: FcmService.onMessageReceived]
       ↓
[OverlayService 시작 → "다른 앱 위" 권한으로 풀스크린 표시]
       ↓
[빨간 경고 + 강제 타이핑 + 60초 카운트다운]
       ↓
[사용자가 비트겟으로 가서 청산하거나, 타이핑 통과 후 닫기]
```

### 크롬 확장과의 차이

| 항목 | 크롬 확장 | 안드로이드 앱 |
|------|----------|-------------|
| 개입 시점 | 진입 직전 (100% 차단 가능) | 진입 직후 3~5초 (빠른 청산 유도) |
| 기술 | DOM 인터셉트 | SYSTEM_ALERT_WINDOW 권한 |
| 지원 OS | Windows/Mac | Android만 |
| 효과 | 진입 자체 방지 | 손실 최소화 |

---

## 진행 상태

| 단계 | 작업 | 상태 |
|------|------|------|
| A1 | 환경 확인 + Android Studio 프로젝트 생성 | ✅ 완료 |
| A2 | manifest 권한 + 기본 구조 | ✅ 완료 |
| A3 | Firebase Cloud Messaging 설정 | ✅ 완료 |
| A4 | OverlayService (다른 앱 위 표시) | ✅ 완료 |
| A5 | 경고 UI (Level 1, 2, 3) | ✅ 완료 |
| A6 | 강제 타이핑 + 카운트다운 검증 | ✅ 완료 |
| A7 | 로그인 + 토큰 관리 | ✅ 완료 |
| A8 | 권한 안내 온보딩 플로우 | ✅ 완료 |
| A9 | 메인 서버 통합 (디바이스 등록, FCM 발송) | ✅ 완료 |
| A10 | 빌드 + 구글 플레이 등록 준비 | ✅ 완료 |

---

## A1. 환경 확인 + 프로젝트 생성

### 1-1. 환경 점검

```bash
# Android Studio 설치 확인
# JDK 확인
java -version  # 17 이상

# Android SDK 확인 (Android Studio에서 SDK Manager로)
```

⚠️ 미설치 시 멈추고 사용자에게 알릴 것:
- Android Studio 설치 안내 (https://developer.android.com/studio)
- 처음 설치 시 SDK 다운로드 약 30분 소요

### 1-2. 프로젝트 생성

```
Android Studio → New Project
- Template: Empty Activity
- Name: Futures Journal
- Package: com.futuresjournal.app
- Language: Kotlin
- Minimum SDK: API 26 (Android 8.0)
  → SYSTEM_ALERT_WINDOW가 26부터 권한 요청 방식 변경
- Build configuration: Kotlin DSL (.kts)
```

### 1-3. 폴더 구조

```
futures-journal-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/futuresjournal/app/
│   │   │   ├── MainActivity.kt              메인 화면 (설정)
│   │   │   ├── SplashActivity.kt            앱 시작 시 표시
│   │   │   ├── LoginActivity.kt             토큰 입력
│   │   │   ├── OnboardingActivity.kt        권한 안내
│   │   │   ├── SettingsActivity.kt          설정
│   │   │   ├── service/
│   │   │   │   ├── FcmService.kt            푸시 수신
│   │   │   │   └── OverlayService.kt        오버레이 표시 서비스
│   │   │   ├── overlay/
│   │   │   │   ├── OverlayWindow.kt         WindowManager 관리
│   │   │   │   ├── WarningView.kt           경고 UI
│   │   │   │   └── TypeChallenge.kt         타이핑 검증
│   │   │   ├── api/
│   │   │   │   ├── ApiClient.kt             서버 API 호출
│   │   │   │   └── models/                  데이터 클래스
│   │   │   ├── auth/
│   │   │   │   └── TokenStore.kt            EncryptedSharedPreferences
│   │   │   └── util/
│   │   │       ├── PermissionHelper.kt
│   │   │       └── Logger.kt
│   │   ├── res/
│   │   │   ├── layout/                      XML 레이아웃
│   │   │   ├── drawable/                    아이콘
│   │   │   ├── values/
│   │   │   │   ├── strings.xml              한국어 문자열
│   │   │   │   ├── colors.xml
│   │   │   │   └── themes.xml
│   │   │   └── values-night/                다크모드
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── google-services.json                 FCM 설정 (Firebase에서 받음)
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

### 1-4. 의존성 (build.gradle.kts)

```kotlin
dependencies {
    // AndroidX 기본
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Coroutines (비동기)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Firebase Cloud Messaging
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // 네트워크
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

    // 암호화된 SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // 테스트
    testImplementation("junit:junit:4.13.2")
}
```

---

## A2. Manifest 권한 + 기본 구조

### 2-1. AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- 필수 권한 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

    <application
        android:name=".FuturesJournalApp"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.FuturesJournal"
        android:allowBackup="false"
        tools:targetApi="34">

        <!-- 메인 액티비티 -->
        <activity
            android:name=".SplashActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity android:name=".MainActivity" android:exported="false" />
        <activity android:name=".LoginActivity" android:exported="false" />
        <activity android:name=".OnboardingActivity" android:exported="false" />
        <activity android:name=".SettingsActivity" android:exported="false" />

        <!-- FCM 서비스 -->
        <service
            android:name=".service.FcmService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>

        <!-- 오버레이 서비스 -->
        <service
            android:name=".service.OverlayService"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="다른 앱 위 경고 표시" />
        </service>

    </application>
</manifest>
```

### 2-2. 권한 요약

| 권한 | 용도 | 필수 |
|------|------|------|
| INTERNET | 서버 API 호출 | 필수 |
| SYSTEM_ALERT_WINDOW | 다른 앱 위 표시 | 핵심 |
| POST_NOTIFICATIONS | 푸시 알림 (Android 13+) | 필수 |
| VIBRATE | 진동 | 권장 |
| WAKE_LOCK | 화면 깨우기 | 권장 |
| USE_FULL_SCREEN_INTENT | 잠금화면 풀스크린 | 권장 |
| FOREGROUND_SERVICE | 백그라운드 서비스 | 필수 |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | 배터리 최적화 예외 | 권장 |

---

## A3. Firebase Cloud Messaging 설정

### 3-1. Firebase 프로젝트 생성

⚠️ 멈추고 사용자에게 안내:

```
1. https://console.firebase.google.com 접속
2. "프로젝트 만들기" 클릭
3. 프로젝트 이름: "Futures Journal"
4. Google Analytics 활성화 (선택)
5. 프로젝트 생성 완료

6. Android 앱 추가:
   - 패키지 이름: com.futuresjournal.app
   - 앱 닉네임: Futures Journal
   - SHA-1 인증서 (디버그용): keytool 명령으로 생성
     keytool -list -v -keystore ~/.android/debug.keystore
     비밀번호: android

7. google-services.json 다운로드
8. app/ 폴더에 저장
```

### 3-2. 빌드 설정 추가

`build.gradle.kts` (project level):
```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

`app/build.gradle.kts`:
```kotlin
plugins {
    id("com.google.gms.google-services")
}
```

### 3-3. FcmService 구현

```kotlin
// service/FcmService.kt
class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // 새 토큰을 서버에 등록
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.registerDevice(token)
            } catch (e: Exception) {
                Logger.error("token register failed", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val type = data["type"]

        when (type) {
            "emergency" -> {
                val level = data["level"]?.toIntOrNull() ?: 0
                val sessionId = data["sessionId"] ?: return
                val payload = EmergencyPayload(
                    sessionId = sessionId,
                    level = level,
                    symbol = data["symbol"] ?: "",
                    side = data["side"] ?: "",
                    size = data["size"]?.toDoubleOrNull() ?: 0.0,
                    sizeMultiplier = data["sizeMultiplier"]?.toDoubleOrNull() ?: 0.0,
                    triggers = data["triggers"]?.split(",") ?: emptyList(),
                    forceSentence = data["forceSentence"] ?: "원칙을 지켜야 살아남는다",
                    countdownSeconds = data["countdown"]?.toIntOrNull() ?: 60
                )

                // 오버레이 서비스 시작
                val intent = Intent(this, OverlayService::class.java).apply {
                    putExtra("payload", payload)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }
    }
}
```

---

## A4. OverlayService (다른 앱 위 표시)

### 4-1. 핵심 구현

```kotlin
// service/OverlayService.kt
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var countdownJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Foreground 알림 시작 (필수)
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val payload = intent?.getParcelableExtra<EmergencyPayload>("payload")
        if (payload == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // 권한 체크
        if (!Settings.canDrawOverlays(this)) {
            Logger.error("SYSTEM_ALERT_WINDOW permission denied")
            // 권한 없으면 일반 풀스크린 알림으로 fallback
            showFullScreenNotification(payload)
            stopSelf()
            return START_NOT_STICKY
        }

        showOverlay(payload)
        return START_NOT_STICKY
    }

    private fun showOverlay(payload: EmergencyPayload) {
        if (overlayView != null) return  // 중복 방지

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv() and
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED.inv() and
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON.inv(),
            PixelFormat.TRANSLUCENT
        )

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_warning, null)

        setupWarningContent(overlayView!!, payload)
        setupTypeChallenge(overlayView!!, payload)
        startCountdown(overlayView!!, payload.countdownSeconds)

        // 강력 효과 (Level 3)
        if (payload.level == 3) {
            vibratePattern()
            playWarningSound()
        }

        windowManager.addView(overlayView, params)
    }

    private fun setupTypeChallenge(view: View, payload: EmergencyPayload) {
        val targetSentence = payload.forceSentence
        val input = view.findViewById<EditText>(R.id.typing_input)
        val proceedBtn = view.findViewById<Button>(R.id.btn_proceed)
        val feedback = view.findViewById<TextView>(R.id.typing_feedback)

        input.isEnabled = false  // 카운트다운 동안 비활성

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val typed = s?.toString() ?: ""
                val match = typed == targetSentence
                proceedBtn.isEnabled = match
                feedback.text = if (match) "✓ 정확히 타이핑하셨습니다"
                                else if (typed.isNotEmpty()) "✗ 다시 확인하세요"
                                else ""
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        view.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                ApiClient.cancelEmergency(payload.sessionId)
            }
            removeOverlay()
            stopSelf()
        }

        proceedBtn.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                ApiClient.proceedEmergency(payload.sessionId, input.text.toString())
            }
            removeOverlay()
            stopSelf()
        }

        view.findViewById<Button>(R.id.btn_open_bitget).setOnClickListener {
            // 비트겟 앱으로 이동
            val intent = packageManager.getLaunchIntentForPackage("com.bitget.exchange")
            if (intent != null) startActivity(intent)
        }
    }

    private fun startCountdown(view: View, seconds: Int) {
        val display = view.findViewById<TextView>(R.id.countdown)
        val input = view.findViewById<EditText>(R.id.typing_input)
        var remaining = seconds

        countdownJob = CoroutineScope(Dispatchers.Main).launch {
            while (remaining > 0) {
                val m = remaining / 60
                val s = remaining % 60
                display.text = String.format("%02d:%02d", m, s)
                delay(1000)
                remaining--
            }
            display.text = "⌨️ 타이핑 가능"
            input.isEnabled = true
            input.requestFocus()
        }
    }

    private fun removeOverlay() {
        overlayView?.let { windowManager.removeView(it) }
        overlayView = null
        countdownJob?.cancel()
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

### 4-2. Foreground 서비스 알림

```kotlin
private fun buildForegroundNotification(): Notification {
    val channelId = "emergency_overlay"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Emergency Brake 활성",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    return NotificationCompat.Builder(this, channelId)
        .setSmallIcon(R.drawable.ic_shield)
        .setContentTitle("Emergency Brake 활성")
        .setContentText("매매 보호 모드 작동 중")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
}
```

---

## A5. 경고 UI (Level 1, 2, 3)

### 5-1. overlay_warning.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#D9000000"
    android:clickable="true"
    android:focusable="true">

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:padding="24dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:background="@drawable/warning_card_bg"
            android:padding="24dp">

            <!-- 헤더 -->
            <TextView
                android:id="@+id/header_title"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="🚨 매매 중단 권고 🚨"
                android:textSize="28sp"
                android:textStyle="bold"
                android:textColor="@color/warning_red"
                android:gravity="center"
                android:layout_marginBottom="24dp" />

            <!-- 트리거 목록 -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="다음 조건이 동시 감지됐습니다:"
                android:textStyle="bold"
                android:textColor="@color/text_primary"
                android:layout_marginBottom="8dp" />

            <LinearLayout
                android:id="@+id/trigger_list"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:layout_marginBottom="16dp" />

            <!-- 컨텍스트 (사이즈, 평균 등) -->
            <TextView
                android:id="@+id/context_text"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textColor="@color/text_secondary"
                android:layout_marginBottom="16dp" />

            <!-- 타이핑 섹션 -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="계속하려면 정확히 타이핑하세요:"
                android:textStyle="bold"
                android:layout_marginBottom="8dp" />

            <TextView
                android:id="@+id/target_sentence"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textSize="20sp"
                android:textStyle="bold"
                android:gravity="center"
                android:padding="12dp"
                android:background="@drawable/sentence_bg"
                android:layout_marginBottom="8dp" />

            <EditText
                android:id="@+id/typing_input"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="여기에 타이핑..."
                android:enabled="false"
                android:inputType="text" />

            <TextView
                android:id="@+id/typing_feedback"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textSize="14sp"
                android:layout_marginBottom="16dp" />

            <!-- 카운트다운 -->
            <TextView
                android:id="@+id/countdown"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="01:00"
                android:textSize="32sp"
                android:textStyle="bold"
                android:textColor="@color/warning_red"
                android:gravity="center"
                android:layout_marginBottom="16dp" />

            <!-- 액션 버튼들 -->
            <Button
                android:id="@+id/btn_open_bitget"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="🛑 비트겟으로 가서 청산하기"
                android:backgroundTint="@color/warning_red"
                android:textColor="@android:color/white"
                android:layout_marginBottom="8dp" />

            <Button
                android:id="@+id/btn_cancel"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="취소"
                android:backgroundTint="@color/button_secondary"
                android:layout_marginBottom="8dp" />

            <Button
                android:id="@+id/btn_proceed"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="그래도 진행"
                android:enabled="false"
                android:backgroundTint="@color/button_disabled" />

        </LinearLayout>
    </ScrollView>
</FrameLayout>
```

### 5-2. Level별 디자인

```kotlin
private fun applyLevelStyle(view: View, level: Int) {
    val card = view.findViewById<LinearLayout>(R.id.warning_card)
    when (level) {
        1 -> card.setBackgroundResource(R.drawable.warning_yellow)
        2 -> card.setBackgroundResource(R.drawable.warning_orange)
        3 -> card.setBackgroundResource(R.drawable.warning_red)
    }
}
```

---

## A6. 강제 타이핑 + 카운트다운 검증

이미 A4의 setupTypeChallenge에서 처리. 추가 강화 사항:

### 6-1. 뒤로가기 버튼 차단 (Level 3)

```kotlin
// Level 3에서는 뒤로가기 키 무시
private fun blockBackButton() {
    overlayView?.setOnKeyListener { _, keyCode, _ ->
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 무시
            true
        } else false
    }
}
```

오버레이 뷰는 system overlay이므로 뒤로가기 키가 직접 도달 안 함. 다만 사용자가 비트겟 앱으로 가서 우리 알림을 무시하는 건 막을 수 없음 (정상).

### 6-2. 통계 기록

타이핑 통과 또는 취소 시 서버에 기록:

```kotlin
// onProceed
ApiClient.proceedEmergency(sessionId, typedText, durationSeconds = ...)

// onCancel
ApiClient.cancelEmergency(sessionId, reason = "user_cancelled")
```

서버는 통계 페이지에서 "이번 달 청산 비율" 같은 지표 표시.

---

## A7. 로그인 + 토큰 관리

### 7-1. TokenStore (EncryptedSharedPreferences)

```kotlin
// auth/TokenStore.kt
object TokenStore {
    private const val FILE_NAME = "fj_secure_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context, FILE_NAME, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(context: Context, token: String, userId: String) {
        prefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun getToken(context: Context): String? =
        prefs(context).getString(KEY_TOKEN, null)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun isAuthenticated(context: Context): Boolean = getToken(context) != null
}
```

### 7-2. LoginActivity (토큰 입력)

크롬 확장과 동일하게 메인 사이트에서 받은 토큰을 직접 입력하는 방식.

```kotlin
class LoginActivity : AppCompatActivity() {

    private lateinit var tokenInput: EditText
    private lateinit var loginBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tokenInput = findViewById(R.id.token_input)
        loginBtn = findViewById(R.id.login_btn)

        loginBtn.setOnClickListener {
            val token = tokenInput.text.toString().trim()
            if (token.isEmpty()) return@setOnClickListener

            // 토큰 검증
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val user = ApiClient.verifyToken(token)
                    if (user != null) {
                        TokenStore.saveToken(this@LoginActivity, token, user.id)
                        // FCM 토큰 등록
                        val fcmToken = FirebaseMessaging.getInstance().token.await()
                        ApiClient.registerDevice(fcmToken)

                        withContext(Dispatchers.Main) {
                            startActivity(Intent(this@LoginActivity, OnboardingActivity::class.java))
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, "토큰 오류", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
```

---

## A8. 권한 안내 온보딩

### 8-1. OnboardingActivity

권한을 단계별로 안내. 각 단계마다 ViewPager로 슬라이드.

```
Step 1: "다른 앱 위에 표시" 권한
  - 설명: "비트겟 앱 위에 경고를 띄우려면 필요합니다"
  - "권한 설정 열기" 버튼 → Settings.ACTION_MANAGE_OVERLAY_PERMISSION

Step 2: 알림 권한 (Android 13+)
  - 설명: "푸시 알림을 받기 위해 필요합니다"
  - "권한 요청" 버튼 → requestPermissions(POST_NOTIFICATIONS)

Step 3: 배터리 최적화 예외
  - 설명: "백그라운드에서 안정적으로 작동하기 위해 필요합니다"
  - "설정 열기" 버튼

Step 4: 완료
  - "Emergency Brake가 준비됐습니다"
  - MainActivity로 이동
```

### 8-2. PermissionHelper

```kotlin
object PermissionHelper {

    fun hasOverlayPermission(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun requestOverlayPermission(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        activity.startActivityForResult(intent, REQUEST_OVERLAY)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
```

### 8-3. 제조사별 추가 안내

샤오미 MIUI, 화웨이 EMUI 등은 추가 설정 필요. 디바이스 모델 감지 후 안내:

```kotlin
when (Build.MANUFACTURER.lowercase()) {
    "xiaomi" -> showXiaomiGuide()
    "huawei" -> showHuaweiGuide()
    "samsung" -> showSamsungGuide()  // 보통 문제 없음
}
```

---

## A9. 메인 서버 통합

### 9-1. 메인 프로젝트 변경사항

`futures-journal` 메인 프로젝트에 다음 추가:

```prisma
model DeviceRegistration {
  id         String   @id @default(cuid())
  userId     String
  user       User     @relation(fields: [userId], references: [id], onDelete: Cascade)
  fcmToken   String   @unique
  platform   String                              // "android" | "ios"
  deviceName String?                             // "Pixel 8 Pro"
  lastSeenAt DateTime?
  createdAt  DateTime @default(now())

  @@index([userId])
}
```

API 라우트:
- `POST /api/devices/register` — FCM 토큰 등록
- `DELETE /api/devices/:id` — 디바이스 해제
- `GET /api/devices` — 내 디바이스 목록

### 9-2. FCM 발송 함수

```typescript
// src/lib/fcm-sender.ts
import { initializeApp, cert, getApps } from "firebase-admin/app";
import { getMessaging } from "firebase-admin/messaging";

if (!getApps().length) {
  initializeApp({
    credential: cert({
      projectId: process.env.FIREBASE_PROJECT_ID!,
      clientEmail: process.env.FIREBASE_CLIENT_EMAIL!,
      privateKey: process.env.FIREBASE_PRIVATE_KEY!.replace(/\\n/g, "\n")
    })
  });
}

export async function sendEmergencyPush(
  userId: string,
  payload: {
    sessionId: string;
    level: number;
    symbol: string;
    side: string;
    size: number;
    sizeMultiplier: number;
    triggers: string[];
    forceSentence: string;
    countdown: number;
  }
): Promise<boolean> {
  const devices = await prisma.deviceRegistration.findMany({
    where: { userId, platform: "android" }
  });

  if (devices.length === 0) return false;

  for (const device of devices) {
    try {
      await getMessaging().send({
        token: device.fcmToken,
        data: {
          type: "emergency",
          sessionId: payload.sessionId,
          level: payload.level.toString(),
          symbol: payload.symbol,
          side: payload.side,
          size: payload.size.toString(),
          sizeMultiplier: payload.sizeMultiplier.toString(),
          triggers: payload.triggers.join(","),
          forceSentence: payload.forceSentence,
          countdown: payload.countdown.toString()
        },
        android: {
          priority: "high",
          notification: {
            channelId: "emergency",
            visibility: "public"
          }
        }
      });
    } catch (e) {
      console.error("FCM send failed", e);
    }
  }
  return true;
}
```

### 9-3. emergency-scheduler 확장

기존 텔레그램 도배에 FCM 발송도 추가:

```typescript
// startEmergencySequence 함수 내
if (decision.level >= 2) {
  // FCM 푸시 (모바일 안드로이드 앱)
  await sendEmergencyPush(userId, {
    sessionId: session.id,
    level: decision.level,
    // ...
  });
}
```

### 9-4. 환경변수 추가

`.env`:
```
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_CLIENT_EMAIL=firebase-adminsdk-xxx@your-project.iam.gserviceaccount.com
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n..."
```

⚠️ Firebase 서비스 계정 키 다운로드 필요:
```
Firebase Console → 프로젝트 설정 → 서비스 계정 →
"새 비공개 키 생성" → JSON 다운로드 → 환경변수에 입력
```

---

## A10. 빌드 + 구글 플레이 등록

### 10-1. 디버그 빌드 (개발/테스트)

```bash
./gradlew assembleDebug

# APK 위치
app/build/outputs/apk/debug/app-debug.apk
```

본인 디바이스에 직접 설치 후 테스트.

### 10-2. 릴리즈 빌드

서명 키 생성:
```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias futures-journal
```

`app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("release.jks")
            storePassword = "비밀번호"
            keyAlias = "futures-journal"
            keyPassword = "비밀번호"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

```bash
./gradlew bundleRelease

# AAB 위치 (구글 플레이용)
app/build/outputs/bundle/release/app-release.aab
```

⚠️ 멈추고 사용자에게 안내: 서명 키 비밀번호는 안전한 곳에 보관 필수. 분실 시 앱 업데이트 불가.

### 10-3. 구글 플레이 등록

⚠️ 멈추고 사용자에게 안내:

```
1. https://play.google.com/console 가입
2. 개발자 등록비 $25 결제 (1회성)
3. "앱 만들기"
4. 앱 정보 작성:
   - 이름: Futures Journal — Emergency Brake
   - 카테고리: 금융
   - 콘텐츠 등급: 만 17세 이상
5. AAB 업로드 (closed test → open test → production 순)
6. 권한 사용 정당화:
   - "SYSTEM_ALERT_WINDOW는 사용자가 사전 설정한 매매 룰 위반 시 경고 표시 목적"
7. 프라이버시 정책 URL (필수)
8. 심사 제출 (1~3일 소요)
```

### 10-4. 베타 테스트 흐름

```
1. 내부 테스트 (Internal Testing) — 본인 계정만
2. 클로즈드 베타 (Closed Testing) — 초대된 사용자
3. 오픈 베타 (Open Testing) — 누구나 참여
4. 정식 출시 (Production)
```

권장: 클로즈드 베타 → 2주 운영 → 오픈 베타 → 1개월 → 정식 출시.

---

## 작업 후 점검

모든 단계 완료 후:

1. `./gradlew build` 성공 확인
2. 디버그 APK 생성
3. 실제 디바이스에 설치 후 테스트:
   - 권한 부여 흐름 정상
   - 토큰 등록 정상
   - 메인 서버에서 테스트 FCM 발송 시 오버레이 표시
   - 강제 타이핑 검증 작동
4. 변경/생성 파일 목록 보고
5. 진행 상태 표 업데이트

### 통합 테스트 시나리오

```
1. 메인 사이트에서 디바이스 등록 토큰 발급
2. 안드로이드 앱에서 토큰 등록
3. 권한 4종 부여 (오버레이, 알림, 배터리, 진동)
4. /settings/notifications에서 "안드로이드 푸시 테스트" 발송
5. 비트겟 모바일 앱 위로 경고 오버레이 표시
6. 강제 타이핑 후 닫힘 확인
7. 메인 서버에 통계 기록 확인
```

---

## 주의사항 및 한계

### 기술적 한계

- 안드로이드 전용 (iOS 별도 개발 필요)
- iOS는 SYSTEM_ALERT_WINDOW 같은 권한 없음 (Critical Alerts로 우회 필요)
- 진입 직후 3~7초 지연 (즉시 차단 불가)
- 일부 제조사 ROM은 추가 설정 필요
- 사용자가 강제 종료하면 작동 안 함

### 정책적 한계

- 구글 플레이 심사 까다로움 (SYSTEM_ALERT_WINDOW)
- 권한 정당화 문서 필수
- 거래소 관련 앱이라 추가 검토 가능

### 디자인 원칙

- 권한 미부여 시에도 앱 기본 기능은 작동
- 푸시는 옵션이지만 권장
- 오버레이 권한은 명확히 사유 설명

---

## 작업 순서 권장

```
A1 (환경 + 프로젝트 생성)
  → Android Studio 실행 가능 확인
A2 (manifest + 기본 구조)
  → 빈 앱 빌드 성공
A3 (Firebase + FCM)
  → 푸시 수신 테스트 (Firebase Console에서 발송)
A4 (OverlayService)
  → 오버레이 권한 부여 후 더미 표시
A5 (경고 UI)
  → Level 1/2/3 디자인 완성
A6 (타이핑 검증)
  → 키워드 매칭 작동
A7 (로그인 + 토큰)
  → 메인 서버 연동
A8 (권한 온보딩)
  → 처음 사용자 흐름 완성
A9 (메인 서버 통합)
  → 실제 FCM 발송 → 오버레이 표시까지 end-to-end
A10 (빌드 + 등록)
  → 디버그 APK → 베타 → 정식
```

---

## 메인 프로젝트와의 통합

이 앱은 별도 프로젝트이지만 메인 `futures-journal`에 다음이 추가되어야 함:

```
futures-journal/  (메인)
├── prisma/schema.prisma           → DeviceRegistration 모델
├── src/lib/fcm-sender.ts          → FCM 발송
├── src/app/api/devices/           → 디바이스 등록 API
├── src/lib/emergency-scheduler.ts → FCM 통합 (기존 텔레그램에 추가)
└── .env                           → Firebase Admin SDK 환경변수
```

이 메인 프로젝트 작업도 A9 단계에 포함되어 있음.

---

## 예상 개발 기간

| 단계 | 예상 시간 |
|------|---------|
| A1~A3 (셋업) | 1~2일 |
| A4~A6 (오버레이 핵심) | 5~7일 |
| A7~A8 (인증 + 권한) | 2~3일 |
| A9 (메인 서버 통합) | 2~3일 |
| A10 (빌드 + 등록) | 2~3일 |
| **총합** | **약 3~4주** |

추가로 구글 플레이 심사 1~3일.

---

**이 PHASE_ANDROID.md를 읽고 자동 진행해줘.**
