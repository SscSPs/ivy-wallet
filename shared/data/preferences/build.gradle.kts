plugins {
    id("ivy.module")
}

android {
    namespace = "com.ivy.data.preferences"
}

dependencies {
    implementation(projects.shared.base)
    implementation(libs.datastore)
    implementation(libs.kotlin.coroutines.core)
}
