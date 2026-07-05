# Room3Poc

Room 3.0のKotlin Multiplatform対応を検証するPoCプロジェクト。
`commonMain`に1回だけ書いた同一のDAOが Android / iOS / Desktop(JVM) / Web(WasmJs) で動作し、Webではブラウザをリロードしてもデータが残る（OPFS永続化）ことを確認する。

アプリは画面1つ・Entity1つの最小構成のCatリスト。名前を入力して追加すると、`Flow`を購読している一覧に即座に反映され、アプリを再起動（Webはリロード）してもデータが残る。

## 検証状況

| ターゲット | 状態 | 永続化先 |
|---|---|---|
| Desktop (JVM) | ✅ 動作確認済み | `$TMPDIR/cats.db`（`BundledSQLiteDriver`） |
| Android | ✅ 動作確認済み（API 36エミュレータ） | `getDatabasePath("cats.db")`（`BundledSQLiteDriver`） |
| Web (WasmJs) | ✅ 動作確認済み・リロード後もデータ残存 | OPFS上の`cats.db`（`WebWorkerSQLiteDriver`） |
| Web (JS) | ビルド確認のみ（WasmJs不調時のフォールバック） | 同上 |
| iOS | ✅ 動作確認済み（iPhone 17 Pro / iOS 26.1シミュレータ・再起動後もデータ残存） | `Documents/cats.db`（`BundledSQLiteDriver`） |

## アーキテクチャ

```
shared/commonMain
  ├── @Entity Cat / @Dao CatDao (suspend + Flow) / @Database CatDatabase
  └── expect fun databaseBuilder(): RoomDatabase.Builder<CatDatabase>
        │
        ├── jvmMain:     actual → BundledSQLiteDriver → ローカルファイル
        ├── androidMain: actual → BundledSQLiteDriver → アプリ内DBファイル
        ├── iosMain:     actual → BundledSQLiteDriver → Documents/cats.db
        └── webMain:     actual → WebWorkerSQLiteDriver（js/wasmJs共通）
                                        │ postMessage (open/prepare/step/close)
                                        ▼
                              sqliteWasmWorker/worker/worker.js
                                        │ @sqlite.org/sqlite-wasm
                                        ▼
                              SQLite WASM → OPFS（ブラウザ内永続化）
```

DAO・Entity・クエリは`commonMain`に1回しか書かない。プラットフォームごとに違うのは`databaseBuilder()`の`actual`（ドライバーの差し替え）だけ。

## モジュール構成

- `shared/` — Entity・DAO・DB定義（commonMain）と各プラットフォームの`actual`、共通Compose UI
- `androidApp/` / `desktopApp/` / `webApp/` — 各プラットフォームのエントリポイント
- `iosApp/` — iOSエントリポイント（SwiftUIから`ComposeUIViewController`を表示）。sharedの成果物は**SPM（ローカルSwiftパッケージ）経由**で取り込む
  - `iosApp/SharedFramework/` — `Shared.xcframework`を`binaryTarget`で参照するローカルパッケージ。XCFramework本体はGradleが生成する（gitignore対象）
- `sqliteWasmWorker/` — Web用Workerモジュール。`WebWorkerSQLiteDriver`のメッセージングプロトコル（open/prepare/step/close）を実装した`worker.js`と、`@sqlite.org/sqlite-wasm`へのnpm依存を持つ。
  - [danysantiago/room-web-demo](https://github.com/danysantiago/room-web-demo/)（Apache-2.0）からの移植

## 実行方法

```bash
# Desktop（最速で動作確認できる）
./gradlew :desktopApp:run

# Android（ビルドしてadbでインストール、またはIDEのRun Configurationから）
./gradlew :androidApp:assembleDebug

# Web (WasmJs) — devServerが http://localhost:8080 で起動する
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# Web (JS) — フォールバック用
./gradlew :webApp:jsBrowserDevelopmentRun

# iOS — 先にXCFrameworkを生成してから、XcodeでiosApp.xcodeprojを開いてRun
./gradlew :shared:syncSharedXCFrameworkForSpm
open iosApp/iosApp.xcodeproj
```

### iOSターゲットの注意点（SPM統合）

- sharedのKotlin成果物（`Shared.xcframework`）は、ローカルSwiftパッケージ`iosApp/SharedFramework`の`binaryTarget`としてiosAppに取り込まれる
- **sharedのKotlinコードを変更したら`./gradlew :shared:syncSharedXCFrameworkForSpm`を再実行**してからXcodeでビルドする（Xcodeビルドは自動でGradleを呼ばない）
- クローン直後はXCFrameworkが存在せずXcodeのパッケージ解決に失敗するため、必ず先に上記タスクを実行する
- コマンドラインでビルドする場合:

```bash
xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  CODE_SIGNING_ALLOWED=NO
```

### Webターゲットの注意点

- **COOP/COEPヘッダ**: OPFSの高速アクセス（SharedArrayBuffer）に必要なcross-origin isolationを、`webApp/webpack.config.d/webpack.config.js`でdevServerに設定済み。DevTools Consoleで`crossOriginIsolated`が`true`を返せば有効
- **npm依存を変更した場合**: 初回ビルドがlockfile不一致で失敗したら `./gradlew kotlinWasmUpgradeYarnLock kotlinUpgradeYarnLock` を実行してから再ビルドする
- worker.jsのメッセージログ（`handleMessage: {...}`）がDevTools Consoleに流れるので、Room→Worker→SQLite WASMのやり取りを観察できる

## テスト

```bash
# DAOの挿入・Flow購読・再オープン後のデータ残存を検証するJVMテスト
./gradlew :shared:jvmTest --tests "dev.dai.room3poc.db.CatDaoJvmTest"

# 同内容のiOSテスト（シミュレータをヘッドレス起動して実行）
./gradlew :shared:iosSimulatorArm64Test
```

## Room 3.0まわりのメモ

- Roomのimportはすべて`androidx.room3.*`（2.xの`androidx.room.*`と混ぜない）
- DAOのブロッキング関数は、非Androidプラットフォームを対象とするソースセットではコンパイルエラーになる（suspendまたはFlow等が必須）
- `@ConstructedBy`の`actual`はKSPが各ターゲット向けに自動生成する（`shared/build/generated/ksp/<target>/`）
- スキーマは`room3 { schemaDirectory }`の設定により`shared/schemas/`にexportされる
