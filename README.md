# Smart Moving (Fabric 1.21.8)

예전 Forge/ModLoader 시절의 **Smart Moving** 모드를 Fabric으로 다시 만든 프로젝트입니다.

## 기능

| 기능 | 조작 | 설명 |
| --- | --- | --- |
| 스태미나 | - | 달리기·벽타기·모아뛰기·슬라이딩에 소모됩니다. 0이 되면 **지침** 상태가 되어 30%까지 회복될 때까지 달리기/벽타기 불가. 배고픔 줄 위 노란 바로 표시 |
| 벽 타기 | 벽을 보고 **R**(잡기) 유지 | 앞키 = 위로, 뒤키 = 아래로, 좌우키 = 벽을 따라 옆으로. 아무 키도 안 누르면 매달려 있음. 꼭대기에 닿으면 자동으로 턱 위로 올라섬. 벽 타는 동안 낙하 거리 초기화 |
| 벽 짚고 도약 | 벽 타는 중 **점프** | 위로 튀어오름 |
| 벽 점프 | 벽 타는 중 **뒤 + 점프** | 벽을 박차고 뒤로 뜀 |
| 기어가기 | **Z** (켜기/끄기) | 1칸 높이 틈을 지나갈 수 있음 (바닐라 기어가기 자세 사용 → 다른 플레이어에게도 보임) |
| 슬라이딩 | 달리다가 **웅크리기** | 앞으로 미끄러짐. 웅크리기를 계속 누르고 있으면 끝난 뒤 기어가기로 이어짐. 슬라이딩 중 점프 가능 |
| 헤드 점프 (여우 점프) | 달리면서 **R(잡기) + 점프** | 몸을 눕혀 머리부터 앞으로 멀리 날아감. 공중 감속이 거의 없고, 시점을 돌리면 그쪽으로 조금씩 꺾임. 슬라이딩 중에도 가능 |
| 착지 슬라이딩 | 공중에서 **웅크리기** 누른 채 착지 | 속도를 그대로 살려 슬라이딩으로 이어짐 |
| 모아 뛰기 | 제자리에서 **웅크리기** 유지 → **점프** | 최대 약 3칸 높이. 조준점 아래 파란 게이지 |
| 빠른 사다리 | 사다리에서 앞키 | 바닐라보다 빠르게 올라감 |
| 애니메이션 | - | 벽 타는 플레이어는 팔을 위로 뻗은 자세 (멀티플레이에서도 동기화) |

### 여우 무빙 (연계)

**달리기 → 웅크리기(슬라이딩) → R + 점프(헤드 점프) → 웅크리기 누른 채 착지(슬라이딩) → R + 점프 ...**

R · 점프 · 웅크리기를 계속 누르고 달리면 착지할 때마다 자동으로 슬라이딩 → 헤드 점프가 이어집니다.
헤드 점프는 지금 속도에 부스트를 더하므로 연계할수록 빨라지고(상한 `headJumpMaxSpeed`),
대신 한 번에 스태미나를 15씩 써서 무한히 이어갈 수는 없습니다.

키는 `옵션 → 조작 → Smart Moving`에서 바꿀 수 있습니다.

## 설정

처음 실행하면 `config/smartmoving.json`이 생깁니다. 각 기능 on/off, 스태미나 소모량, 속도 등을 바꿀 수 있습니다.

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
  state/SmartMovingState.java         플레이어별 상태 (기어가기/벽타기/슬라이딩/스태미나 ...)
  state/SmartMovingPlayer.java        PlayerEntity 에 상태를 붙이는 인터페이스
  logic/MovementPhysics.java          벽타기·슬라이딩 물리 (바닐라 travel 대체)
  logic/WallProbe.java                앞에 벽/턱이 있는지 검사
  logic/Stamina.java                  스태미나 계산
  network/                            클라이언트↔서버 상태 동기화 패킷
  mixin/PlayerEntityMixin.java        자세 강제, 벽타기 중 낙하거리 초기화
  mixin/LivingEntityMixin.java        travel 가로채기, 모아 뛰기 점프력

src/client/java/com/smartmoving/client/ (클라이언트 전용)
  SmartMovingClient.java              진입점 (키, HUD, 패킷 수신)
  ClientMovementController.java       키 입력 → 상태 결정 (핵심 로직)
  StaminaHud.java                     스태미나 바, 모아뛰기 게이지
  mixin/ClientPlayerEntityMixin.java  매 틱 컨트롤러 호출, 지치면 달리기 금지
  mixin/BipedEntityModelMixin.java    벽타기 팔 애니메이션
```
