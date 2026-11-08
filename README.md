# RF-Scope

Android 10+（minSdk 29）Kotlin + Jetpack Compose 的 RF/网络监测 App 骨架。

## 当前已落地

- 纯黑背景 + `#00FF00` 专业频谱仪风格。
- 英文主标签 + 紧邻的中文小号/灰色注释：`RSRP  (参考信号接收功率)  -70 dBm`。
- `CellInfoNr` / `CellIdentityNr` / `CellSignalStrengthNr` 读取骨架。
- 中国移动、联通、电信、广电 MCC/MNC 静态运营商表。
- n41、n79、n28、n78、n1、GSM900 静态频段表。
- `NR-ARFCN -> MHz` 3GPP 换算。
- SINR < 0 dB 红色“Co-Channel Interference Risk / 同频干扰风险”提示。
- 首次启动自动请求权限；蜂窝与 Wi-Fi 权限可独立重新授权。
- Wi-Fi 扫描、频段/信道/频宽/标准识别与 2.4/5/6 GHz 提示。
- 手动刷新带 SPINNER、SYNC 状态和更新时间反馈。
- 底部三页切换：LIVE / WI-FI / HARDWARE。
- 信号趋势图包含网格、纵轴刻度、时间轴。
- 硬件页可选 HT8、U5 V3 八木、拉杆 GP、小环天线。
- USB Host 设备检测（VID/PID、CDC 串口、权限状态）。
- TinySA VID 0x0483 / PID 0x5740 识别、USB 权限请求和 USB 设备选择器。
- TinySA `scanraw` 串口读取：400–500 MHz、200 点、峰值/噪声底/SNR。
- Wi‑Fi 2.4/5/6 GHz 信道占用图，按信号强度着色。
- 趋势图按固定 120 秒窗口、缺失采样断线，并显示相对时间刻度（0s、-Xs、-Xm）。
- 实时 TinySA 频谱扫描、LIVE/STOP、单次扫描和范围预设。
- 手动起始/终止频率、扫描点数、RBW 和 VBW/AVG 选项。
- 移动/联通/电信/广电 4G/5G 频段选择。
- PEAK / AVERAGE / MARKER 三种 TONE 模式与可调参考电平。
- 多 Marker M1-M4。
- 固定行高、按扫描逐行堆叠并带时间刻度的瀑布图。
- WiFi 峰顶 SSID 标注、5GHz 空闲信道压缩显示。
- 最长 240 帧频谱瀑布图。
- 峰值强度驱动的自动升调/升音量提示音。
- RSRP/RSRQ/SINR 三格信号图标。
- 事件驱动 SignalStrength 回调采样，轮询仅作兜底。
- Wi‑Fi 信道图改用开口向下的抛物线包络。
- n41 / FREQ / PCI / RSRP / RSRQ / SINR 重点指标卡。
- RSRP、RSRQ、SINR 三行并行实时趋势图（最近 60 个采样点）。
- TinySA Ultra USB 串口传输接口；HT8 作为 TinySA 的下游能力，不由手机直连。
- SNR、-3dB Width、Harmonic、Max Hold、Averaging 算法骨架。

## 关键文件

- `app/src/main/java/com/rfscope/app/ui/RFScopeScreen.kt`
- `app/src/main/java/com/rfscope/app/data/CellInfoReader.kt`
- `app/src/main/java/com/rfscope/app/data/BandCatalog.kt`
- `app/src/main/java/com/rfscope/app/data/WifiScanner.kt`
- `app/src/main/java/com/rfscope/app/hardware/RfMeasurement.kt`

## 权限矩阵

| 能力 | Android 10-11（API 29-30） | Android 12-12L（API 31-32） | Android 13-14（API 33-34） |
|---|---|---|---|
| `getAllCellInfo()` / `CellInfoNr` | `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION`，定位开关开启 | 同左，且用户必须授予“精确位置” | 同左 |
| `getDataNetworkType()` / 运营商上下文 | `READ_PHONE_STATE` | `READ_PHONE_STATE` | `READ_PHONE_STATE` |
| Wi-Fi 扫描 | `ACCESS_FINE_LOCATION`，定位开关开启 | 同左 | `NEARBY_WIFI_DEVICES`（声明 `neverForLocation`） |
| Wi-Fi 开关/扫描控制 | `ACCESS_WIFI_STATE`、`CHANGE_WIFI_STATE`（普通权限） | 同左 | 同左 |
| USB 串口（Module 4） | 无 Manifest 运行时权限；使用 `UsbManager.requestPermission()` | 同左 | 同左 |

说明：

- `ACCESS_BACKGROUND_LOCATION` 当前没有申请。只在后台持续采集时需要，而且 Google Play 审核更严格。
- `READ_PHONE_STATE` 是危险权限。若未来只读取极少量基础电话状态，可评估 Android 13+ 的 `READ_BASIC_PHONE_STATE`，但本骨架使用 `READ_PHONE_STATE` 以覆盖网络类型读取。
- `READ_PRIVILEGED_PHONE_STATE`、`MODIFY_PHONE_STATE`、`LOCAL_MAC_ADDRESS` 等属于系统/签名权限，普通第三方 App 不能依赖。
- Android 12+ 手机可能只授予“大致位置”；该状态下 CellInfo 通常会为空或被遮挡，因此 UI 明确要求精确位置。

