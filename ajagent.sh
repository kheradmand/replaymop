#!/bin/bash

ajc -outjar temp.jar -outxml -Xjoinpoints:synchronization $1;
mkdir tempXf12;
cd tempXf12 && jar xf ../temp.jar && cd ..;
ASPECT=`head -n 3 tempXf12/META-INF/aop-ajc.xml | tail -n 1`;
echo "<aspectj>" > tempXf12/META-INF/aop-ajc.xml
echo "<aspects>" >> tempXf12/META-INF/aop-ajc.xml;
echo $ASPECT >> tempXf12/META-INF/aop-ajc.xml;
echo "</aspects>" >> tempXf12/META-INF/aop-ajc.xml;
echo "<weaver options=\"-Xjoinpoints:synchronization\"/>" >> tempXf12/META-INF/aop-ajc.xml;
echo "</aspectj>" >> tempXf12/META-INF/aop-ajc.xml;
jar cvf agent.jar -C tempXf12/ . ;
