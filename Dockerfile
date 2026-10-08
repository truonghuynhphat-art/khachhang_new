# ===== Stage 1: Build =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml trước để tận dụng Docker layer cache - chỉ tải lại dependency
# khi pom.xml thay đổi, không phải mỗi lần sửa code Java.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Chạy bằng user thường, không phải root - giảm rủi ro bảo mật nếu container bị chiếm quyền.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

# Thư mục ghi log (vấn đề 6) - cần volume mount ra ngoài ở docker-compose,
# nếu không log sẽ mất khi container bị xoá.
VOLUME ["/app/logs"]

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]