./gradlew :web:build
./gradlew :web:imageBuild
docker tag revethq/revet-auth-jvm:latest harbor.cartobucket.com/revethq/revet-auth-jvm:latest
docker push harbor.cartobucket.com/revethq/revet-auth-jvm:latest
