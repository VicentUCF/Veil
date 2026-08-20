fun secureDependencyVersion(group: String?, name: String, version: String?): String? = when {
    group == "io.netty" && name.startsWith("netty-") && version?.startsWith("4.1.") == true ->
        "4.1.137.Final"
    group == "org.apache.commons" && name == "commons-lang3" -> "3.20.0"
    group == "org.apache.httpcomponents" && name in setOf("httpclient", "httpmime") -> "4.5.14"
    group == "org.bouncycastle" && name in setOf("bcpkix-jdk18on", "bcutil-jdk18on") -> "1.85"
    group == "org.bouncycastle" && name == "bcprov-jdk18on" -> "1.85.2"
    group == "org.bitbucket.b_c" && name == "jose4j" -> "0.9.6"
    group == "org.jdom" && name == "jdom2" -> "2.0.6.1"
    else -> null
}

buildscript {
    val secureBuildDependencyVersion: (String?, String, String?) -> String? =
        { group, name, version ->
            when {
                group == "io.netty" && name.startsWith("netty-") &&
                    version?.startsWith("4.1.") == true -> "4.1.137.Final"
                group == "org.apache.commons" && name == "commons-lang3" -> "3.20.0"
                group == "org.apache.httpcomponents" &&
                    name in setOf("httpclient", "httpmime") -> "4.5.14"
                group == "org.bouncycastle" &&
                    name in setOf("bcpkix-jdk18on", "bcutil-jdk18on") -> "1.85"
                group == "org.bouncycastle" && name == "bcprov-jdk18on" -> "1.85.2"
                group == "org.bitbucket.b_c" && name == "jose4j" -> "0.9.6"
                group == "org.jdom" && name == "jdom2" -> "2.0.6.1"
                else -> null
            }
        }
    configurations.classpath {
        resolutionStrategy.eachDependency {
            val secureVersion = secureBuildDependencyVersion(
                requested.group,
                requested.name,
                requested.version,
            )
            if (secureVersion != null) {
                useVersion(secureVersion)
                because("Avoid known vulnerabilities in Android build-tool transitive dependencies")
            }
        }
    }
}

allprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            val secureVersion = secureDependencyVersion(
                requested.group,
                requested.name,
                requested.version,
            )
            if (secureVersion != null) {
                useVersion(secureVersion)
                because("Avoid known vulnerabilities in transitive dependencies")
            }
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}
