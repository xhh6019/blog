---
title: Retrofit学习小结
date: 2018-07-18 16:58:32
tags: android
reward: true
more: true
---


[官网](http://square.github.io/retrofit/)

Use annotations to describe the HTTP request:

URL parameter replacement and query parameter support  
Object conversion to request body (e.g., JSON, protocol buffers)  
Multipart request body and file upload  

三大特点：
1. 使用注释描述HTTP请求接口，可传入URL和qurey参数
2. 支持请求体和结果通过Gson等**协议**方式转换
3. 多部分请求主体和文件上传？**这个暂时不理解**

GRADLE配置：

    compile 'com.squareup.retrofit2:retrofit:+'
<!--more-->
**Retrofit requires at minimum Java 7 or Android 2.3.**

PROGUARD配置：  

    # Platform calls Class.forName on types which do not exist on Android to determine platform.
    -dontnote retrofit2.Platform
    # Platform used when running on Java 8 VMs. Will not be used at runtime.
    -dontwarn retrofit2.Platform$Java8
    # Retain generic type information for use by reflection by converters and adapters.
    -keepattributes Signature
    # Retain declared checked exceptions for use by a Proxy instance.
    -keepattributes Exceptions
    #okio PROGUARD配置
    -dontwarn okio.**


# 1. 支持的CONVERTERS  
CONVERTERS
By default, Retrofit can only deserialize HTTP bodies into OkHttp's ResponseBody type and it can only accept its RequestBody type for @Body.

Converters can be added to support other types. Six sibling modules adapt popular serialization libraries for your convenience.

**Gson: com.squareup.retrofit2:converter-gson**  
**Jackson: com.squareup.retrofit2:converter-jackson**  
**Moshi: com.squareup.retrofit2:converter-moshi**  
**Protobuf: com.squareup.retrofit2:converter-protobuf**  
**Wire: com.squareup.retrofit2:converter-wire**  
**Simple XML: com.squareup.retrofit2:converter-simplexml**  
**Scalars (primitives, boxed, and String): com.squareup.retrofit2:converter-scalars**  

Here's an example of using the GsonConverterFactory class to generate an implementation of the GitHubService interface which uses Gson for its deserialization.

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("https://api.github.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build();
    GitHubService service = retrofit.create(GitHubService.class);

# 2. 基本用法
定义HTTP API：  

    public interface GitHubService {
      @GET("users/{user}/repos")
      Call<List<Repo>> listRepos(@Path("user") String user);
    }

定义Retrofit实例：

    Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("https://api.github.com/") //必须以'/'结尾
    .build();

构建client实例：

    GitHubService service = retrofit.create(GitHubService.class);

调用：

    Call<List<Repo>> repos = service.listRepos("octocat");
        
    //异步方式
    call.enqueue(new Callback<ResponseBody>() {
        @Override
        public void onResponse(
                Call<ResponseBody> call, Response<ResponseBody> response) {
            try {
                if(response.isSuccessful())
                    System.out.println(response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {
            t.printStackTrace();
        }
    });
        
    //同步方式
    try {
            Response<ResponseBody> response = call.execute();
            if (response.isSuccessful()) {
                System.out.println(response.body().string());
            } else {
                System.err.println("HttpCode:" + response.code());
                System.err.println("Message:" + response.message());
                System.err.println(response.errorBody().string());
            }
        } catch (IOException e) {
            e.printStackTrace();
    }

Call instances can be executed either synchronously or asynchronously. Each instance can only be used once, but calling clone() will create a new instance that can be used.

**这个call只能执行一次，使用clone可以生成一个可执行的新实例。**


# 3.使用GsonConverter

#### 3.1 反序列化 GET

服务端口,添加json数据返回:

            Route route = new Route() {
            @Override
            public Object handle(Request request, Response response) throws Exception {
                int id = getId(request);
                if (id < 0) {
                    return "hello from server";
                } else {
                    System.out.println("z handle id=" + id);
                }
                Gson gson = new Gson();
                Bean bean = new Bean("server", id);
                String jsons = gson.toJson(bean, Bean.class);
                System.out.println("jsons=" + jsons);
                return jsons;
            }
            
            public int getId(Request request) {
                String id = request.params("id");
                String idInQueryString = request.queryParams("id");
                
                if (id == null && idInQueryString == null) {
                    return -1;
                } else if (id == null) {
                    id = idInQueryString;
                }
                try {
                    return Integer.valueOf(id);
                } catch (NumberFormatException e) {
                    throw new NumberFormatException("bad id:`" + id + "`");
                }
            }
        };
        Spark.get("/hello", route);
        Spark.get("/hello/:id", route);//这里添加:代表id是个参数


客户端定义接口:

    public interface RetrofitService {
        
        @GET("/hello")
        Call<ResponseBody> getHello();
        
        @GET("/hello/{id}")
        Call<Bean> getHello(@Path("id") int id);
        
    }

构建服务实例:

    Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("http://localhost:7756/")
                .build();

调用:

    Call<Bean> beancall = service.getHello(123);
        beancall.enqueue(new Callback<Bean>() {
            @Override
            public void onResponse(Call<Bean> call, Response<Bean> response) {
                if (response.isSuccessful()) {
                    Bean bean = response.body();
                    System.out.println("recive=" + bean);
                } else {
                    System.out.println("fai 111");
                }
            }
            
            @Override
            public void onFailure(Call<Bean> call, Throwable t) {
                System.out.println("fai 222");
            }
        });

Bean定义:

    class Bean{
        String name;
        int id;
            
        public Bean(String name, int id) {
            this.name = name;
            this.id = id;
        }
            
        @Override
        public String toString() {
            return "Bean{" +
                    "name='" + name + '\'' +
                    ", id=" + id +
                    '}';
        }
    }


结果输出:

    recive=Bean{name='server', id=123}

这边我自己练习的时候剥离了参考demo的Result<T>这一层包装,仅仅传递了一个bean类的json数据,看起来简单多了.

#### 3.2 序列化 POST

服务端添加POST接口：

        Route posetRoute = new Route() {  
            
            @Override  
            public Object handle(Request request, Response response) throws   Exception {
                System.out.println("handle post 55");
                Gson gson = new Gson();
                Bean bean = gson.fromJson(request.body(),Bean.class);
                System.out.println("handle bean="+bean);
                if (bean != null){
                    return "create "+bean.name+" id="+bean.id+" ok.";
                }
                return "error";
            }
        };
        Spark.post("/bean", posetRoute);
        

客户端定义接口: 

        public interface RetrofitService {
        ...
         
        @POST("bean")
        Call<ResponseBody> createBean(@Body Bean bean);
        
    }
    
构建实例和Bean定义 和上一点相同，就不放代码了。
最后是调用：

    Bean newBean = new Bean("xhh",7412);
        Call<ResponseBody> postCall = service.createBean(newBean);
        postCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        System.out.println("z get s=" + (response.body().string()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("fai 222");
                }
            }
            
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
            
            }
        });
        

输出结果：

    z get s=create xhh id=7412 ok.

服务端log:

    handle post 55
    handle bean=Bean{name='xhh', id=7412}

根据实例做了修改，返回只是一个简单的字符串了，同时也剥离了Result<T>


# 4.引入RxJava

添加dependencies

    dependencies {
    compile fileTree(include: ['*.jar'], dir: 'libs')
    compile 'com.squareup.retrofit2:retrofit:2.4.0'
    compile 'com.squareup.retrofit2:converter-gson:2.4.0'
      
    compile 'com.squareup.retrofit2:adapter-rxjava:2.4.0'
    compile 'com.squareup.retrofit2:adapter-rxjava2:2.3.0'
     
    compile 'com.google.code.gson:gson:2.8.5'
    //implementation 'com.github.ikidou:TypeBuilder:1.0'
}

定义Observable接口，

    public interface RetrofitService {
         
        @GET("/hello/{id}")
        Observable<Bean> getRxHello(@Path("id") int id);
    }

引入库：

    import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory; 
    import rx.Observable;
    import rx.Subscriber;
    import rx.schedulers.Schedulers;
        

实例化和调用

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl("http://localhost:7756/")
                .build();
         
        RetrofitService service = retrofit.create(RetrofitService.class);
        service.getRxHello(123)
                .observeOn(Schedulers.io())
                .subscribe(new Subscriber<Bean>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Bean bean) {
                        System.out.println("onNext bean="+bean);
                    }
                });

