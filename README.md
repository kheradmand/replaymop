# ReplayMOP
Deterministic replay of concurrent Java programs using monitoring oriented programming. 

The ultimate goal of this project is to come up with a specification language that can describe the runtime behavior of a given program at the non-deterministic points of its execution, and efficiently enforcing the specified behavior at runtime without any modification to java virtual machine. 

This way, a developer can specify exactly how a program should behave during the runtime without any modification to the source code. An automatic tool can also observe execution of a program and generate such an specification.


##Build:
1. Install [Maven](http://maven.apache.org/). 
2. Install [RV-Predict](https://runtimeverification.com/predict/).
3. Run `mvn install:install-file -Dfile=path/to/rv-predict/lib/rv-predict.jar -DgroupId=com.runtimeverification.rvpredict -DartifactId=root -Dversion=1.3-SNAPSHOT -Dpackaging=jar`
4. Go to the project's root and run `mvn package`

##Use:
ReplayMOP get as input a replay specification (.rs) and outputs a java intrumentation agent (.jar). You can then run your program with the agent and get the specified behavior. For example if you normally run your program this way: 
```
java MyClass
```
You only need to change it to this:
```
java -agent:/path/to/generated/agent.jar MyClass
```

If you want to write an specification manually, refer to the examples (e.g [this](examples/basic/Example1/Example1.rs)) to get familiar with the language. In order to generate the agent from a specication run:
```
java -jar:/path/to/replayMOP/target/release/replayMOP-1.0.0-snapshot.java relplayMop.rsParser /path/to/replay/spec.rs
```

If everything goes well, it generates the agent (```spec.jar```)


