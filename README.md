# ReplayMOP
Deterministic replay of concurrent Java programs using monitoring oriented programming. 

The ultimate goal of this project is to come up with a specification language that can describe the runtime behavior of a given program at the non-deterministic points of its execution, and efficiently enforcing the specified behavior at runtime without any modification to java virtual machine. 

This way, a developer can specify exactly how a program should behave during the runtime without any modification to the source code. An automatic tool can also observe execution of a program and generate such an specification.


##Build:
1. Install [Maven](http://maven.apache.org/). 
2. Install [RV-Predict](https://runtimeverification.com/predict/).
3. Run `mvn install:install-file -Dfile=path/to/rv-predict/lib/rv-predict.jar -DgroupId=com.runtimeverification.rvpredict -DartifactId=root -Dversion=1.3-SNAPSHOT -Dpackaging=jar`
4. Go to the project's root and run `mvn package`




