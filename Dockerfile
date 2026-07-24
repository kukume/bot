# ============================================
# Multi-stage Dockerfile for kukume/bot
# Supports: onebot, qq, telegram
# ============================================

# ---------- Build Stage ----------
FROM gradle:9.5-jdk21 AS builder

WORKDIR /app

# Copy Gradle files first for better layer caching
COPY gradle gradle
COPY gradlew .
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY gradle.properties* ./

# Copy subprojects (headless intentionally omitted)
COPY logic logic
COPY onebot onebot
COPY qq qq
COPY telegram telegram

# Build all projects (installDist for distribution)
RUN gradle --no-daemon clean installDist


# ============================================
# OneBot
# ============================================
FROM eclipse-temurin:21-jre AS onebot

# jmcomic runs through pipx, so keep pipx-managed executables on PATH.
ENV PATH="/root/.local/bin:${PATH}"

RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    pipx \
    && rm -rf /var/lib/apt/lists/* \
    && pipx ensurepath \
    && pipx install jmcomic

RUN mkdir -p /root/jmcomic
COPY jmcomic.yml /root/jmcomic/jmcomic.yml

WORKDIR /app

COPY --from=builder /app/onebot/build/install/onebot /app

EXPOSE 8080

ENTRYPOINT ["/app/bin/onebot"]


# ============================================
# QQ
# ============================================
FROM eclipse-temurin:21-jre AS qq

WORKDIR /app

COPY --from=builder /app/qq/build/install/qq /app

EXPOSE 8080

ENTRYPOINT ["/app/bin/qq"]


# ============================================
# Telegram
# ============================================
FROM eclipse-temurin:21-jre AS telegram

WORKDIR /app

COPY --from=builder /app/telegram/build/install/telegram /app

EXPOSE 8080

ENTRYPOINT ["/app/bin/telegram"]
