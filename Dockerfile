# syntax=docker/dockerfile:1
# Image سفارشی Keycloak: providerهای providers/* و تم‌های themes/* داخل image ساخته می‌شوند.
# الگو برگرفته از https://github.com/Hossein-i/keycloak-starter-kit

ARG KC_VERSION=26.8.0
ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21

# --------------------------------------------------
# Provider builder
# --------------------------------------------------
FROM ${MAVEN_IMAGE} AS provider-builder
ARG KC_VERSION

WORKDIR /build

COPY pom.xml ./
COPY providers/ ./providers/

# تست‌ها هم اجرا می‌شوند؛ jar هر ماژول + وابستگی‌های runtime (مثل libphonenumber)
# مخزن Maven در cache mount می‌ماند (حتی با --no-cache) تا هر build دوباره از اینترنت دانلود نکند
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -Dkeycloak.version=${KC_VERSION} package dependency:copy-dependencies \
        -DincludeScope=runtime -DoutputDirectory=target/dependency \
 && mkdir -p /build/dist \
 && for module in providers/*/; do \
        cp "$module"target/*.jar /build/dist/; \
        if [ -d "$module"target/dependency ]; then cp "$module"target/dependency/*.jar /build/dist/; fi; \
    done \
 && rm -f /build/dist/*-sources.jar /build/dist/*-javadoc.jar /build/dist/*-tests.jar \
 && ls -l /build/dist

# --------------------------------------------------
# Keycloak runtime
# --------------------------------------------------
FROM quay.io/keycloak/keycloak:${KC_VERSION}

# گزینه‌های build-time؛ در kc.sh build ثابت می‌شوند و در اجرا با --optimized استفاده می‌شوند
ENV KC_DB=postgres

COPY --from=provider-builder /build/dist/ /opt/keycloak/providers/
COPY themes/ /opt/keycloak/themes/

RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized"]
