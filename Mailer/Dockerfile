FROM openjdk:18-oracle

WORKDIR /app/Common
COPY Common .

RUN ./mvnw clean package

WORKDIR /app/Mailer
COPY Mailer .

RUN ./mvnw clean package

CMD ["./mvnw", "spring-boot:run"]