结果输出：

    onNext bean=Bean{name='server', id=123}


服务器端口直接复用之前

    @GET("/hello/{id}")
        Call<Bean> getHello(@Path("id") int id);
        

从这边可以看出，RxJava只是替换了之前的Call<T>，并通过返回的Observable链式编程。Observable的相关操作方法就是RxJava库的范畴咯。

# 5.自定义Converter和CallAdapter

这部分只是简单的运行一下参考的例子。

##### 5.1 Converter

其实就是实现 Converter<F, T>接口和Converter.Factory类

       /**
     * 自定义Converter实现RequestBody到String的转换
     */
    public static class StringConverter implements Converter<ResponseBody, String> {

        public static final StringConverter INSTANCE = new StringConverter();

        @Override
        public String convert(ResponseBody value) throws IOException {
            return value.string();
        }
    }

    /**
     * 用于向Retrofit提供StringConverter
     */
    public static class StringConverterFactory extends Converter.Factory {

        public static final StringConverterFactory INSTANCE = new StringConverterFactory();

        public static StringConverterFactory create() {
            return INSTANCE;
        }

        // 我们只关实现从ResponseBody 到 String 的转换，所以其它方法可不覆盖
        @Override
        public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
            if (type == String.class) {
                return StringConverter.INSTANCE;
            }
            //其它类型我们不处理，返回null就行
            return null;
        }
    }


