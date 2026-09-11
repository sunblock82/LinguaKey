# LinguaKey 2.1.1 검토 기록

기능·입력, 번역·학습, 보안·개인정보 영역을 분담한 AI 코드 검토와 통합 검토 결과입니다. 외부 사람 전문가의 인증이나 침투 테스트가 아닙니다.

| 영역 | 발견 및 반영 | 검증 근거 |
|---|---|---|
| 번역 보존 | 전체 번역을 짧은 예문으로 바꾸는 규칙 제거, 전체 작성문 추출·문단 유지, 길이 초과 오류 | TranslationIntegrityTest, PrivacyAndPipelineTest |
| 비동기 결과 | 입력 변경·비공개 진입 후 이전 결과가 표시·저장되지 않도록 세대 번호와 취소 처리, 캐시 삭제 | 취소 후 콜백·다음 청크·캐시 회귀 테스트 |
| 문맥 번역 | GPT/Gemini 고정 공식 HTTPS API, 현재 작성문+수동 상황·말투, 입력과 지시 분리 | 응답 파싱·거절·출력 제한·엔드포인트 테스트; 실 API 미검증 |
| 키보드 | 한글 조합, 연속 모음, 쌍자음 되돌리기, 선택 삭제, 이모지 단위 삭제 | HangulComposerTest, CompositionEditsTest, KeyboardTextEditsTest, Android 실제 EditText 테스트 |
| 화면·타건 | 시스템 내비게이션 inset, 상세 크기·간격·반응 옵션, 삭제 반복 종료 처리 | 설정 화면 실행·옵션 저장 에뮬레이터 테스트; 실제 휴대전화 감도 미실측 |
| 학습 | 단어 수 기반 CEFR 단정 제거, 표현 예시 분리, 답 숨기기·개별 삭제·복습 페이지 | 번역 보존 테스트 및 복습 화면 실행 테스트 |
| API 키 | Keystore AES-GCM, 제공자 AAD 결합, 손상 시 빈 키 반환, 암호화 데이터 삭제, 설정 캡처 방지 | Android Keystore 왕복·제공자 교체·손상·삭제 테스트 |
| 개인정보 | 비밀번호·인증 입력 보호, 외부 맞춤법 검사기 호출 제거, 백업·이전 제외, 문장·통계 삭제 | 민감 입력·로케일 테스트, manifest·네트워크 호출 검토 |
| TTS | 네트워크를 요구하지 않는 설치된 영어 음성만 선택 | 코드 검토; 제3자 엔진 행동은 실측하지 않음 |
| 배포 | 최종 release APK 디버깅 금지, 저장소·Actions 밖에서 고정 배포 키로 서명, CI 보고서 보관 | apksigner 및 aapt 검사 |

## 자동 테스트
GitHub Actions는 JVM 단위 테스트와 Android 35 x86_64 에뮬레이터 테스트를 실행합니다. API 키는 테스트에 사용하지 않습니다. Keystore 테스트 문자열은 가짜이며 네트워크로 전송하지 않습니다. 성공 여부는 각 커밋의 Actions 결과와 `LinguaKey-test-reports` 아티팩트로 확인합니다.

## 배포 서명 개선
기존 CI는 임시 디버그 키의 캐시 경로가 실제 생성 경로와 달라 키를 보존하지 못했습니다. 이전 2.0.1의 개인키는 APK에서 복구할 수 없습니다. 이번 배포부터 개인 배포 키를 별도로 영구 보관하고 CI 밖에서 서명합니다. 서명키를 GitHub 캐시에 저장하는 방식도 제거했습니다. 이번 전환에는 재설치가 필요하며 앱 삭제 시 기존 저장 데이터가 사라집니다. 이후 업데이트는 보관한 동일 키를 재사용해야 합니다.

## 남은 제약
- 번역 결과의 자연스러움·의미 보존을 보장할 수 없습니다. 실제 API 키를 앱에서 설정한 후 사용자 문장으로 검증해야 합니다. 받은 대화 자동 수집은 구현하지 않았으므로 필요한 문맥을 직접 지정해야 합니다.
- 공개 서비스용 인증 서버·한도 관리 체계는 없습니다. 개인 API 키 방식이며 API 비용은 사용자 제공자 계정에 청구됩니다. 취소해도 이미 처리된 요청 비용은 생길 수 있습니다.
- 문장 저장은 Android 앱 전용 저장소에 의존합니다. 루팅·기기 전체 침해·제3자 접근성/TTS 서비스에 대한 보호를 보장하지 않습니다.
- ML Kit 입력·출력은 기기 내 처리지만 SDK 진단 통신이 존재합니다. 추가 분석 SDK를 붙이지 않았다는 사실을 ‘수집 없음’으로 확대하지 않았습니다.
- 제조사별 키보드 inset, 회전, 멀티윈도, 긴 문장 타건감은 실제 휴대전화에서 확인해야 합니다. 네이버 키보드의 모든 레이아웃·스와이프·음성·추천 학습 기능을 복제한 것은 아닙니다.

## 참고한 공식 문서
- [OpenAI 텍스트 생성](https://developers.openai.com/api/docs/guides/text)
- [Gemini generateContent](https://ai.google.dev/api/generate-content)
- [Android 백업 제어](https://developer.android.com/identity/data/autobackup)
- [Android TTS Voice](https://developer.android.com/reference/android/speech/tts/Voice)
- [ML Kit 개인정보](https://developers.google.com/ml-kit/terms)
- [ML Kit 데이터 공개](https://developers.google.com/ml-kit/android-data-disclosure)
