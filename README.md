# Chrono Mod (Fabric 1.21.4)

## GitHub으로 jar 만들기 (설치 불필요)
1. github.com 가입 후 New repository 로 빈 저장소를 만든다.
2. 이 폴더 안의 내용물 전체(.github 폴더 포함)를 저장소에 업로드한다.
3. 저장소의 Actions 탭 > build 가 초록색 체크가 될 때까지 기다린다 (약 3~5분).
4. 완료된 실행을 열고 맨 아래 Artifacts 의 chronomod-jar 를 내려받는다.
5. zip 안의 chronomod-1.0.0.jar 를 1.21.4 Fabric 프로필의 mods 폴더에 넣는다. (Fabric API 필요)

## 사용법
- 크리에이티브 탭 > 도구 또는 /give @s chronomod:chrono_watch
- 우클릭: 능력 발동 / Shift+우클릭: 능력 변경
