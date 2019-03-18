#!/bin/bash
set +x

# Enable GC logging for memory tracking
JAVA_OPTS="${JAVA_OPTS} -Dvertx.cacheDirBase=/tmp -XX:NativeMemoryTracking=summary -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps"

exec java $JAVA_OPTS -jar $JAR $@
