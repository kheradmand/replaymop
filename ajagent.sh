#!/bin/bash

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <aspect>" >&2
  exit 1
fi

if [ ! -f "$1" ]; then
  echo "file does not exist" >&2
  exit 1
fi

TFOLDER="tempXf12";
AOPAJC=$TFOLDER"/META-INF/aop-ajc.xml";

ajc -outjar temp.jar -outxml -Xjoinpoints:synchronization $1;
mkdir $TFOLDER;
cd $TFOLDER && jar xf ../temp.jar && cd ..;
ASPECT=`head -n 3 $AOPAJC | tail -n 1`;
echo "<aspectj>" > $AOPAJC
echo "<aspects>" >> $AOPAJC;
echo $ASPECT >> $AOPAJC;
echo "</aspects>" >> $AOPAJC;
echo "<weaver options=\"-Xjoinpoints:synchronization\"/>" >> $AOPAJC;
echo "</aspectj>" >> $AOPAJC;
jar cvf agent.jar -C $TFOLDER . ;
rm -rf $TFOLDER/ temp.jar;
