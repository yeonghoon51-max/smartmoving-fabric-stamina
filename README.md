# Smart Moving (Fabric 1.21.8)

Divisor의 **Smart Moving** (Forge/ModLoader, 마지막 버전 16.3 for MC 1.8.9)을 Fabric으로 옮긴 프로젝트입니다.
움직임 공식과 기본값은 원작 16.3 소스 코드에서 가져왔습니다.

## 조작 (원작 Smart Moving 16.3 기준)

**잡기 = 달리기 키(기본 왼쪽 Ctrl).** 원작은 두 키의 기본값이 같았습니다. 따로 쓰고 싶으면 `옵션 → 조작 → Smart Moving → 잡기`에 키를 지정하고 설정에서 `grabUsesSprintKey`를 `false`로 바꾸세요.

| 동작 | 조작 | 설명 |
| --- | --- | --- |
| 달리기 | **Ctrl** 누른 채 앞으로 | 걷기의 1.5배 (바닐라는 1.3배). 지침이 쌓임. W 두 번 달리기는 바닐라 속도, 지침 없음 |
| 달리기 점프 | 달리면서 점프 | 지금 수평 속도 × 2 (상한 있음). 지침 65 |
| 슬라이딩 | 달리면서 **Ctrl + Shift** | 앞으로 튀어나가며 미끄러짐 (얼음 위에서 더 멀리). 좌우 키로 방향 조절. Shift를 떼면 끝, 느려지면 기어가기로 |
| 헤드 점프 | 달리면서 **Ctrl + 점프를 누르고 있다가 뗌** | 머리부터 날아감. 짧게 누를수록 납작하고, 0.5초(10틱) 이상이면 일반 점프 높이 |
| 모아 뛰기 | 제자리에서 **Shift + 점프를 누르고 있다가 뗌** | 최대 1초(20틱) 충전, 1.3배 높이 |
| 기어가기 | **Shift 누른 채 Ctrl** 누르기 (또는 Z로 켜기/끄기) | Shift를 떼면 일어섬 (공간이 없으면 계속) |
| 벽 타기 | 벽을 보고 **Ctrl** 유지 | 앞 = 위, 뒤 = 아래, 좌우 = 옆. 꼭대기에서 자동으로 올라섬 |
| 벽 점프 | 벽 타는 중 점프 / **뒤 + 점프** | 위로 점프 / 벽을 박차고 뒤로 헤드 점프 |
| 빠른 사다리 | 사다리에서 앞키 | 바닐라보다 빠르게 올라감 |

### 여우 무빙

원작 코드에서 확인한 핵심 규칙은 두 가지입니다.

1. **슬라이딩하다가 공중에 뜨면 "활공"으로 바뀝니다.** 공기 저항이 거의 없습니다 (감속 0.999/틱, 일반 점프는 0.91).
2. **Ctrl + Shift를 누른 채 착지하면** 속도를 그대로 살려서 다시 슬라이딩합니다.

그래서 **달리기 → Ctrl+Shift(슬라이딩) → 점프 → (활공) → Ctrl+Shift 누른 채 착지 → 점프 → …** 로 이어가면
처음 슬라이딩 속도를 거의 잃지 않고 계속 날아갑니다. 슬라이딩에서 뛰는 점프는 지침이 들지 않고, 처음 슬라이딩할 때만 지침 10이 듭니다.
내리막에서 슬라이딩하면 턱마다 저절로 활공합니다.

### 지침 (원작 exhaustion)

행동하면 올라가고 쉬면 내려갑니다. 배고픔 줄 위에 **번개 아이콘**으로 남은 체력이 표시됩니다.

- 행동마다 "이 값 이하일 때만 시작"과 "이 값을 넘으면 중단" 기준이 따로 있습니다. 회색 번개는 지금 하려는 행동에 필요한 만큼을 나타냅니다.
- 회복 속도는 가만히 서 있을 때 2배, 공중에서 2.5배이고 달리는 중에는 회복되지 않습니다.
- 배고픔이 4칸 이하면 회복되지 않습니다.

