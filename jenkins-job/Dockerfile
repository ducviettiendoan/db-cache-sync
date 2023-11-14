###DOCKERFILE###
FROM maven:3.8.3-openjdk-17

WORKDIR /jenkins-job
COPY . .
# Compile and package the application to an executable JAR 

EXPOSE 8099

CMD [ "mvn", "spring-boot:run" ]