将接口定义类型从ResponseBody改为String

     public interface RetrofitService {

        @GET("/hello")
        Call<ResponseBody> getHello();
   
        @GET("/hello")
        Call<String> getHelloString();
    }
    
剩下的就交给IDE自动implement对应的接口吧。

        Retrofit retrofit = new Retrofit.Builder()
                // 如是有Gson这类的Converter 一定要放在其它前面
                .addConverterFactory(StringConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("http://localhost:7756/")
                .build();
         
        RetrofitService service = retrofit.create(RetrofitService.class);
        Call<String> call = service.getHelloString();
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                String body = response.body();
                // 对比ResponseBody的打印方法，少了一层.string()。在自定义的Converter里调用了string()
                // System.out.println("z get s=" + (response.body().string()));
                System.out.println("getHelloString s=" + (response.body()));
            }
          
            @Override
            public void onFailure(Call<String> call, Throwable t) {
     
            }
        });
        
最后输出结果：

    getHelloString s=hello from server

> 注：addConverterFactory是有先后顺序的，如果有多个ConverterFactory都支持同一种类型，那么就是只有第一个才会被使用，而GsonConverterFactory是不判断是否支持的，所以这里交换了顺序还会有一个异常抛出，原因是类型不匹配。  

> 只要返回值类型的泛型参数就会由我们的StringConverter处理,不管是Call<String>还是Observable<String>

##### 5.2 CallAdapter

这个接口在2.3以上版本多了一个参数T,我做了对应的修改。

        public static class CustomCall<R> {

        public final Call<R> call;

        public CustomCall(Call<R> call) {
            this.call = call;
        }

        // 提供一个同步获取数据的方法
        public R get() throws IOException {
            return call.execute().body();
        }
    }

    public static class CustomCallAdapter implements CallAdapter<Call,CustomCall<?>> {

        private final Type responseType;

        // 下面的 responseType 方法需要数据的类型
        CustomCallAdapter(Type responseType) {
            this.responseType = responseType;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public CustomCall<?> adapt(Call<Call> call) {
            // 由 CustomCall 决定如何使用
            return new CustomCall<>(call);
        }
    }

    public static class CustomCallAdapterFactory extends CallAdapter.Factory {
        public static final CustomCallAdapterFactory INSTANCE = new CustomCallAdapterFactory();

        @Override
        public CallAdapter<Call,CustomCall<?>> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
            // 获取原始类型
            Class<?> rawType = getRawType(returnType);
            // 返回值必须是CustomCall并且带有泛型
            if (rawType == CustomCall.class && returnType instanceof ParameterizedType) {
                Type callReturnType = getParameterUpperBound(0, (ParameterizedType) returnType);
                return new CustomCallAdapter(callReturnType);
            }
            return null;
        }
    }
    

添加自定义的服务接口定义：

     public interface RetrofitService {
         
        @GET("/hello")
        Call<ResponseBody> getHello();
         
        @GET("/hello")
        CustomCall<String> getCallHello();
        
    }

调用：

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://localhost:7756/")
            .addConverterFactory(StringConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CustomCallAdapterFactory.INSTANCE)
            .build();
     
    RetrofitService service = retrofit.create(RetrofitService.class);
    CustomCall<String> call = service.getCallHello();
    try {
        String result = call.get();
        System.out.println(result);
    } catch (IOException e) {
        e.printStackTrace();
    }

输出：

    hello from server
    

> 注： addCallAdapterFactory与addConverterFactory同理，也有先后顺序。


# 6.其他

其他的部分没试过，看了一下而已。。


BaseUrl | 和URL有关的注解中提供的值 | 最后结果
---|---|---
http://localhost:4567/path/to/other/ | /post | http://localhost:4567/post
http://localhost:4567/path/to/other/ | post	| http://localhost:4567/path/to/other/post
http://localhost:4567/path/to/other/ | https://github.com/ | https://github.com/

> 从上面不能难看出以下规则：  
如果你在注解中提供的url是完整的url，则url将作为请求的url。  
如果你在注解中提供的url是不完整的url，且不以 / 开头，则请求的url为baseUrl+注解中提供的值  
如果你在注解中提供的url是不完整的url，且以 / 开头，则请求的url为baseUrl的主机部分+注解中提供的值  



参考：  
[简书](https://www.jianshu.com/p/308f3c54abdd)

<br>
<br>
<br>
<br>
<br>
<br>
