# Runtime-only Image. Das Fat-JAR wird auf dem pve-Host (JDK 21 + Maven) gebaut
# und hierher kopiert — so bleibt der Build auf der ressourcenknappen VM klein.
# Neu bauen: `mvn -DskipTests package` auf pve, dann JAR zur VM syncen.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -S app && adduser -S app -G app

COPY target/m108-owasp.jar app.jar
USER app

EXPOSE 8080
ENV JAVA_OPTS="-Xms64m -Xmx256m"

HEALTHCHECK --interval=30s --timeout=4s --start-period=40s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q UP || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
