import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    compilerOptions {
        // @ConstructedByのexpect object（と生成actual）に対するBeta警告(KT-61573)を抑制。
        // Room 3.0自体がこの仕組みの上に成立しているため、本PoCでは意図的に使用している
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // iosAppは公式のLocal SPM統合で取り込む: iosAppスキームのBuild Pre-actionが
    // :shared:embedAndSignAppleFrameworkForXcode を実行し、ローカルSwiftパッケージ
    // （iosApp/SharedKit）のSwiftコードがこのframeworkをimportする
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    jvm()
    
    js {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    androidLibrary {
       namespace = "dev.dai.room3poc.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.sqlite.bundled)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.room.runtime)
        }
        jvmMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
        // BundledSQLiteDriverならframeworkへのlinkerOpts追加は不要
        // （NativeSQLiteDriverを使う場合のみ -lsqlite3 が必要）
        iosMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
        // js/wasmJs共有の中間ソースセット。Web用ドライバーはここに置く
        webMain.dependencies {
            implementation(libs.androidx.sqlite.web)
            implementation(projects.sqliteWasmWorker)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)

    // Room 3.0 compiler (KSP) — ターゲットごとに登録が必要
    // 設定名の確認: ./gradlew :shared:tasks --all | grep -i ksp
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
    // Web対応(Phase 3)で使用。先に登録しても害はない
    add("kspJs", libs.androidx.room.compiler)
    add("kspWasmJs", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

room3 {
    schemaDirectory(layout.projectDirectory.dir("schemas"))
}