# Renovate 運用メモ

> このリポジトリは `docs/` がgitignore対象（ローカル専用）のため、運用メモは `.github/` 配下に置いている。

## 構成概要

Renovateを**セルフホスト**（GitHub Actions上でRenovate CLIを実行）で運用する。Mendホスト版を使わないのは、ロックファイル再生成に必要な`postUpgradeTasks`（任意コマンド実行）がセルフホストの`allowedCommands`でのみ許可できるため。

| ファイル | 役割 |
|---|---|
| `.github/workflows/renovate.yml` | 週2回（月・木 06:00 JST）＋手動でRenovate CLIを実行するワークフロー |
| `renovate-config.js` | セルフホスト専用のグローバル設定（対象リポジトリ・`allowedCommands`等） |
| `.github/renovate.json5` | リポジトリ設定（グルーピング・automerge・customManagers・postUpgradeTasks） |

実行は**標準macOSランナー**（`macos-latest`）を使う。Publicリポジトリでは標準ランナーは無料。iOSのSPM依存更新時に`xcodebuild`が必要になるためLinuxではなくmacOSにしている。**larger runner（`*-large` / `*-xlarge`）はPublicでも課金されるため絶対に使わないこと。**

### このリポジトリでの更新対象

| 対象 | マネージャ | 備考 |
|---|---|---|
| `gradle/libs.versions.toml`・`settings.gradle.kts`のプラグイン | `gradle` | Kotlin系・AGPはグループ化（下記） |
| Gradle Wrapper（`gradle-wrapper.properties`＋jar/スクリプト） | `gradle-wrapper` | wrapper再生成は`allowedUnsafeExecutions: ["gradleWrapper"]`で許可済み |
| `sqliteWasmWorker/worker/package.json`（`@sqlite.org/sqlite-wasm`） | `npm` | 更新後に`kotlin-js-store/`のyarn.lock 2本を`./gradlew kotlinUpgradeYarnLock kotlinWasmUpgradeYarnLock`で再生成 |
| `.github/workflows/*.yml`のアクション | `github-actions` | `actions/checkout`等 |
| `iosApp/SharedKit/Package.swift`のSPM依存 | `swift` | **現状、外部SPM依存はゼロ**（SharedKitはローカル統合のみ）。追加された時に効く |
| `renovate.yml`内のRenovate CLI自身 | `custom.regex` | `npx renovate@x.y.z`の行を更新 |
| `*.gradle.kts`内の`npm("name", "version")` | `custom.regex` | 現状該当なし（唯一の`npm()`はローカルdir参照）。将来用 |

## GitHub App設定手順（ユーザー作業・必須）

`GITHUB_TOKEN`ではRenovateが作ったPRに対して他のワークフロー（`ci.yml`）が**発火しない**ため、CI通過を条件とするautomergeが機能しない。そこで**専用のGitHub App**を作り、ワークフロー内で`actions/create-github-app-token`がinstallation tokenを発行してRenovateに渡す。PR・コミットは`<App名>[bot]`名義になる。

1. GitHub → Settings → Developer settings → GitHub Apps → **New GitHub App**
   - **GitHub App name**: 例 `room3poc-renovate`（GitHub全体で一意な名前が必要）
   - **Homepage URL**: リポジトリのURL（必須項目のため）
   - **Webhook**: 「Active」のチェックを**外す**（Webhookは使わない）
   - **Repository permissions**:
     - **Contents**: Read and write（ブランチ作成・コミット）
     - **Pull requests**: Read and write（PR作成・automerge）
     - **Issues**: Read and write（Dependency Dashboard用）
     - **Workflows**: Read and write（`.github/workflows/`内のアクション更新PR用）
     - **Commit statuses**: Read and write（ブランチのステータス読み書き）
     - **Checks**: Read and write（CIのcheck結果の読み取り）
     - **Administration**: Read-only（マージ設定・branch protectionの読み取り）
     - **Dependabot alerts**: Read-only（任意。脆弱性アラート起点の更新PR用）
     - Metadata: Read-only（自動で付与される）
   - **Where can this GitHub App be installed?**: Only on this account
2. 作成後に表示される **App ID** を控える
3. 同ページ下部の Private keys → **Generate a private key**（`.pem`ファイルがダウンロードされる）
4. 左メニューの Install App → 自分のアカウントにインストール →
   **Only select repositories** で `Room3Poc` を選択
5. リポジトリの Settings → Secrets and variables → Actions に登録:
   - **Variables**タブ: `RENOVATE_APP_ID` = 手順2のApp ID
   - **Secrets**タブ: `RENOVATE_APP_PRIVATE_KEY` = `.pem`ファイルの中身全文

補足: `GRADLE_ENCRYPTION_KEY`はci.ymlと共用（未設定でもキャッシュ効率が落ちるだけで動作はする）。

### automergeを機能させるための追加設定（ユーザー作業）

- リポジトリの Settings → General → **Allow auto-merge** を有効化する
- mainブランチのbranch protection（またはruleset）で、CIの各ジョブ
  （`Android` / `Desktop (JVM)` / `Web (JS / WasmJs)` / `iOS`）を
  **required status checks**に設定する。これが無いとCI完了を待たずにマージされ得る

