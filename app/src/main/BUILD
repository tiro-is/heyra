load("@rules_jvm_external//:defs.bzl", "artifact")
load("@rules_android//android:rules.bzl", "android_library")
load("@io_bazel_rules_kotlin//kotlin:android.bzl", "kt_android_library")

PACKAGE = "is.tiro.heyra"

android_library(
    name = "res",
    custom_package = PACKAGE,
    enable_data_binding = False,
    manifest = "AndroidManifest.xml",
    resource_files = glob(["res/**"]),
    deps = [
        artifact("com.google.android.material:material"),
        artifact("androidx.drawerlayout:drawerlayout"),
        artifact("androidx.annotation:annotation"),
        artifact("androidx.fragment:fragment"),
        artifact("androidx.preference:preference"),
    ],
)

kt_android_library(
    name = "lib",
    srcs = glob(["java/**/*.kt"]),
    deps = [
        ":res",
        "@com_google_protobuf//:protobuf_javalite",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//okhttp",
        "@io_grpc_grpc_java//stub",
        "@tiro_speech_api_v1alpha//:speech_kt_lite_grpc",
        artifact("androidx.appcompat:appcompat"),
        artifact("androidx.activity:activity-ktx"),
        artifact("androidx.preference:preference-ktx"),
        artifact("androidx.fragment:fragment-ktx"),
        artifact("androidx.core:core-ktx"),
        artifact("androidx.lifecycle:lifecycle-runtime"),
        artifact("androidx.lifecycle:lifecycle-viewmodel"),
        artifact("androidx.lifecycle:lifecycle-common"),
        artifact("com.google.android.material:material"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-android"),
        artifact("org.jetbrains.kotlinx:kotlinx-coroutines-core"),
    ],
)

android_binary(
    name = "app",
    custom_package = PACKAGE,
    dex_shards = 10,
    manifest = "AndroidManifest.xml",
    manifest_values = {
        "minSdkVersion": "24",
        "versionCode": "1",
        "versionName": "0.1.alpha1",
        # targetSdkVersion 30 breaks incremental mobile-install
        "targetSdkVersion": "30",
        "compileSdkVersion": "30",
    },
    multidex = "native",
    deps = [
        ":lib",
        ":res",
        artifact("androidx.appcompat:appcompat"),
    ],
)
