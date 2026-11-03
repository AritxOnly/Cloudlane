plugins { id("com.android.application"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace = "com.aritxonly.cloudlane"
 compileSdk = 37
 defaultConfig { applicationId = "com.aritxonly.cloudlane"; minSdk = 33; targetSdk = 36; versionCode = 1; versionName = "0.1.0" }
 sourceSets.getByName("main").kotlin.srcDir(rootProject.file("third_party/miuix-glass/src/commonMain/kotlin"))
 compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
 buildFeatures { compose = true }
 packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.08.00"))
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.compose.material3:material3:1.5.0-alpha08")
 implementation("androidx.work:work-runtime-ktx:2.10.3")
 implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
 implementation("com.materialkolor:material-kolor:4.1.1")
 implementation("io.github.kyant0:shapes:1.2.0")
 implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4-rc01")
 implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.4-rc01")
 implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.4-rc01")
 testImplementation("junit:junit:4.13.2")
 debugImplementation("androidx.compose.ui:ui-tooling")
}
