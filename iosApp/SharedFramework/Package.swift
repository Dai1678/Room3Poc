// swift-tools-version: 5.9
// sharedモジュールのKotlin/Native成果物（Shared.xcframework）をSPM経由でiosAppに供給する
// ローカルパッケージ。Shared.xcframework自体はGradleが生成する（gitignore対象）:
//   ./gradlew :shared:syncSharedXCFrameworkForSpm
import PackageDescription

let package = Package(
    name: "SharedFramework",
    products: [
        .library(name: "Shared", targets: ["Shared"])
    ],
    targets: [
        .binaryTarget(
            name: "Shared",
            path: "./Shared.xcframework"
        )
    ]
)
