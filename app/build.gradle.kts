import java.net.URI
import javax.net.ssl.HttpsURLConnection

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

fun isPublicHttpsUrl(value: String): Boolean = runCatching {
    val uri = URI(value)
    val host = uri.host?.lowercase().orEmpty()
    uri.isAbsolute &&
        uri.scheme.equals("https", ignoreCase = true) &&
        host.isNotBlank() &&
        uri.rawUserInfo == null &&
        uri.port in setOf(-1, 443) &&
        host != "localhost" &&
        !host.endsWith(".localhost") &&
        !host.endsWith(".invalid")
}.getOrDefault(false)

fun isPrivacyContactEmail(value: String): Boolean =
    value.length in 3..254 &&
        Regex("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
            .matches(value)

fun requirePublishedPrivacyPolicy(value: String) {
    val connection = URI(value).toURL().openConnection() as HttpsURLConnection
    connection.connectTimeout = 5_000
    connection.readTimeout = 5_000
    connection.instanceFollowRedirects = false
    connection.requestMethod = "GET"
    connection.setRequestProperty("Range", "bytes=0-1023")
    connection.setRequestProperty("Accept", "text/html,text/plain")
    connection.setRequestProperty("User-Agent", "Veil release verification")
    try {
        check(connection.responseCode in 200..299) {
            "The privacy-policy URL is not publicly reachable (HTTP ${connection.responseCode})."
        }
        check(connection.contentType?.substringBefore(';')?.trim() in setOf("text/html", "text/plain")) {
            "The privacy-policy URL must serve text/html or text/plain content."
        }
        connection.inputStream.close()
    } finally {
        connection.disconnect()
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val releaseStoreFile = providers.environmentVariable("VEIL_UPLOAD_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("VEIL_UPLOAD_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("VEIL_UPLOAD_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("VEIL_UPLOAD_KEY_PASSWORD").orNull
val privacyPolicyUrl = providers.environmentVariable("VEIL_PRIVACY_POLICY_URL").orNull
val privacyContact = providers.environmentVariable("VEIL_PRIVACY_CONTACT").orNull
val releaseSigningReady = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "dev.vicent.veil"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.vicent.veil"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "PRIVACY_POLICY_URL",
            privacyPolicyUrl.orEmpty().asBuildConfigString(),
        )
        buildConfigField(
            "String",
            "PRIVACY_CONTACT",
            privacyContact.orEmpty().asBuildConfigString(),
        )
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(checkNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val verifyProductionRelease = tasks.register("verifyProductionRelease") {
    group = "verification"
    description = "Rejects a production artifact without signing and privacy publication data."
    doLast {
        check(releaseSigningReady) {
            "Release signing is required. Set VEIL_UPLOAD_STORE_FILE, " +
                "VEIL_UPLOAD_STORE_PASSWORD, VEIL_UPLOAD_KEY_ALIAS and VEIL_UPLOAD_KEY_PASSWORD."
        }
        check(isPublicHttpsUrl(privacyPolicyUrl.orEmpty())) {
            "Set VEIL_PRIVACY_POLICY_URL to a public HTTPS privacy-policy URL; " +
                "localhost and placeholder .invalid hosts are rejected."
        }
        check(isPrivacyContactEmail(privacyContact.orEmpty())) {
            "Set VEIL_PRIVACY_CONTACT to the publisher's monitored email address."
        }
        check(file(checkNotNull(releaseStoreFile)).isFile) {
            "VEIL_UPLOAD_STORE_FILE does not point to a readable keystore file."
        }
        requirePublishedPrivacyPolicy(checkNotNull(privacyPolicyUrl))
    }
}

tasks.configureEach {
    if (name == "packageRelease" || name == "signReleaseBundle") {
        dependsOn(verifyProductionRelease)
    }
}

dependencyLocking {
    lockAllConfigurations()
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
