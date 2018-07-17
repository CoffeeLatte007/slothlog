# slothlog
你是否有以下苦恼:
- 写代码时日志调试信息不足，但是又怕打多了日志影响线上性能。
- 重复的给每个方法打印入参出参，耗时耗力。
- 害怕用写AOP用反射打日志会影响性能。

slothlog是一款快速打日志Info的工具。只需要一个注解，你就可以脱离在重复日志打点，又或是线下调试快速记录信息，并且没有反射的性能损耗，支持在类，方法，变量上面都能进行定点日志打印

# 如何使用
##1.maven打包
首先在编译之前需要注意我们在maven-compile-plugin中有个参数配置,

><compilerArgument>-proc:none</compilerArgument>
>-proc:{none|only}：默认情况下，javac会运行注解处理器并编译全部源文件。如果使用了proc:none选项，
那么所有的注解处理过程都不会被执行——这在编译注解处理器本身的时候很有用。如果使用了proc:only选项，则只有注解处理过程会被执行

输入下面命令进行编译
> mvn compile

然后进行打包和发布到本地仓库
>mvn package

>mvn install

新建一个其他maven项目，在pom中输入
```
        <dependency>
            <groupId>com.lz</groupId>
            <artifactId>sloth</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
```
##2.@LogInfo
@LogInfo可以用于类，方法，以及局部变量中。使用这个注解会创建一个

###2.1 引入日志
由于LogInfo使用的SLF4J，所以想要打印日志请首先配置SLF4J，SLF4J可以整合LOG4J,LOG4J2,LOGBACK等
，这里简单演示一下如何使用SLF4J+LOG4J2整合
 ```
        <dependency>
             <groupId>org.slf4j</groupId>
             <artifactId>slf4j-log4j12</artifactId>
             <version>1.7.2</version>
         </dependency>
         <dependency>
              <groupId>org.slf4j</groupId>
              <artifactId>slf4j-api</artifactId>
              <version>1.7.10</version>
         </dependency>
 ```
 在resource目录下创建log4j.properties文件
 ```
 ### 设置###
 log4j.rootLogger = debug,stdout
 
 ### 输出信息到控制抬 ###
 log4j.appender.stdout = org.apache.log4j.ConsoleAppender
 log4j.appender.stdout.Target = System.out
 log4j.appender.stdout.layout = org.apache.log4j.PatternLayout
 log4j.appender.stdout.layout.ConversionPattern = [%-5p] %d{yyyy-MM-dd HH:mm:ss,SSS} %m%n
 ```
 这里要注意自身的代码中不能占用name为LOGGER这个常量，自己创建一个SLF4J的name为LOGGER的常量。
 ###2.2@LogInfo在类上
 
```
@LogInfo
public class Hello {
    public String hello(String name, String sex) {
        ...
        doSomething();
        ...
        return xx;
      }
}
```
在某个类上使用注解后，编译过后代码会变成如下
```
@LogInfo
public class Hello {
    private static final org.slf4j.Logger LOGGER = LogFactory.getLogger(Hello.class);
    public String hello(String name, String sex) {
            LOGGER.info("Hello.hello invoke start name:{},sex:{}", name, sex);
            ...
            doSomething();
            ...
            return (String)LogUtil.(LOGGER,"Hello.hello invoke end name:{}, sex:{}, end:{}",name,sex, xx);
      }
}
```
我们直接会在类中先生成LOGGER，如果其已经有slf4j的LOGGER，这里就不生成了。
然后在每个方法都会生成入参的info和出参的info。
 ###2.2@LogInfo在类方法上
```
public class Hello {
    @LogInfo
    public String hello(String name, String sex) {
        ...
        doSomething();
        ...
        return xx;
      }
}
```
 编译过后会生成如下代码:  
```
public class Hello {
    private static final org.slf4j.Logger LOGGER = LogFactory.getLogger(Hello.class);
    @LogInfo
    public String hello(String name, String sex) {
            LOGGER.info("Hello.hello invoke start name:{},sex:{}", name, sex);
            ...
            doSomething();
            ...
            return (String)LogUtil.(LOGGER,"Hello.hello invoke end name:{}, sex:{}, end:{}",name,sex, xx);
       }
 }
```
和在类上比较类似只是粒度更小了。
###2.3@LogInfo在局部变量上
```
public class Hello {
    
    public String hello(String name, String sex) {
        ...
        @LogInfo int a = doSomething();
        ...
        return xx;
      }
}
```
编译过后会生成
```
public class Hello {
    private static final org.slf4j.Logger LOGGER = LogFactory.getLogger(Hello.class);
    public String hello(String name, String sex) {
        ...
        @LogInfo int var1 = doSomething();
        LOGGER.info(Hello.hello variable var1:{},var);
        ...
        return xx;
      }
}
```
## 3.线上线下切换
通过Maven可以配置环境，然后通过不同环境隔离使用不同的properties。由于默认状态是开启
如果不想把这个带到线上打印日志的话，在resorces目录下建立文件slothlog.properties
配置state = OFF 即可关闭。

