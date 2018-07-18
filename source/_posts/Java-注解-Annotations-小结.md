---
title: Java 注解 Annotations 小结
date: 2018-06-14 16:38:15
tags: Android
reward: true
more: true
---


一直对Java注解的东西一知半解，对各个库中用到的注解方式也只能记住，并不知道原理。今天就找一些相关的资料做一些联系，学习一下。

---

Java1.5引入了注解，当前许多java框架中大量使用注解，如Hibernate、Jersey、Spring。注解作为程序的元数据嵌入到程序当中。注解可以被一些解析工具或者是编译工具进行解析。我们也可以声明注解在编译过程或执行时产生作用。

##### 创建Java自定义注解
    @Documented
    @Target(ElementType.METHOD)
    
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    /*public*/ @interface MethodInfo {
    
        int revision() default 1;
    
        String comments() default "";
    
        String value() default "";
    }

<!--more-->
多个Target的注释方法
    
    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD,
                ElementType.PARAMETER, ElementType.CONSTRUCTOR,
                ElementType.LOCAL_VARIABLE})
    
1. 注解方法不能带有参数；  
2. 注解方法返回值类型限定为：基本类型、String、Enums、Annotation或者是这些类型的数组；  
3. 注解方法可以有默认值；  
4. 注解本身能够包含元注解，元注解被用来注解其它注解。  

这里有四种类型的元注解：

1. @Documented —— 指明拥有这个注解的元素可以被javadoc此类的工具文档化。这种类型应该用于注解那些影响客户使用带注释的元素声明的类型。如果一种声明使用Documented进行注解，这种类型的注解被作为被标注的程序成员的公共API。
2. @Target——指明该类型的注解可以注解的程序元素的范围。该元注解的取值可以为TYPE,METHOD,CONSTRUCTOR,FIELD等。如果Target元注解没有出现，那么定义的注解可以应用于程序的任何元素。
3. @Inherited——指明该注解类型被自动继承。如果用户在当前类中查询这个元注解类型并且当前类的声明中不包含这个元注解类型，那么也将自动查询当前类的父类是否存在Inherited元注解，这个动作将被重复执行知道这个标注类型被找到，或者是查询到顶层的父类。
4. @Retention——指明了该Annotation被保留的时间长短。RetentionPolicy取值为SOURCE,CLASS,RUNTIME。

> Java内建注解
> 
> Java提供了三种内建注解。
> 
> 1. @Override——当我们想要复写父类中的方法时，我们需要使用该注解去告知编译器我们想要复写这个方法。这样一来当父类中的方法移除或者发生更改时编译器将提示错误信息。
> 
> 2. @Deprecated——当我们希望编译器知道某一方法不建议使用时，我们应该使用这个注解。Java在javadoc 中推荐使用该注解，我们应该提供为什么该方法不推荐使用以及替代的方法。
> 
> 3. @SuppressWarnings——这个仅仅是告诉编译器忽略特定的警告信息，例如在泛型中使用原生数据类型。它的保留策略是SOURCE（译者注：在源文件中有效）并且被编译器丢弃。

##### 使用注解：

    public class AnnotationsMain {

        @Override
        @MethodInfo(value = "abc", comments = "toString method",revision = 8)
        public String toString() {
            return "Overriden toString method";
        }
    
        @Deprecated
        @MethodInfo("oldMethod")//value方法可以默认不写name
        public static void oldMethod() {
            throw new NotImplementedException();
        }
    
        @SuppressWarnings({"unchecked", "deprecation"})
        @MethodInfo(comments = "uncheck method", revision = 10)
        public static void uncheck() throws FileNotFoundException {
        }

    }

##### 解析获取注解数据

1. 通过AnnotationsMain.class反射调用获取Methods。
2. 通过method的isAnnotationPresent接口判断是否使用了我们的注释MethodInfo.class。 
3. 最后通过MethodInfo methodAnno = method.getAnnotation(MethodInfo.class);获取注释接口实例。
4. 然后调用对应的revision()方法获取值。

