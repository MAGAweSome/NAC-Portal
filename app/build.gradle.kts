import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

val localProps = Properties().apply {
  val localPropsFile = rootProject.file("local.properties")
  if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { load(it) }
  }
}

fun resolveSecretOrProp(envKey: String, propKey: String, defaultValue: String = ""): String {
  return System.getenv(envKey)?.takeIf { it.isNotBlank() }
    ?: localProps.getProperty(propKey)?.takeIf { it.isNotBlank() }
    ?: defaultValue
}

android {
  namespace = "com.example.parishsync"
  compileSdk = 37
  defaultConfig {
    applicationId = "com.example.parishsync"
    minSdk = 26
    targetSdk = 37
    versionCode = 2
    versionName = "2.0"

    val mailtrapToken = resolveSecretOrProp("MAILTRAP_API_TOKEN", "mailtrap.api.token", "")
    val mailtrapSenderEmail = resolveSecretOrProp("MAILTRAP_SENDER_EMAIL", "mailtrap.sender.email", "info@NACPortal.com")
    val mailtrapSenderName = resolveSecretOrProp("MAILTRAP_SENDER_NAME", "mailtrap.sender.name", "NAC Portal")
    val mailtrapDefaultRecipient = resolveSecretOrProp("MAILTRAP_DEFAULT_RECIPIENT", "mailtrap.default.recipient", "")
    val mailtrapSmtpHost = resolveSecretOrProp("MAILTRAP_SMTP_HOST", "mailtrap.smtp.host", "sandbox.smtp.mailtrap.io")
    val mailtrapSmtpPort = resolveSecretOrProp("MAILTRAP_SMTP_PORT", "mailtrap.smtp.port", "2525")
    val mailtrapSmtpUsername = resolveSecretOrProp("MAILTRAP_SMTP_USERNAME", "mailtrap.smtp.username", "")
    val mailtrapSmtpPassword = resolveSecretOrProp("MAILTRAP_SMTP_PASSWORD", "mailtrap.smtp.password", "")
    val supabaseUrl = resolveSecretOrProp("SUPABASE_URL", "supabase.url", "")
    val supabaseAnonKey = resolveSecretOrProp("SUPABASE_ANON_KEY", "supabase.anon.key", "")
    val nacUsername = resolveSecretOrProp("NAC_USERNAME", "nac.username", "").replace("\\", "\\\\").replace("\"", "\\\"")
    val nacPassword = resolveSecretOrProp("NAC_PASSWORD", "nac.password", "").replace("\\", "\\\\").replace("\"", "\\\"")

    buildConfigField("String", "MAILTRAP_API_TOKEN", "\"$mailtrapToken\"")
    buildConfigField("String", "MAILTRAP_SENDER_EMAIL", "\"$mailtrapSenderEmail\"")
    buildConfigField("String", "MAILTRAP_SENDER_NAME", "\"$mailtrapSenderName\"")
    buildConfigField("String", "MAILTRAP_DEFAULT_RECIPIENT", "\"$mailtrapDefaultRecipient\"")
    buildConfigField("String", "MAILTRAP_SMTP_HOST", "\"$mailtrapSmtpHost\"")
    buildConfigField("String", "MAILTRAP_SMTP_PORT", "\"$mailtrapSmtpPort\"")
    buildConfigField("String", "MAILTRAP_SMTP_USERNAME", "\"$mailtrapSmtpUsername\"")
    buildConfigField("String", "MAILTRAP_SMTP_PASSWORD", "\"$mailtrapSmtpPassword\"")
    buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
    buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    buildConfigField("String", "NAC_USERNAME", "\"$nacUsername\"")
    buildConfigField("String", "NAC_PASSWORD", "\"$nacPassword\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = true
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose & Material 3
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)

  // Room Local Database (SQLite Offline-First)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // WorkManager Background Processing (Sync & PDF Schedule Parsing)
  implementation(libs.androidx.work.runtime.ktx)

  // Kotlinx Serialization (Supabase REST & Mailtrap payloads)
  implementation(libs.kotlinx.serialization.json)

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestImplementation("androidx.test:rules:1.6.1")

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
