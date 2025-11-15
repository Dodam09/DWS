# 1. Java 21 이미지를 베이스로 사용
FROM eclipse-temurin:21-jdk

# 2. 작업 디렉토리
WORKDIR /app

# 3. 소스 복사
COPY . .

# 4. Maven으로 빌드 (테스트는 생략)
RUN ./mvnw -q -DskipTests package

# 5. 빌드된 JAR을 실행용 이름으로 복사
RUN ls -al target
RUN cp target/dws-print-batch-0.0.1-SNAPSHOT.jar app.jar

# 6. 포트 오픈 (스프링부트 기본 8080)
EXPOSE 8080

# 7. 앱 실행
CMD ["java", "-jar", "app.jar"]
