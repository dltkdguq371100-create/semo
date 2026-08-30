# 세모 (Semo) MVP

갤럭시 S25 세로 화면을 기준으로 만든 오프라인 채팅형 개인 메모 앱입니다. 메모를 메시지처럼 빠르게 기록하고, 여러 원본 메모를 선택해 서로 독립적인 편집 내용을 가진 묶음으로 정리합니다.

## 주요 기능

- 여러 줄 채팅형 메모 작성, 수정, 확인 후 삭제
- 날짜 구분선이 포함된 메모·묶음 통합 타임라인
- 길게 눌러 다중 선택 후 즉시 묶음 생성
- 원본을 보존하는 묶음 제목 및 편집 내용 자동 저장
- many-to-many 방식의 묶음 원본 추가·제거
- 묶음 고정, 보관, 삭제 및 통합 검색
- 밝은 화이트·무채색 Material 3 디자인과 edge-to-edge UI
- Room 로컬 저장, StateFlow 기반 MVVM, DataStore 환경설정

## 빌드

JDK 17과 Android SDK 36이 준비된 환경에서 다음을 실행합니다.

```powershell
./gradlew.bat test lint assembleDebug
```

생성 APK: `app/build/outputs/apk/debug/app-debug.apk`

## 구조

- `data/Models.kt`: Room 엔티티, many-to-many 관계, UI 타임라인 모델
- `data/SemoDao.kt`: Flow 기반 DAO
- `data/SemoRepository.kt`: 트랜잭션과 도메인 규칙
- `ui/SemoViewModel.kt`: SavedStateHandle, StateFlow, 선택/저장 상태
- `MainActivity.kt`: Navigation Compose 및 Compose 화면

내보내기·가져오기는 설정 화면에 후속 기능으로 표시되어 있으며 현재 MVP에는 포함하지 않습니다.

## 검증

- `testDebugUnitTest`: 8개 통과
- `lintDebug`: 오류 0개
- `assembleDebug`: 성공
- `assembleDebugAndroidTest`: 성공

최종 디버그 APK는 Codex 결과물 폴더의 `Semo-1.0-light-timeline-debug.apk`로 제공됩니다.
