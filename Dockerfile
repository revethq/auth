# Build native executable first:
# ./gradlew :web:build -Dquarkus.native.enabled=true
#
# Then build container:
# docker build -t revet/auth:native .

FROM registry.access.redhat.com/ubi9/ubi-micro:9.5

WORKDIR /work

COPY web/build/web-1.0-SNAPSHOT-runner /work/application

EXPOSE 5000

CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
