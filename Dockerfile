FROM eclipse-temurin:21-jre

ENV JAR_NAME=billing-scheduler.jar

ADD build/libs/$JAR_NAME $JAR_NAME

CMD ["sh", "-c", "java $JAVA_OPTS -jar $JAR_NAME"]