import java.io.*;
import java.util.*;

plugins {
    id("org.springframework.boot") version "2.5.6"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.unbroken-dome.test-sets") version "4.0.0"
    id("org.liquibase.gradle") version "2.1.0"
    java
}

group = "com.orangeandbronze.enlistment"
version = "0.0.1-SNAPSHOT"
val testcontainersVersion = "1.16.2"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation( "org.springframework.boot:spring-boot-starter-web")
    implementation( "org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework:spring-aspects")

    implementation("org.apache.commons:commons-lang3")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:localstack:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    runtimeOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")

    liquibaseRuntime("org.liquibase:liquibase-core:4.6.1")
    liquibaseRuntime("info.picocli:picocli:4.6.2")
    liquibaseRuntime("org.liquibase:liquibase-groovy-dsl:3.0.2")
    liquibaseRuntime("org.liquibase.ext:liquibase-hibernate5:4.6.1")
    liquibaseRuntime(sourceSets.getByName("main").compileClasspath)
    liquibaseRuntime(sourceSets.getByName("main").runtimeClasspath)
    liquibaseRuntime(sourceSets.getByName("main").output)
    implementation("net.lbruun.springboot:preliquibase-spring-boot-starter:1.1.1")
}

val prop = Properties().apply {
    load(FileInputStream(File(rootProject.rootDir, "src/main/resources/application.properties")))
}

configurations {
    compileOnly {
        extendsFrom (configurations.annotationProcessor.get())
    }

    liquibase {
        activities.register("main") {
            this.arguments = mapOf(
                    "changeLogFile" to "src/main/resources/db/changelog/db.changelog-master.sql",
                    "url" to prop.getProperty("spring.datasource.url"),
                    "username" to prop.getProperty("spring.datasource.username"),
                    "password" to prop.getProperty("spring.datasource.password"),
                    "driver" to "org.postgresql.Driver",
                    "referenceUrl" to  "hibernate:spring:com.orangeandbronze.enlistment.domain?dialect=org.hibernate.dialect.PostgreSQLDialect&" +
                            "hibernate.physical_naming_strategy=org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy&" +
                            "hibernate.implicit_naming_strategy=org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy"
            )
        }
        runList = "main"
    }
}

testSets {
    libraries {
        create("testCommon")
    }
    create("integrationTest") {
        imports("testCommon")
    }

    val unitTest by getting {
        imports("testCommon")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}