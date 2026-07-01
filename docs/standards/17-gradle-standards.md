# Gradle Standards

## 1. Purpose

These standards ensure consistent, maintainable, and efficient Gradle builds across the ERP SaaS platform. All Gradle configuration must follow these guidelines.

## 2. Gradle Principles

- **Convention over configuration:** Sensible defaults, minimal configuration
- **Performance:** Fast builds with caching and parallelism
- **Reproducibility:** Deterministic builds
- **Maintainability:** Clear, organized build files
- **Security:** Dependency verification, vulnerability scanning

## 3. Project Structure

### 3.1 Multi-Module Structure

```
erp-ai-platform/
├── build.gradle.kts           # Root build file
├── settings.gradle.kts        # Project settings
├── gradle.properties          # Gradle properties
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── business-finance/
│   ├── build.gradle.kts
│   └── src/
├── business-hr/
│   ├── build.gradle.kts
│   └── src/
├── platform-core/
│   ├── build.gradle.kts
│   └── src/
├── libraries-common/
│   ├── build.gradle.kts
│   └── src/
└── ...
```

### 3.2 settings.gradle.kts

```kotlin
rootProject.name = "erp-ai-platform"

// Include modules
include("business-finance")
include("business-hr")
include("business-inventory")
include("business-sales")
include("platform-core")
include("libraries-common")
include("libraries-data")
include("libraries-observability")
include("integration-events")
include("integration-gateway")

// Plugin management
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        val springBootVersion = "3.2.0"
        val springDependencyManagementVersion = "1.1.3"
        
        id("org.springframework.boot") version springBootVersion
        id("io.spring.dependency-management") version springDependencyManagementVersion
        id("org.jetbrains.kotlin.jvm") version "1.9.20"
        id("com.diffplug.spotless") version "6.23.3"
        id("com.github.spotbugs") version "5.0.15"
        id("jacoco") version "0.8.10"
    }
}
```

## 4. Root build.gradle.kts

```kotlin
plugins {
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.3" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.20" apply false
    id("com.diffplug.spotless") version "6.23.3" apply false
    id("com.github.spotbugs") version "5.0.15" apply false
    id("jacoco") apply false
}

allprojects {
    group = "com.erp.platform"
    version = "1.0.0-SNAPSHOT"
    
    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "com.github.spotbugs")
    
    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    dependencies {
        // Common dependencies
        implementation("org.springframework:spring-context")
        implementation("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.assertj:assertj-core")
        testImplementation("org.mockito:mockito-core")
        testImplementation("org.testcontainers:junit-jupiter")
        testImplementation("org.testcontainers:postgresql")
    }
    
    // Spotless configuration
    spotless {
        java {
            googleJavaFormat("1.19.0").aosp()
            importOrder()
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlin {
            ktfmt("0.50.0").kotlinLangStyle()
        }
        format("xml") {
            target("**/*.xml", "**/*.yaml", "**/*.yml")
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
    
    // SpotBugs configuration
    spotbugs {
        toolVersion = "4.8.3"
        effort = "max"
        reportLevel = "low"
        excludeFilter = file("config/spotbugs/exclude.xml")
    }
    
    tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
        reports {
            create("html") {
                required.set(true)
                outputLocation.set(file("$buildDir/reports/spotbugs/spotbugs.html"))
            }
        }
    }
    
    // JaCoCo configuration
    jacoco {
        toolVersion = "0.8.10"
    }
    
    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
    
    tasks.jacocoTestCoverageVerification {
        violationRules {
            rule {
                limit {
                    minimum = "0.80".toBigDecimal()
                }
            }
        }
    }
    
    tasks.check {
        dependsOn(tasks.jacocoTestCoverageVerification)
    }
}
```

## 5. Module build.gradle.kts

### 5.1 Application Module

```kotlin
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(project(":platform-core"))
    implementation(project(":libraries-common"))
    implementation(project(":libraries-data"))
    implementation(project(":integration-events"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Database
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    
    // Messaging
    implementation("org.springframework.kafka:spring-kafka")
    
    // Utilities
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")
    implementation("io.minio:minio:8.5.7")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("io.rest-assured:rest-assured")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    archiveFileName.set("${project.name}.jar")
    manifest {
        attributes["Main-Class"] = "com.erp.platform.business.finance.FinanceApplication"
    }
}
```

### 5.2 Library Module

```kotlin
plugins {
    id("java-library")
    id("io.spring.dependency-management")
}

dependencies {
    api(project(":platform-core"))
    
    // Public API
    api("org.springframework:spring-context")
    api("org.springframework:spring-tx")
    
    // Internal
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.apache.kafka:kafka-clients")
    
    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

## 6. Dependency Management

### 6.1 Dependency Versions

```kotlin
// In root build.gradle.kts or separate versions.toml
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("springBoot", "3.2.0")
            version("springDependencyManagement", "1.1.3")
            version("kotlin", "1.9.20")
            version("mapstruct", "1.5.5.Final")
            version("postgresql", "42.6.0")
            version("flyway", "10.8.0")
            version("kafka", "3.5.1")
            version("minio", "8.5.7")
            version("junit", "5.10.1")
            version("assertj", "3.24.2")
            version("mockito", "5.7.0")
            version("testcontainers", "1.19.3")
            
            library("spring-boot-starter-web", "org.springframework.boot", "spring-boot-starter-web").versionRef("springBoot")
            library("spring-boot-starter-data-jpa", "org.springframework.boot", "spring-boot-starter-data-jpa").versionRef("springBoot")
            library("postgresql", "org.postgresql", "postgresql").versionRef("postgresql")
            library("flyway-core", "org.flywaydb", "flyway-core").versionRef("flyway")
            library("kafka-clients", "org.apache.kafka", "kafka-clients").versionRef("kafka")
            library("minio", "io.minio", "minio").versionRef("minio")
            library("junit-jupiter", "org.junit.jupiter", "junit-jupiter").versionRef("junit")
            library("assertj-core", "org.assertj", "assertj-core").versionRef("assertj")
            library("mockito-core", "org.mockito", "mockito-core").versionRef("mockito")
            library("testcontainers-junit", "org.testcontainers", "junit-jupiter").versionRef("testcontainers")
        }
    }
}
```

### 6.2 Using Version Catalog

```kotlin
dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.postgresql)
    implementation(libs.flyway.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}
