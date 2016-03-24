#!/usr/bin/env bash

SCRIPT="$0"

# SCRIPT may be an arbitrarily deep series of symlinks. Loop until we have the concrete path.
while [ -h "$SCRIPT" ] ; do
  ls=`ls -ld "$SCRIPT"`
  # Drop everything prior to ->
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    SCRIPT="$link"
  else
    SCRIPT=`dirname "$SCRIPT"`/"$link"
  fi
done

# determine machine agent home
MACHINE_AGENT_HOME="`dirname "$SCRIPT"`/.."

# make MACHINE_AGENT_HOME absolute
MACHINE_AGENT_HOME="`cd "$MACHINE_AGENT_HOME"; pwd`"

if [ -x "$MACHINE_AGENT_HOME/jre/bin/java" ]; then
    # Use bundled JRE by default
    JAVA="$MACHINE_AGENT_HOME/jre/bin/java"
elif [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA="`which java`"
fi

if [ ! -x "$JAVA" ]; then
    echo "Could not find any executable java binary. Please install java in your PATH or set JAVA_HOME"
    exit 1
fi

if [ "$1" == "" ]; then
    echo -n "Enter Encryption Key:"
    read key
else
    key="$1"
fi

if [ "$2" == "" ]; then
    echo -n "Enter Password:"
    read -s password
else
    password="$2"
fi

echo ""
java -cp $(dirname "$SCRIPT")/../f5-monitoring-extension.jar com.appdynamics.extensions.crypto.Encryptor $key $password


