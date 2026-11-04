# 콜 체크리스트 (CallCheck)

스마트 메신저(TeamingTalk) 콜 알림을 ntfy로 받아 **한 앱에서** 알림 수신 + 시간대별 체크리스트를 처리하는 안드로이드 앱.

- 푸시 수신: ntfy 토픽에 상시 연결(포그라운드 서비스) → 콜 알림을 받으면 **체크리스트에 자동 추가** + 고중요도 알림(갤럭시워치 미러링)
- 데이터: 폰 내부에만 저장(서버 전송 없음). 알림 내용에는 환자정보가 들어가지 않음("콜 도착 + 시각"만).
- PC 쪽 송신: `watch-smartmsg.ps1` (상위 폴더). 두 곳의 **토픽 이름이 같아야** 합니다.

현재 토픽: `smsg-9f3k7q2x8m` — 바꾸려면 `app/src/main/java/com/callcheck/app/Config.kt` 와 PC 스크립트의 `$Topic` 을 **둘 다** 동일하게 수정.

## APK 빌드 (로컬 설치 불필요 · GitHub Actions)

1. GitHub에 비공개 저장소를 만들고 이 폴더(`callcheck-android`)를 push 합니다.
   ```
   gh repo create callcheck-android --private --source . --remote origin --push
   ```
   (또는 수동: git init → add → commit → remote add → push, 브랜치명 `main`)
2. 저장소 **Actions** 탭에서 `build-apk` 워크플로가 자동 실행됩니다. (안 되면 workflow_dispatch로 수동 실행)
3. 완료되면 실행 결과 페이지 하단 **Artifacts → `callcheck-debug-apk`** 를 내려받습니다 → 압축 안에 `app-debug.apk`.

## 폰에 설치

1. `app-debug.apk` 를 폰으로 옮겨 실행 → "출처를 알 수 없는 앱 설치" 허용 → 설치.
2. 첫 실행 시:
   - **알림 권한** 허용
   - **배터리 최적화 제외** 허용(상시 수신에 중요)
3. 상단에 "콜 체크리스트 실행 중" 상시 알림이 보이면 수신 대기 정상.

### 사내 관리 단말(MDM)이면
사이드로딩이 차단될 수 있습니다. 그 경우 회사 정책 확인이 필요합니다.

## 동작 점검

PC에서 테스트 송신:
```
powershell -ExecutionPolicy Bypass -File ..\watch-smartmsg.ps1 -Test
```
→ 폰/워치에 "📞 (테스트) 콜 도착" 알림이 오고, 앱 체크리스트에도 한 건 추가되면 성공.