```

## 7. Build Optimization

### 7.1 Build Cache

```kotlin
tasks.withType<JavaCompile> {
    options.isFork = true
    options.forkOptions.jvmArgs!!.addAll(listOf("-XX:+UseParallelGC"))
}

tasks.withType<Test> {
    maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
}

// Configure build cache
tasks.register("enableBuildCache") {
    doLast {
        gradle.startParameter.buildCacheEnabled = true
    }
}
```

### 7.2 Parallel Execution

```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.workers.max=4
org.gradle.caching=true
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx2g -XX:+UseParallelGC
```

### 7.3 Configuration Cache

```properties
# gradle.properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn
```

## 8. Testing Configuration

### 8.1 Test Tasks

```kotlin
tasks.test {
    useJUnitPlatform()
    
    maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
    
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
    }
    
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
    
    systemProperty("spring.profiles.active", "test")
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    
    useJUnitPlatform {
        includeTags("integration")
    }
    
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(tasks.integrationTest)
}
```

### 8.2 Test Containers

```kotlin
dependencies {
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:redis")
}

tasks.test {
    systemProperty("testcontainers.reuse.enable", "true")
}
```

## 9. Code Quality

### 9.1 Spotless

```kotlin
spotless {
    java {
        googleJavaFormat("1.19.0").aosp()
        importOrder()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        target("src/**/*.java")
    }
    kotlin {
        ktfmt("0.50.0").kotlinLangStyle()
        target("src/**/*.kt")
    }
    format("markdown") {
        target("*.md", "**/*.md")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.spotlessCheck {
    description = "Check code formatting."
    group = "verification"
}

tasks.spotlessApply {
    description = "Apply code formatting."
    group = "formatting"
}
```

### 9.2 SpotBugs

```kotlin
spotbugs {
    toolVersion = "4.8.3"
    effort = "max"
    reportLevel = "low"
    excludeFilter = file("config/spotbugs/exclude.xml")
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports {
        create("html") {
            required.set(true)
            outputLocation.set(file("$buildDir/reports/spotbugs/spotbugs.html"))
        }
        create("xml") {
            required.set(true)
            outputLocation.set(file("$buildDir/reports/spotbugs/spotbugs.xml"))
        }
    }
}
```

### 9.3 Checkstyle

```kotlin
dependencies {
    checkstyle("com.puppycrawl.tools:checkstyle:10.12.5")
}

tasks.register<com.puppycrawl.tools.checkstyle.gradle.CheckstyleTask>("checkstyle") {
    configFile = file("config/checkstyle/checkstyle.xml")
    source = fileTree("src")
    include("**/*.java")
    exclude("**/test/**")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.check {
    dependsOn("checkstyle")
}
```

## 10. Dependency Management

### 10.1 Dependency Updates

```kotlin
plugins {
    id("com.github.ben-manes.versions") version "0.48.0"
}

tasks.register<com.github.benmanes.gradle.versions.updates.UpdateTask>("dependencyUpdates") {
    checkForGradleUpdate = true
    outputFormatter = "json"
    reportfileName = "dependency-updates-report"
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
```

### 10.2 Dependency Locking

```properties
# gradle.properties
dependencyLocking.enabled=true
dependencyLocking.lockMode=strict
```

### 10.3 Dependency Verification

```properties
# gradle.properties
dependencyVerification=strict
```

## 11. Publishing

### 11.1 Maven Publish

```kotlin
plugins {
    id("maven-publish")
    id("signing")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            
            pom {
                name.set("ERP Platform Common")
                description.set("Common library for ERP Platform")
                url.set("https://github.com/company/erp-platform")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("company")
                        name.set("Company")
                        email.set("dev@company.com")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/company/erp-platform.git")
                    developerConnection.set("scm:git:ssh://github.com/company/erp-platform.git")
                    url.set("https://github.com/company/erp-platform")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/company/erp-platform")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

## 12. Gradle Wrapper

### 12.1 Wrapper Configuration

```properties
# gradle/wrapper/gradle-wrapper.properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### 12.2 Wrapper Update

```bash
# Update wrapper
./gradlew wrapper --gradle-version 8.5 --distribution-type bin

# Commit wrapper files
git add gradle/wrapper/gradle-wrapper.properties gradlew gradlew.bat
```

## 13. Gradle Checklist

- [ ] Multi-module structure used
- [ ] Version catalog for dependencies
- [ ] Spotless configured for formatting
- [ ] SpotBugs configured for static analysis
- [ ] JaCoCo configured with 80% coverage threshold
- [ ] Build cache enabled
- [ ] Parallel execution enabled
- [ ] Dependency updates automated
- [ ] Dependency verification enabled
- [ ] Wrapper committed to repository