代码：

	try {
        for (Method method : AnnotationsMain.class
                .getClassLoader()
                .loadClass("com.xhh.client.AnnotationsMain")
                .getMethods()) {
            // checks if MethodInfo annotation is present for the method
            if (method.isAnnotationPresent(/*com.xhh.client.*/MethodInfo.class)) {
                try {
                    // iterates all the annotations available in the method
                    System.out.println("method="+method);
                    for (Annotation anno : method.getDeclaredAnnotations()) {
                        System.out.println("Annotation=" + anno);
                    }
                    MethodInfo methodAnno = method.getAnnotation(MethodInfo.class);
                    if (methodAnno != null) {
                        System.out.print("args: revision=" + methodAnno.revision());
                        System.out.print(" comments=" + methodAnno.comments());
                        System.out.println(" value=" + methodAnno.value());
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        }
    } catch (SecurityException | ClassNotFoundException e) {
        e.printStackTrace();
    }
        

输出的结果为：

    method=public java.lang.String com.xhh.client.AnnotationsMain.toString()
    Annotation=@com.xhh.client.MethodInfo(value=abc, revision=8, comments=toString method)
    args: revision=8 comments=toString method value=abc
     
    method=public static void com.xhh.client.AnnotationsMain.uncheck() throws java.io.FileNotFoundException
    Annotation=@com.xhh.client.MethodInfo(value=, revision=10, comments=uncheck method)
    args: revision=10 comments=uncheck method value=
    
    method=public static void com.xhh.client.AnnotationsMain.oldMethod()
    Annotation=@java.lang.Deprecated()
    Annotation=@com.xhh.client.MethodInfo(value=oldMethod, revision=1, comments=)
    args: revision=1 comments= value=oldMethod

[右键另存demo例子的源码](/assets/blogImg/annotations/AnnotationsMain.java)

#### 简单分析Retrofit中的注释解析过程

1. 定义一个service接口

代码：

    public interface BlogService {
        @GET("blog/{id}") //这里的{id} 表示是一个变量
        Call<ResponseBody> getBlog(/** 这里的id表示的是上面的{id} */@Path("id") int id);
    }
    
2. 使用Retrofit的create接口实例化service接口  

    BlogService service = retrofit.create(BlogService.class);

进入create方法：

    public <T> T create(final Class<T> service) {
        ...
        return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
        new InvocationHandler() {
          private final Platform platform = Platform.get();

          @Override public Object invoke(Object proxy, Method method, Object... args)
              throws Throwable {
            // If the method is a method from Object then defer to normal invocation.
            if (method.getDeclaringClass() == Object.class) {
              return method.invoke(this, args);
            }
            if (platform.isDefaultMethod(method)) {
              return platform.invokeDefaultMethod(method, service, proxy, args);
            }
            ServiceMethod serviceMethod = loadServiceMethod(method);
            OkHttpCall okHttpCall = new OkHttpCall<>(serviceMethod, args);
            return serviceMethod.callAdapter.adapt(okHttpCall);
          }
        });
    }
    
Proxy.newProxyInstance()是java.lang.reflect里的方法，属于反射技术不管它，这边最主要的是通过**InvocationHandler接口**获得method方法，然后**解析注解**，生成注解对应的方法和OkHttpCall之间的关系。

    ServiceMethod serviceMethod = loadServiceMethod(method);//解析注解
    OkHttpCall okHttpCall = new OkHttpCall<>(serviceMethod, args);//传入OKHttp
    return serviceMethod.callAdapter.adapt(okHttpCall);返回接口

解析方法： 

      ServiceMethod loadServiceMethod(Method method) {
        ServiceMethod result;
        synchronized (serviceMethodCache) {
          result = serviceMethodCache.get(method);
          if (result == null) {
            result = new ServiceMethod.Builder(this, method).build();
            serviceMethodCache.put(method, result);
          }
        }
        return result;
      }
      
方法关系保存在:

    private final Map<Method, ServiceMethod> serviceMethodCache = new LinkedHashMap<>();

传入Method解析流程：

    result = new ServiceMethod.Builder(this, method).build();

进入build,里面遍历了Annotation注释:

    public ServiceMethod build() {
    ...
        for (Annotation annotation : methodAnnotations) {
            parseMethodAnnotation(annotation);
        }
    ...
    }

最后就是解析具体的注释了，

    private void parseMethodAnnotation(Annotation annotation) {
      if (annotation instanceof DELETE) {
        parseHttpMethodAndPath("DELETE", ((DELETE) annotation).value(), false);
      } else if (annotation instanceof GET) {
        parseHttpMethodAndPath("GET", ((GET) annotation).value(), false);
      } else if (annotation instanceof HEAD) {
        parseHttpMethodAndPath("HEAD", ((HEAD) annotation).value(), false);
        if (!Void.class.equals(responseType)) {
          throw methodError("HEAD method must use Void as response type.");
        }
      } else if (annotation instanceof PATCH) {
        parseHttpMethodAndPath("PATCH", ((PATCH) annotation).value(), true);
      } else if (annotation instanceof POST) {
        parseHttpMethodAndPath("POST", ((POST) annotation).value(), true);
      } else if (annotation instanceof PUT) {
        parseHttpMethodAndPath("PUT", ((PUT) annotation).value(), true);
      } else if (annotation instanceof OPTIONS) {
        parseHttpMethodAndPath("OPTIONS", ((OPTIONS) annotation).value(), false);
      } else if (annotation instanceof HTTP) {
        HTTP http = (HTTP) annotation;
        parseHttpMethodAndPath(http.method(), http.path(), http.hasBody());
      } else if (annotation instanceof retrofit2.http.Headers) {
        String[] headersToParse = ((retrofit2.http.Headers) annotation).value();
        if (headersToParse.length == 0) {
          throw methodError("@Headers annotation is empty.");
        }
        headers = parseHeaders(headersToParse);
      } else if (annotation instanceof Multipart) {
        if (isFormEncoded) {
          throw methodError("Only one encoding annotation is allowed.");
        }
        isMultipart = true;
      } else if (annotation instanceof FormUrlEncoded) {
        if (isMultipart) {
          throw methodError("Only one encoding annotation is allowed.");
        }
        isFormEncoded = true;
      }
    }



参考地址：  

[英文参考](https://www.javacodegeeks.com/2012/11/java-annotations-tutorial-with-custom-annotation.html)  
[java注解教程](http://www.importnew.com/17413.html)
