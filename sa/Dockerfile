FROM maven:3.8.3-openjdk-17

WORKDIR /sa
COPY . .
# Compile and package the application to an executable JAR
# RUN mvn clean install
# RUN mvn package 

EXPOSE 8090

CMD [ "mvn", "spring-boot:run" ]