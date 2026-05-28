# Use this Dockerfile after building the native executable locally:
# ./gradlew :web:build -Dquarkus.native.enabled=true
#
# Then build the container:
# docker build -f Dockerfile.native-prebuilt -t revet/auth:native .

FROM chainguard/wolfi-base 

WORKDIR /work

# Copy pre-built native executable
COPY web/build/web-1.0-SNAPSHOT-runner /work/application

EXPOSE 5000

CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
