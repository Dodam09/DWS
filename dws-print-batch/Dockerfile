# ----- 빌드 단계 -----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 의존성 캐시를 위해 먼저 pom만 복사
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 실제 소스 복사 후 패키징
COPY src ./src
RUN mvn -B -DskipTests package

# ----- 실행 단계 -----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 빌드에서 생성된 jar 복사 (target 안에 하나뿐이라고 가정)
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
