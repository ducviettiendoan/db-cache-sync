###DOCKERFILE###
FROM maven:3.8.3-openjdk-17

WORKDIR /sb
COPY . .
# Compile and package the application to an executable JAR 

EXPOSE 8085

CMD [ "mvn", "spring-boot:run" ]