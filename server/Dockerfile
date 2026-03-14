FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/dongadeuce-server-*-all.jar /app/server.jar
EXPOSE 9090
ENV PORT=9090
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
