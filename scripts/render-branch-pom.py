#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
按分支名重写 opencli-java-sdk 的 pom.xml，对齐 dreamina-java-sdk 的多版本 / 多 JDK 策略。

JDK 基线:
  2.3.x -> JDK 8（Unirest + Java-WebSocket，不依赖 java.net.http）
  2.7.x -> JDK 11
  3.0.x-3.4.x -> JDK 17
  3.5.x / 4.0.x -> JDK 21

发布: 各分支 pom 均包含阿里云 Packages distributionManagement。

用法:
  python3 scripts/render-branch-pom.py <branch>
"""
from __future__ import annotations

import os
import pathlib
import sys
from datetime import date

ROOT = pathlib.Path(__file__).resolve().parents[1]
POM = ROOT / "pom.xml"

def version_date_suffix() -> str:
    """SNAPSHOT: {date}-SNAPSHOT；RELEASE(RELEASE=1): 仅 {date}。"""
    raw = os.environ.get("RELEASE_DATE", "").strip()
    day = raw if raw else date.today().strftime("%Y%m%d")
    if os.environ.get("RELEASE", "").strip().lower() in ("1", "true", "yes"):
        return day
    return f"{day}-SNAPSHOT"


VERSION_DATE_SUFFIX = version_date_suffix()

ALIYUN_DM = """
    <distributionManagement>
        <repository>
            <id>2624322-release-6F6h6R</id>
            <url>https://packages.aliyun.com/6927b116e6c3e0425dbdf60d/maven/2624322-release-6f6h6r</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
        <snapshotRepository>
            <id>2624322-snapshot-3EoOv3</id>
            <url>https://packages.aliyun.com/6927b116e6c3e0425dbdf60d/maven/2624322-snapshot-3eoov3</url>
            <releases>
                <enabled>false</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
            <uniqueVersion>true</uniqueVersion>
        </snapshotRepository>
    </distributionManagement>
"""

COMMON_META = """    <licenses>
        <license>
            <name>The Apache Software License, Version 2.0</name>
            <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
        </license>
    </licenses>

    <scm>
        <connection>scm:git:https://github.com/hiwepy/${project.artifactId}.git</connection>
        <developerConnection>scm:git:https://github.com/hiwepy/${project.artifactId}.git</developerConnection>
        <url>https://github.com/hiwepy/${project.artifactId}</url>
        <tag>${project.artifactId}</tag>
    </scm>

    <developers>
        <developer>
            <name>hiwepy</name>
            <email>hiwepy@gmail.com</email>
            <roles>
                <role>developer</role>
            </roles>
            <timezone>+8</timezone>
        </developer>
    </developers>
"""

UNIREST_WS_DEPS = """
        <dependency>
            <groupId>com.konghq</groupId>
            <artifactId>unirest-java</artifactId>
            <version>${unirest.version}</version>
        </dependency>
        <dependency>
            <groupId>org.java-websocket</groupId>
            <artifactId>Java-WebSocket</artifactId>
            <version>${java-websocket.version}</version>
        </dependency>
"""

DEPS_HEAD = """
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-exec</artifactId>
            <version>${commons-exec.version}</version>
        </dependency>
