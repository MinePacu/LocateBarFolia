# LocateBarFolia

[English README](README.md)

LocateBarFolia는 Folia 환경을 지원하는 Paper 플러그인입니다. 근처에 있는 플레이어를 Minecraft의 추적 웨이포인트 표시로 보여줍니다. Java Edition 플레이어에게는 네이티브 웨이포인트 패킷을 보내고, Floodgate로 접속한 Bedrock 플레이어에게는 액션바 기반 대체 표시를 제공합니다.

## 주요 기능

- 근처의 활성화된 플레이어를 locator 웨이포인트로 표시합니다.
- Folia region scheduler 흐름을 지원합니다.
- 플레이어별 화면 표시 켜짐/꺼짐 설정을 저장합니다.
- 월드, 반경, 관전자 모드, 선택적 vanish 가시성 기준으로 대상을 필터링합니다.
- Floodgate 기반 Bedrock 액션바 대체 표시를 지원합니다.
- 설정 리로드와 스캔 반경 변경을 서버 실행 중에 처리할 수 있습니다.

## 요구 사항

- Java 21
- Paper/Folia API `1.21`과 호환되는 Minecraft 서버
- Paper dev bundle `1.21.8-R0.1-SNAPSHOT` 기준으로 빌드됨
- 선택 사항: Bedrock 대체 표시를 위한 Floodgate 및 Geyser

## 빌드

```sh
./gradlew build
```

플러그인 jar는 다음 위치에 생성됩니다.

```text
app/build/libs/LocateBarFolia-1.0.1.jar
```

## 설치

1. 플러그인을 빌드하거나 jar 파일을 준비합니다.
2. `LocateBarFolia-1.0.1.jar`를 서버의 `plugins` 폴더에 넣습니다.
3. 서버를 재시작합니다.
4. 필요하면 `plugins/LocateBarFolia/config.yml`을 수정합니다.
5. 설정을 바꾼 뒤 `/locatebar reload`를 실행합니다.

## 명령어

| 명령어 | 권한 | 설명 |
| --- | --- | --- |
| `/locatebar` | 없음 | 자기 화면의 LocateBar 표시 상태를 전환합니다. |
| `/locatebar on` | 없음 | 자기 화면의 LocateBar 표시를 켭니다. |
| `/locatebar off` | 없음 | 자기 화면의 LocateBar 표시를 끕니다. |
| `/locatebar toggle` | 없음 | 자기 화면의 LocateBar 표시 상태를 전환합니다. |
| `/locatebar radius` | `locatebar.admin` | 현재 스캔 반경을 표시합니다. |
| `/locatebar radius <blocks>` | `locatebar.admin` | 스캔 반경을 변경하고 저장합니다. 8보다 작은 값은 8로 보정됩니다. |
| `/locatebar reload` | `locatebar.admin` | 플러그인 설정을 다시 불러옵니다. |

## 권한

| 권한 | 기본값 | 설명 |
| --- | --- | --- |
| `locatebar.use` | `true` | 호환성을 위해 남겨둔 권한입니다. 플레이어 본인의 on/off/toggle 명령어는 모든 플레이어가 사용할 수 있습니다. |
| `locatebar.admin` | `op` | 반경 변경과 설정 리로드를 허용합니다. |

## 설정

```yaml
enabled-by-default: true
scan-radius: 48.0
movement-threshold-blocks: 0.5
ignore-spectators: true
ignore-vanished: false
bedrock-fallback:
  enabled: true
  actionbar-interval-ticks: 20
  max-targets: 4
```

| 옵션 | 설명 |
| --- | --- |
| `enabled-by-default` | 저장된 개인 설정이 없을 때 기본적으로 LocateBar 표시를 켤지 정합니다. |
| `scan-radius` | 대상을 탐지할 최대 반경입니다. 유효하지 않은 값은 `48.0`으로 바뀌고, `8.0`보다 작은 값은 보정됩니다. |
| `movement-threshold-blocks` | 대상 가시성을 다시 계산하기 위한 최소 이동 거리입니다. `0.05`보다 작은 값은 보정됩니다. |
| `ignore-spectators` | 관전자 모드 플레이어를 수신자 또는 대상으로 제외합니다. |
| `ignore-vanished` | 켜면 Bukkit 가시성 검사 기준으로 수신자가 볼 수 있는 대상만 표시합니다. |
| `bedrock-fallback.enabled` | Floodgate Bedrock 플레이어용 액션바 대체 표시를 켭니다. |
| `bedrock-fallback.actionbar-interval-ticks` | 액션바 갱신 주기입니다. `10`보다 작은 값은 보정됩니다. |
| `bedrock-fallback.max-targets` | Bedrock 대체 표시에서 한 번에 보여줄 최대 대상 수입니다. `1`보다 작은 값은 보정됩니다. |

## Bedrock 대체 표시

Floodgate가 설치되어 있으면 LocateBarFolia는 Floodgate API로 Bedrock 플레이어를 감지합니다. Bedrock 플레이어에게는 Java 웨이포인트 패킷을 보내지 않고, 다음과 같은 액션바 메시지를 표시합니다.

```text
Locate: F Alex 10m | L Steve 20m
```

방향 코드는 수신자가 바라보는 방향을 기준으로 합니다.

| 코드 | 의미 |
| --- | --- |
| `F` | 앞 |
| `FR` | 앞-오른쪽 |
| `R` | 오른쪽 |
| `BR` | 뒤-오른쪽 |
| `B` | 뒤 |
| `BL` | 뒤-왼쪽 |
| `L` | 왼쪽 |
| `FL` | 앞-왼쪽 |

Floodgate가 설치되어 있지 않아도 Java Edition 플레이어용 기능은 그대로 동작하며, Bedrock 대체 표시가 비활성화되었다는 로그만 출력됩니다.

## 개발 참고

이 프로젝트는 `io.papermc.paperweight.userdev`를 사용합니다. IDE에서는 Gradle wrapper로 프로젝트를 가져와야 합니다. VS Code에서 `net.minecraft` import가 unresolved로 보이면 Java extension pack을 사용하고, `./gradlew build` 실행 후 Java language server를 다시 로드하세요.
