// Renovateセルフホスト実行用のグローバル設定。
// .github/workflows/renovate.yml が RENOVATE_CONFIG_FILE としてこのファイルを読み込む。
// 「何をどう更新するか」のリポジトリ設定は .github/renovate.json5 側にある。
// 認証はGitHub Appのinstallation token（RENOVATE_TOKEN）で行い、
// username / gitAuthor もワークフロー側が環境変数
// （RENOVATE_USERNAME / RENOVATE_GIT_AUTHOR）で注入する。
module.exports = {
  platform: "github",
  // 対象リポジトリを明示する（autodiscoverは使わない）
  repositories: ["Dai1678/Room3Poc"],

  // リポジトリ設定（.github/renovate.json5）をコミット済みのためonboarding PRは不要
  onboarding: false,
  requireConfig: "required",

  // java / node / xcodebuild はランナーにプリインストールされたものを直接使う。
  // macOSランナーにはDockerが無いためsidecar系（docker / install）は使わない
  binarySource: "global",

  // postUpgradeTasksで実行を許可するコマンド（正規表現）。
  // ここに一致しないコマンドはRenovateが実行を拒否する。
  // .github/renovate.json5 側のコマンドを変えたら必ずここも更新すること
  allowedCommands: [
    "^\\./gradlew kotlinUpgradeYarnLock kotlinWasmUpgradeYarnLock$",
    "^xcodebuild -resolvePackageDependencies -project iosApp/iosApp\\.xcodeproj -scheme iosApp$",
  ],

  // gradle-wrapperマネージャが `./gradlew wrapper` を実行して
  // wrapper一式（properties / jar / スクリプト / checksum）を更新することを許可する
  allowedUnsafeExecutions: ["gradleWrapper"],
};
