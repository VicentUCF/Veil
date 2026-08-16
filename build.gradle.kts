buildscript {
    configurations.classpath {
        resolutionStrategy.eachDependency {
            val secureVersion = when {
                requested.group == "io.netty" &&
                    requested.name.startsWith("netty-") &&
                    requested.version?.startsWith("4.1.") == true -> "4.1.137.Final"
                requested.group == "org.apache.commons" &&
                    requested.name == "commons-lang3" -> "3.20.0"
                requested.group == "org.apache.httpcomponents" &&
                    requested.name in setOf("httpclient", "httpmime") -> "4.5.14"
                requested.group == "org.bouncycastle" &&
                    requested.name in setOf("bcpkix-jdk18on", "bcutil-jdk18on") -> "1.85"
                requested.group == "org.bouncycastle" &&
                    requested.name == "bcprov-jdk18on" -> "1.85.2"
                requested.group == "org.bitbucket.b_c" &&
                    requested.name == "jose4j" -> "0.9.6"
                requested.group == "org.jdom" &&
                    requested.name == "jdom2" -> "2.0.6.1"
                else -> null
            }
            if (secureVersion != null) {
                useVersion(secureVersion)
                because("Avoid known vulnerabilities in Android build-tool transitive dependencies")
            }
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}
