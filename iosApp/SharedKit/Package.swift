// swift-tools-version: 5.9
// Kotlin framework（Shared）をローカルSwiftパッケージから利用する、公式のLocal SPM統合:
// https://kotlinlang.org/docs/multiplatform/multiplatform-spm-local-integration.html
// Shared.framework自体はschemeのBuild Pre-actionが
// `./gradlew :shared:embedAndSignAppleFrameworkForXcode` でビルド・埋め込みする
// （binaryTargetは使わない。binaryTargetは公式ではRemote配布用の仕組み）。
import PackageDescription

let package = Package(
    name: "SharedKit",
    platforms: [
        .iOS("18.2")
    ],
    products: [
        .library(name: "SharedKit", targets: ["SharedKit"])
    ],
    targets: [
        .target(
            name: "SharedKit",
            linkerSettings: [
                .linkedFramework("Shared")
            ]
        )
    ]
)
