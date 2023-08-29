FROM konajdk/konajdk:8
ARG JAR_FILE=target/*.jar

ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8
ENV workdir /app
WORKDIR ${workdir}

ENV jar app.jar

COPY ${JAR_FILE} ${workdir}/app.jar
CMD ["sh", "-ec", "exec java -XX:+PrintGCDateStamps -XX:+PrintGCDetails -verbose:gc -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=8 -XX:GCLogFileSize=50M ${JAVA_OPTS} -Xshare:off -jar ${jar}"]
