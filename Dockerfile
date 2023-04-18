FROM azul/zulu-openjdk:17.0.1

ENV TZ=Europe/Berlin

COPY ./org/ /app/org/
COPY ./BOOT-INF/lib/ /app/BOOT-INF/lib/
COPY ./BOOT-INF/classes/de/ /app/BOOT-INF/classes/de/
COPY ./BOOT-INF/classes/config/ /app/BOOT-INF/classes/config/
COPY ./BOOT-INF/classes/banner.txt /app/BOOT-INF/classes/
COPY ./BOOT-INF/classes/document.txt /app/BOOT-INF/classes/
COPY ./META-INF /app/META-INF/

WORKDIR /app

ENTRYPOINT exec java $JAVA_OPTS -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom org.springframework.boot.loader.JarLauncher
