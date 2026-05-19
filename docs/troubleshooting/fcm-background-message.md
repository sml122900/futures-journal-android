# FCM 백그라운드 수신 시 onMessageReceived 미호출 문제

## 문제 상황

FCM 메시지가 서버에서 발송되고 Android 기기에 도달하는 것은 확인됐으나, `FcmService.onMessageReceived`가 호출되지 않아 `OverlayService`가 시작되지 않았다. 앱이 포그라운드일 때는 간헐적으로 동작하는 것처럼 보였다.

## 시도한 것들

1. FCM 토큰 등록 확인 → 정상
2. `FcmService` 매니페스트 등록 확인 → 정상
3. `adb logcat` 분석:
   ```
   W FirebaseMessaging: Notification Channel requested (emergency) has not been created by the app.
   I NotificationManager: notify(..., channel=fcm_fallback_notification_channel ...)
   ```
   → 시스템이 알림을 직접 처리하고 있었음. `onMessageReceived` 흔적 없음.

## 최종 해결법

`fcm-sender.ts`에서 `android.notification` 블록을 제거해 **순수 data message**로 전송.

```typescript
// 수정 전 (문제)
android: {
  priority: "high",
  notification: {           // ← 이 블록이 문제
    channelId: "emergency",
    visibility: "public",
  },
}

// 수정 후 (해결)
android: {
  priority: "high",         // notification 블록 제거
}
```

**원인:** FCM에서 `notification` 블록이 있으면 앱이 백그라운드/종료 상태일 때 Firebase SDK가 `onMessageReceived`를 호출하지 않고 시스템이 직접 알림 트레이에 표시한다. 순수 data message는 앱 상태와 무관하게 항상 `onMessageReceived`를 호출한다.

## 이력서 소재 한 줄

> adb logcat 분석으로 FCM notification/data message 혼용 버그를 진단하고, 순수 data message로 전환해 앱 포/백그라운드 무관한 오버레이 트리거 파이프라인 완성.
