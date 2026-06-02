FROM eclipse-temurin:24-jdk

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew bootJar -x test

EXPOSE 8080

CMD ["java", "-jar", "build/libs/streakflow-0.0.1-SNAPSHOT.jar"]