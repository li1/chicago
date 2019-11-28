FROM openjdk
RUN mkdir -p /usr/chicago
WORKDIR /usr/chicago
EXPOSE 6262
COPY target/chicago-1.0.0-standalone.jar /usr/chicago
CMD ["java", "-jar", "chicago-1.0.0-standalone.jar"]