## 5G NSA/SA 的真实系统限制

1. `CellInfoNr` 从 Android 10（API 29）开始存在，但“类存在”不等于“每台设备都返回数据”。
2. SA 场景下，若网络、基带和 OEM ROM 都允许，`getAllCellInfo()` 可返回 `CellInfoNr`，再从 `CellIdentityNr.getNrarfcn()`、`getPci()`、`getTac()` 和 `CellSignalStrengthNr` 读取参数。
3. NSA 场景下，很多终端只通过 `CellInfo` 暴露 LTE 锚点小区；NR 小区可能完全不出现。此时 `getDataNetworkType()` 可能返回 `NETWORK_TYPE_NR`，但公开 API 仍拿不到 NR band/frequency。
4. `CellIdentityNr.getBands()` 在 Android 11（API 30）才加入，且 OEM 可能返回空数组。**不能**用频段范围的中点伪造“中心频率”；本工程只在 `getNrarfcn()` 有效时计算频率。
5. 双卡设备应通过 `TelephonyManager.createForSubscriptionId(subId)` 读取目标数据卡；本骨架使用默认数据订阅。
6. 载波聚合、NSA 多连接、不同厂商的 Modem 实现会导致 `getAllCellInfo()` 返回的“服务小区”并不总是唯一或完整。
7. 公开 Android Telephony API 没有保证可读取全部 NR 调度、带宽、载波和邻区数据；这通常需要厂商 SDK、运营商权限或系统级权限。

## NR-ARFCN 换算

代码位于 `BandCatalog.kt` 的 `NrArfcn.toMhz()`：

- FR1 0–3000 MHz：`F = N × 0.005 MHz`
- FR1 3000–24250 MHz：`F = 3000 + (N - 600000) × 0.015 MHz`
- FR2 24250–100000 MHz：`F = 24250 + (N - 2016667) × 0.060 MHz`

例如 `n41` 的 `504990` 得到 `2524.95 MHz`。

## Wi-Fi 注意事项

- `ScanResult.BSSID` 是 AP 的 MAC，不是手机自身 Wi-Fi MAC；Android 6+ 不允许普通 App 获取本机真实 Wi-Fi MAC。
- 没有精确定位/定位开关时，SSID/BSSID 可能被隐藏或返回占位值。
- Android 会对 `startScan()` 限流；`startScan()` 返回 `false` 时仍可展示缓存结果。
- 2.4 GHz 的“拥堵”和 5 GHz 的“干净”只能是通用提示，不能替代信道占用率、噪声底和邻区扫描。
- 代码用 `channelWidth` 读取 20/40/80/160 MHz 等声明值；真正占用带宽还需结合 `centerFreq0/1` 与频谱仪测量。

## 构建

本仓库未包含 Gradle Wrapper，因为当前执行环境没有 JDK/Android SDK。导入 Android Studio 后：

1. 使用 Android Studio 的 Gradle Wrapper 生成/同步功能。
2. 安装 Android SDK Platform 34 和 Build Tools。
3. 连接 Android 10+ 真机运行。
4. 模拟器通常不会返回真实蜂窝小区，必须用带 SIM 的真机验证。

> 本文档和代码骨架中的 WiFi/Telephony 行为均以公开 Android API 为准；不同厂商 ROM 的结果可能显著不同。









## v0.8.0 UI update

- SmartSDR-style panadapter and frequency-locked waterfall.
- Shared blue-cyan-green-yellow-red signal color scale.
- 2dp waterfall rows, aligned time and frequency rulers.
- dBm scale, threshold line, auto-peak marker, and compact TinySA menu.
- Clean build: dist/RF-Scope-v0.8.0-debug.apk.


## v0.9.0 performance and menu update

- Incremental bitmap waterfall rendering for fluid LIVE scanning.
- Compact TinySA-style FREQ / LEVEL / MARKER / MEASURE / CONFIG menu.
- LIVE, SINGLE, TONE modes and TONE REF are available on the main screen.
- Manual dBm display range in the LEVEL tab.
- RBW options now include 600k and 850k.
- Clean build: dist/RF-Scope-v0.9.0-debug.apk.


## v0.9.1 menu layout fixes

- Main controls fit on one screen without horizontal scrolling.
- FREQ input fields have enough height for the full number.
- RBW and VBW/AVG options wrap into aligned rows.
- Menu tabs have a clear active state and divider before content.
- Scale range uses a readable connector and includes 850k RBW.
- Clean build: dist/RF-Scope-v0.9.1-debug.apk.


## v0.9.2 connection diagnostics

- CONNECT requests USB permission automatically when the device is not yet authorized.
- Serial-open success is no longer overwritten by a first-scan failure.
- Connection and scan errors are shown in red with the underlying reason.
- Clean build: dist/RF-Scope-v0.9.2-debug.apk.
