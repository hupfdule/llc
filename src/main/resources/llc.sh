#!/bin/sh

get_first() {
  echo $1
}

# We need to refence the tools.jar
if [ -n "$JAVA_HOME" ]; then
  # If JAVA_HOME is set, use its tools.jar
  TOOLS_JAR=$JAVA_HOME/lib/tools.jar
else
  # If JAVA_HOME is not set, try to search for it from the java executable
  WHICH_JAVA=$(which java)
  if [ -n "$WHICH_JAVA" ] && [ -f $(dirname $WHICH_JAVA)/../lib/tools.jar ]; then
    TOOLS_JAR=$(dirname $WHICH_JAVA)/../lib/tools.jar
  fi
  if [ -z "$TOOLS_JAR" ]; then
    # If that didn't work, too, try to find it under /usr/lib/jvm/
    TOOLS_JAR_FROM_USR_LIB_JVM=$(ls /usr/lib/jvm/*/lib/tools.jar)
    if [ -n "$TOOLS_JAR_FROM_USR_LIB_JVM" ]; then
      TOOLS_JAR=$(get_first $TOOLS_JAR_FROM_USR_LIB_JVM)
    else
      # If even that didn't work, assume the tools.jar is already set in
      # the $CLASSPATH
      true
    fi
  fi
fi

SCRIPT_LOCATION=$(dirname $(readlink -f "$0"))
# The application jar file
JAR_FILE=$SCRIPT_LOCATION/llc.jar
CLASSPATH=$CLASSPATH:$JAR_FILE
# If we found a tools.jar, append it to the classpath
if [ -n "$TOOLS_JAR" ]; then
  CLASSPATH="$CLASSPATH":"$TOOLS_JAR"
fi

#now run the application
java -cp $CLASSPATH eu.herrn.loglevelchanger.LogLevelChanger $@