## 実行方法

- **スケジュール**: 日・水 21:00 UTC（月・木 06:00 JST）に自動実行
- **手動**: Actions → Renovate → Run workflow
  - `logLevel: debug` で詳細ログ（どのマネージャが何を検出したか確認できる）
  - `dryRun: true` でPRを作らずログだけ出す（初回の動作確認に使う）
- **設定変更時**: `renovate-config.js` / `.github/renovate.json5` / `renovate.yml` を
  mainにpushすると自動で1回実行される

## 導入後の検証手順

1. 設定ファイルの検証（ローカルでも実行可能）:
   ```bash
   npx --yes --package renovate -- renovate-config-validator renovate-config.js .github/renovate.json5
   ```
2. ローカルdry-run（任意）: リポジトリルートで以下を実行すると、ワーキングツリーの
   設定ファイルを使って依存検出とバージョンルックアップだけを試せる（PR作成・
   postUpgradeTasksは行われない）。`workflow_dispatch`はワークフローがデフォルト
   ブランチに入るまで使えないため、**マージ前の設定確認はこれを使う**:
   ```bash
   GITHUB_COM_TOKEN=$(gh auth token) LOG_LEVEL=debug npx --yes renovate@43.251.3 --platform=local
   ```
3. GitHub App設定後、`workflow_dispatch`（`logLevel: debug`＋`dryRun: true`）で実行し、
   ログで `gradle` / `gradle-wrapper` / `npm` / `github-actions` / `swift` / `custom.regex` の
   各マネージャの依存検出結果を確認する
4. `dryRun: false`で実行し、Dependency DashboardのIssueが作成されることを確認する
5. 動作テスト（任意）: 依存を1つ意図的に古くしてから実行し、以下を確認する
   - `@sqlite.org/sqlite-wasm`を古くする → 更新PRに`kotlin-js-store/yarn.lock`と
     `kotlin-js-store/wasm/yarn.lock`の差分が含まれる
   - `libs.versions.toml`のkotlinを古くする → kotlin/KSP/CMPが単一PR（`kotlin`グループ）に
     まとまり、automergeされない
   - SPMの検証は外部SPM依存を追加してから（現状はゼロのため対象PRが出ない）

## 既知の注意点

- **larger runner禁止**: `runs-on`は標準SKU（`macos-latest`等）のみ。`*-large` / `*-xlarge`はPublicでも課金される。
- **macOSランナーはキュー待ち・低速になりやすい**。Publicは無償なのでコスト実害は無いが、実行時間短縮のため`gradle/actions/setup-gradle`のキャッシュを使っている。
- **GitHub Appのinstallation tokenは発行から1時間で失効する**。トークン発行はRenovate実行の直前に置いてあるが、Renovate本体の実行（postUpgradeTasksのGradle実行込み）が1時間を超えると途中で失敗する。現状の依存規模では十分収まる想定。超えるようになったら、更新対象を絞った複数回実行などの分割を検討する。
- **Compose Compilerの遅延**: 新しいKotlinが出ても対応するCompose Compiler / CMPが揃うまで`kotlin`グループのPRはビルドが赤くなり得る。だからこのグループはautomergeしない。赤いままのPRは対応版が出るまで放置してよい（Renovateが自動でリベース・更新する）。
- **AGPとCMPの結合**: AGPの対応バージョンはCMPプラグイン側の要求（例: AGP 9.0にはCMP 1.9.3 / 1.10.0以降が必要）と結合し得るため、`android-gradle-plugin`グループもautomergeしない。
- **swiftマネージャの限界**: `Package.resolved`はRenovate自身では更新されない（postUpgradeTaskで`xcodebuild -resolvePackageDependencies`により再生成する設定済み）。また**Xcodeプロジェクト直付け（project.pbxproj）のSPM依存はswiftマネージャで検出できない**。外部SPM依存を追加するときは必ず`iosApp/SharedKit/Package.swift`の`dependencies`に書くこと。
- **postUpgradeTasksは`allowedCommands`（renovate-config.js）に一致しないと実行されない**。`.github/renovate.json5`のコマンドを変えたら`renovate-config.js`の正規表現も必ず更新する。
- **Dockerアクションとの非互換**: 公式の`renovatebot/github-action`はLinux Docker前提でmacOSランナーでは動かないため、`npx`でCLIを直接実行している。
- **Xcodeバージョン**: 現状は`macos-latest`のデフォルトXcodeで足りる（外部SPM依存ゼロのため）。SPM依存追加後にresolveが失敗する場合は、renovate.ymlで`sudo xcode-select -s /Applications/Xcode_<version>.app`によるバージョン固定を検討する。

## 代替構成（軽量・高速版）

iOSの`Package.resolved`をコミットしない運用（SPMはビルド時に解決）にするなら、`ubuntu-latest`＋公式`renovatebot/github-action`（Docker）でも運用できる。この場合macOSのキュー待ちが無く高速だが、iOSのロック固定はできない。Webの`yarn.lock`再生成はLinux上の`./gradlew`で可能。現状は将来のSPM依存追加に備えてmacOS構成を採用している。
