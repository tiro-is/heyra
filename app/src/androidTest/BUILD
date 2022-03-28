load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_android_library")
load(
    "@rules_android//android:rules.bzl",
    "android_instrumentation_test"
)
load("@rules_jvm_external//:defs.bzl", "artifact")

kt_android_library(
    name = "example_test_lib",
    srcs = glob(["java/**/*.kt"]),
    deps = [
        "//app/src/main:lib",
        artifact("androidx.test.ext:junit"),
        artifact("androidx.test.espresso:espresso-core"),
        artifact("junit:junit"),
    ],
)

android_instrumentation_test(
    name = "example_uiinstrumentation_test",
    test_app = ":example_test_app",
    target_device = "@android_test_support//tools/android/emulated_devices/generic_phone:android_30_x86",
)