"""

DEPS_TAIL = """
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${slf4j.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>${slf4j.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
"""


def compiler_block(*, use_release: bool) -> str:
    if use_release:
        return """            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven-compiler-plugin.version}</version>
                <configuration>
                    <release>${maven.compiler.release}</release>
                    <encoding>${project.build.sourceEncoding}</encoding>
                </configuration>
            </plugin>"""
    return """            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven-compiler-plugin.version}</version>
                <configuration>
                    <source>${maven.compiler.source}</source>
                    <target>${maven.compiler.target}</target>
                    <encoding>${project.build.sourceEncoding}</encoding>
                </configuration>
            </plugin>"""


def properties_block(
    *,
    java_version: str,
    java_release: str,
    use_release: bool,
    jackson: str,
    junit: str,
    slf4j: str,
    lombok: str,
    unirest: str,
    java_websocket: str,
    maven_enforcer: str,
    maven_compiler: str,
    maven_surefire: str,
    maven_source: str,
    maven_jar: str,
) -> str:
    release_lines = ""
    if use_release:
        release_lines = f"""
        <maven.compiler.release>{java_release}</maven.compiler.release>
        <maven.compiler.source>{java_release}</maven.compiler.source>
        <maven.compiler.target>{java_release}</maven.compiler.target>"""
    else:
        release_lines = f"""
        <maven.compiler.source>{java_release}</maven.compiler.source>
        <maven.compiler.target>{java_release}</maven.compiler.target>"""
    enforcer_ver = f"[{java_release}.0,)" if use_release else f"[{java_version},)"
    return f"""    <properties>
        <java.version>{java_version}</java.version>{release_lines}
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <jackson.version>{jackson}</jackson.version>
        <commons-exec.version>1.4.0</commons-exec.version>
        <unirest.version>{unirest}</unirest.version>
        <java-websocket.version>{java_websocket}</java-websocket.version>
        <junit.version>{junit}</junit.version>
        <slf4j.version>{slf4j}</slf4j.version>
        <lombok.version>{lombok}</lombok.version>
        <maven.version>3.6</maven.version>
        <maven-compiler-plugin.version>{maven_compiler}</maven-compiler-plugin.version>
        <maven-enforcer-plugin.version>{maven_enforcer}</maven-enforcer-plugin.version>
        <maven-surefire-plugin.version>{maven_surefire}</maven-surefire-plugin.version>
        <maven-source-plugin.version>{maven_source}</maven-source-plugin.version>
        <maven-jar-plugin.version>{maven_jar}</maven-jar-plugin.version>
        <maven-enforcer-java-version>{enforcer_ver}</maven-enforcer-java-version>
    </properties>"""


def write_pom(
    *,
    version: str,
    java_version: str,
    java_release: str,
    use_release: bool,
    description: str,
    jackson: str,
    junit: str,
    slf4j: str,
    lombok: str,
    unirest: str,
    java_websocket: str,
    maven_enforcer: str,
    maven_compiler: str,
    maven_surefire: str,
    maven_source: str,
    maven_jar: str,
) -> None:
    props = properties_block(
        java_version=java_version,
        java_release=java_release,
        use_release=use_release,
        jackson=jackson,
        junit=junit,
        slf4j=slf4j,
        lombok=lombok,
        unirest=unirest,
        java_websocket=java_websocket,
        maven_enforcer=maven_enforcer,
        maven_compiler=maven_compiler,
        maven_surefire=maven_surefire,
        maven_source=maven_source,
        maven_jar=maven_jar,
    )
    comp = compiler_block(use_release=use_release)
    body = f'''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.hiwepy</groupId>
    <artifactId>opencli-java-sdk</artifactId>
    <version>{version}</version>
    <packaging>jar</packaging>
    <name>${{project.groupId}}:${{project.artifactId}}</name>
    <description>{description}</description>
    <url>https://github.com/hiwepy/${{project.artifactId}}</url>

{COMMON_META}
{ALIYUN_DM}
{props}

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-enforcer-plugin</artifactId>
                <version>${{maven-enforcer-plugin.version}}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>enforce</goal>
                        </goals>
                        <phase>validate</phase>
                        <configuration>
                            <rules>
                                <requireMavenVersion>
                                    <version>[${{maven.version}}.0,)</version>
                                </requireMavenVersion>
                                <requireJavaVersion>
                                    <version>${{maven-enforcer-java-version}}</version>
                                </requireJavaVersion>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
{comp}
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>${{maven-surefire-plugin.version}}</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-source-plugin</artifactId>
                <version>${{maven-source-plugin.version}}</version>
                <executions>
                    <execution>
                        <id>attach-sources</id>
                        <goals>
                            <goal>jar-no-fork</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>${{maven-jar-plugin.version}}</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-deploy-plugin</artifactId>
                <version>3.1.2</version>
            </plugin>
        </plugins>
    </build>
{DEPS_HEAD}{UNIREST_WS_DEPS}{DEPS_TAIL}
</project>
'''
    POM.write_text(body, encoding="utf-8")


def render(branch: str) -> None:
    snap = VERSION_DATE_SUFFIX
    common_unirest = "3.14.5"
    common_ws = "1.5.7"
    if branch == "2.3.x":
        write_pom(
            version=f"{branch}.{snap}",
            java_version="1.8",
            java_release="1.8",
            use_release=False,
            description=(
                "OpenCLI multi-adapter CLI integration SDK — Spring Boot 2.3.x line (JDK 8)"
            ),
            jackson="2.18.8",
            junit="5.9.3",
            slf4j="1.7.36",
            lombok="1.18.34",
            unirest=common_unirest,
            java_websocket=common_ws,
            maven_enforcer="3.4.1",
            maven_compiler="3.11.0",
            maven_surefire="3.1.2",
            maven_source="3.3.0",
            maven_jar="3.3.0",
        )
        return
    if branch == "2.7.x":
        write_pom(
            version=f"{branch}.{snap}",
            java_version="11",
            java_release="11",
            use_release=True,
            description=(
                "OpenCLI multi-adapter CLI integration SDK — Spring Boot 2.7.x line (JDK 11)"
            ),
            jackson="2.18.8",
            junit="5.9.3",
            slf4j="1.7.36",
            lombok="1.18.34",
            unirest=common_unirest,
            java_websocket=common_ws,
            maven_enforcer="3.4.1",
            maven_compiler="3.11.0",
            maven_surefire="3.1.2",
            maven_source="3.3.0",
            maven_jar="3.3.0",
        )
        return
    if branch in {"3.0.x", "3.1.x", "3.2.x", "3.3.x", "3.4.x"}:
        write_pom(
            version=f"{branch}.{snap}",
            java_version="17",
            java_release="17",
            use_release=True,
            description=(
                "OpenCLI multi-adapter CLI integration SDK — Spring Boot "
                f"{branch.replace('.x', '')} line (JDK 17)"
            ),
            jackson="2.17.2",
            junit="5.11.4",
            slf4j="1.7.36",
            lombok="1.18.36",
            unirest=common_unirest,
            java_websocket=common_ws,
            maven_enforcer="3.4.1",
            maven_compiler="3.13.0",
            maven_surefire="3.5.2",
            maven_source="3.3.1",
            maven_jar="3.4.2",
        )
        return
    if branch in {"3.5.x", "4.0.x"}:
        write_pom(
            version=f"{branch}.{snap}",
            java_version="21",
            java_release="21",
            use_release=True,
            description=(
                "OpenCLI multi-adapter CLI integration SDK — Spring Boot "
                f"{branch.replace('.x', '')} line (JDK 21; SLF4J 2.x)"
            ),
            jackson="2.18.2",
            junit="5.11.4",
            slf4j="2.0.16",
            lombok="1.18.36",
            unirest=common_unirest,
            java_websocket=common_ws,
            maven_enforcer="3.4.1",
            maven_compiler="3.13.0",
            maven_surefire="3.5.2",
            maven_source="3.3.1",
            maven_jar="3.4.2",
        )
        return
    raise SystemExit(f"unsupported branch: {branch}")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__.strip(), file=sys.stderr)
        sys.exit(2)
    render(sys.argv[1])