모아 뛰기와 헤드 점프 충전량은 체력 줄 위에 **파란 다이아**로 표시됩니다.

## 원작과 다른 점

- **벽 타기**: 원작은 벽에 틈이나 턱이 있어야만 탈 수 있었습니다. 이 포트는 아직 아무 벽이나 탈 수 있습니다. 그래서 원작에서는 꺼져 있던 벽 타기 지침을 켜 두었습니다.
- **아직 없는 기능**: 공중에서 벽을 차는 벽 점프, 옆·뒤 점프, 천장 매달리기, 수영·다이빙, 비행, 원작 애니메이션.
- **지침이 배고픔으로 바뀌는 부분**: 원작은 지침이 회복될 때 배고픔을 조금 소모했습니다. 이 포트에는 없습니다.

## 설정

처음 실행하면 `config/smartmoving.json`이 생깁니다. 각 기능 on/off, 지침 수치, 점프 배율 등을 바꿀 수 있습니다.
기본값은 원작의 기본 설정과 같고, 각 항목 옆 주석에 원작 설정 키 이름을 적어 두었습니다.

## 멀티플레이

**서버와 클라이언트 모두에 설치해야 합니다.** 서버에 모드가 없으면 클라이언트 기능이 자동으로 꺼집니다
(자세·낙하 대미지 판정이 서버와 어긋나는 것을 막기 위함).

## 빌드

JDK 21 필요.

```
./gradlew build
```

결과물: `build/libs/smartmoving-fabric-<버전>.jar` → `.minecraft/mods`에 Fabric API와 함께 넣으면 됩니다.
GitHub Actions에서도 푸시할 때마다 빌드되며, Actions 탭의 `smartmoving-fabric` 아티팩트에서 jar를 받을 수 있습니다.

게임에서 바로 테스트: `./gradlew runClient`

## 코드 구조

```
src/main/java/com/smartmoving/        (서버+클라이언트 공용)
  SmartMoving.java                    진입점
  config/SmartMovingConfig.java       설정 파일
  state/SmartMovingState.java         플레이어별 상태 (기어가기/벽타기/슬라이딩/헤드점프/지침 ...)
  state/SmartMovingPlayer.java        PlayerEntity 에 상태를 붙이는 인터페이스
  logic/MovementPhysics.java          벽타기·슬라이딩 물리 (바닐라 travel 대체)
  logic/WallProbe.java                앞에 벽/턱이 있는지 검사
  logic/Jumps.java                    원작 tryJump 포트 (모든 점프 공식)
  logic/Exhaustion.java               원작 지침 계산
  network/                            클라이언트↔서버 상태 동기화 패킷
  mixin/PlayerEntityMixin.java        자세 강제, 벽타기 중 낙하거리 초기화
  mixin/LivingEntityMixin.java        travel 가로채기, 바닐라 점프 대체, 달리기 속도, 헤드 착지 대미지

src/client/java/com/smartmoving/client/ (클라이언트 전용)
  SmartMovingClient.java              진입점 (키, HUD, 패킷 수신)
  ClientMovementController.java       키 입력 → 상태 결정 (핵심 로직)
  ExhaustionHud.java                  원작 HUD (번개 = 지침, 다이아 = 점프 충전)
  mixin/ClientPlayerEntityMixin.java  매 틱 컨트롤러 호출, 지치면 달리기 금지
  mixin/BipedEntityModelMixin.java    벽타기 팔 애니메이션
```

## 라이선스

GPL-3.0-or-later. 원작 Smart Moving / Smart Render (© Divisor)가 GPL-3.0-or-later이고, 이 프로젝트는 그 코드를 옮겨 왔기 때문에 같은 라이선스를 따릅니다.
HUD 아이콘(`textures/gui/icons.png`)도 원작에서 가져왔습니다.
