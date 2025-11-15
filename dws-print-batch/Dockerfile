FROM eclipse-temurin:21-jdk

# 1. 컨테이너 안 작업 디렉토리
WORKDIR /app

# 2. 레포 루트에 있는 dws-print-batch 폴더 내용을 /app 으로 복사
COPY dws-print-batch/ .

# 3. mvnw 실행 권한 부여 (혹시 몰라서)
RUN chmod +x mvnw

# 4. Maven 빌드 (테스트 생략)
RUN ./mvnw -q -DskipTests package

# 5. 빌드된 JAR을 실행용 이름으로 이동
RUN mv target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
