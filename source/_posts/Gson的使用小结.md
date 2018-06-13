---
title: Gson的使用小结
date: 2018-06-13 09:37:06
tags: android
reward: true
more: true
---

# 1.Gson的基本用法 #

	public class User {
	    //省略其它  
	    public String name;  
	    public int age;  
	    public String emailAddress;  
	}

生成：  

	Gson gson = new Gson();  
	User user = new User("xhh",24);  
	String jsonObject = gson.toJson(user); // {"name":"xhh","age":24}  
解析：   

	Gson gson = new Gson();  
	String jsonString = "{\"name\":\"xhh\",\"age\":24}";  
	User user = gson.fromJson(jsonString, User.class);  


<!--more-->

# 2. 属性重命名 @SerializedName 注解的使用 #

	 @SerializedName("email_address")
	 public String emailAddress="";

为POJO字段提供备选属性名(注：alternate需要2.4版本)

	 @SerializedName(value = "email_address", alternate = {"email", "emailAddress"})
	 public String emailAddress="";

当上面的三个属性(email_address、email、emailAddress)都中出现任意一个时均可以得到正确的结果。
注：当多种情况同时出时，以最后一个出现的值为准。


# 3.Gson中使用泛型 #

List泛型定义：

	Gson gson = new Gson();
	String jsonArray = "[\"Android\",\"Java\",\"PHP\"]";
	String[] strings = gson.fromJson(jsonArray, String[].class);
	List<String> stringList = gson.fromJson(jsonArray, new TypeToken<List<String>>() {}.getType());


自定义 泛型定义：

	public class Result<T> {
	    public int code;
	    public String message;
	    public T data;
	}

	//不再重复定义Result类  
	Type userType = new TypeToken<Result<User>>(){}.getType();
	Result<User> userResult = gson.fromJson(json,userType);
	User user = userResult.data;

	Type userListType = new TypeToken<Result<List<User>>>(){}.getType();
	Result<List<User>> userListResult = gson.fromJson(json,userListType);
	List<User> users = userListResult.data;

泛型接口定义：

	class Result<T> {
	    public int code;
	    public String message;
	    public T data;
	
	    @Override
	    public String toString() {
	        return "Result{" +
	                "code=" + code +
	                ", message='" + message + '\'' +
	                ", data=" + data +
	                '}';
	    }
	}
	
    public static <T> Result<T> fromJsonObject(String reader, Class<T> clazz) {
        Gson gson = new Gson();
        Type type = new ParameterizedTypeImpl(Result.class, new Class[]{clazz});
        return gson.fromJson(reader, type);
    }

    public static <T> Result<List<T>> fromJsonArray(String reader, Class<T> clazz) {
        Gson gson = new Gson();
        // 生成List<T> 中的 List<T>
        Type listType = new ParameterizedTypeImpl(List.class, new Class[]{clazz});
        // 根据List<T>生成完整的Result<List<T>>
        Type type = new ParameterizedTypeImpl(Result.class, new Type[]{listType});
        return gson.fromJson(reader, type);
    }

泛型生成库：  
[TypeBuilder](https://github.com/ikidou/TypeBuilder "TypeBuilder")

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

	dependencies {
	        implementation 'com.github.ikidou:TypeBuilder:1.0'
	}

例子：Result<List<User>>的type

正常情况：

       Type userListType = new TypeToken<Result<List<User>>>() {
        }.getType();
使用库：

        Type type = TypeBuilder.newInstance(Result.class).addTypeParam(
                TypeBuilder.newInstance(List.class).addTypeParam(User.class).build())
                .build();

# 4.流式序列化 #

	Gson.toJson(Object);
	Gson.fromJson(Reader,Class);
	Gson.fromJson(String,Class);
	Gson.fromJson(Reader,Type);
	Gson.fromJson(String,Type);

手动解析：

	String json = "{\"name\":\"xhh\",\"age\":\"24\"}";
	User user = new User();
	JsonReader reader = new JsonReader(new StringReader(json));
	reader.beginObject(); // throws IOException
	while (reader.hasNext()) {
	    String s = reader.nextName();
	    switch (s) {
	        case "name":
	            user.name = reader.nextString();
	            break;
	        case "age":
	            user.age = reader.nextInt(); //自动转换
	            break;
	        case "email":
	            user.email = reader.nextString();
	            break;
	    }
	}
	reader.endObject(); // throws IOException
	System.out.println(user.name);  // xhh
	System.out.println(user.age);   // 24
	System.out.println(user.email); // ikidou@example.com
	
	Gson gson = new Gson();
	User user = new User("xhh",24,"ikidou@example.com");
	gson.toJson(user,System.out); // 写到控制台

手动序列化：

	JsonWriter writer = new JsonWriter(new OutputStreamWriter(System.out));
	writer.beginArray()
	                    .beginObject() // throws IOException
	                    .name("name").value("xhh123")
	                    .name("age").value(25)
	                    .name("email").nullValue() //演示null
	                    .endObject() // throws IOException
	                    .beginObject() // throws IOException
	                    .name("name").value("xhh789")
	                    .name("age").value(25)
	                    .name("email").nullValue() //演示null
	                    .endObject() // throws IOException
	                    .endArray();
	writer.flush(); // throws IOException

提示：除了beginObject、endObject还有beginArray和endArray，两者可以相互嵌套，注意配对即可。beginArray后不可以调用name方法，同样beginObject后在调用value之前必须要调用name方法。

# 5.GsonBuilder创建手动配置GSON #

	Gson gson = new GsonBuilder()
	        .serializeNulls()//配置输出null值的字段
	        .create();
	User user = new User("xhh", 24);
	System.out.println(gson.toJson(user)); //{"name":"xhh","age":24,"email":null}

其他，没用过。。

	Gson gson = new GsonBuilder()
	        //序列化null
	        .serializeNulls()
	        // 设置日期时间格式，另有2个重载方法
	        // 在序列化和反序化时均生效
	        .setDateFormat("yyyy-MM-dd")
	        // 禁此序列化内部类
	        .disableInnerClassSerialization()
	        //生成不可执行的Json（多了 )]}' 这4个字符）
	        .generateNonExecutableJson()
	        //禁止转义html标签
	        .disableHtmlEscaping()
	        //格式化输出
	        .setPrettyPrinting()
	        .create();

# 6. 字段过滤的几种方法 #

## ① 注释过滤 @Expose ##

	@Expose //
	@Expose(deserialize = true,serialize = true) //序列化和反序列化都都生效，等价于上一条
	@Expose(deserialize = true,serialize = false) //反序列化时生效
	@Expose(deserialize = false,serialize = true) //序列化时生效
	@Expose(deserialize = false,serialize = false) // 和不写注解一样

必须使用GsonBuilder来生成gson:

	Gson gson = new GsonBuilder()
	        .excludeFieldsWithoutExposeAnnotation()
	        .create();
	gson.toJson(category);

## ②基于版本 ##

@Since 和 @Until,和GsonBuilder.setVersion(Double)配合使用。@Since 和 @Until都接收一个Double值。
使用方法：当前版本(GsonBuilder中设置的版本) 大于等于Since的值时该字段导出，小于Until的值时该该字段导出。

**Until 到版本号为止（不含）都生成**  
**Since  从版本号开始（含）都生成**

        SinceUntilSample sinceUntilSample = new SinceUntilSample();
        Gson gson3 = new GsonBuilder()
                //.setVersion(0)
                //.setVersion(3)
                //.setVersion(6)
                //.setVersion(2)
                .setVersion(5)
                .serializeNulls()
                .create();
        System.out.println(gson3.toJson(sinceUntilSample));

	class SinceUntilSample {
	    @Since(2.0)
	    public String since;
	    @Until(5.0)
	    public String until;
	    //设置有效区间 4.0<= version < 5
	    @Since(4.0)
	    @Until(5.0)
	    public String midle;
	}

## ③关键字过滤 public、static 、final、private、protected ##

	class ModifierSample {
	    final String finalField = "final";
	    static String staticField = "static";
	    public String publicField = "public";
	    protected String protectedField = "protected";
	    String defaultField = "default";
	    private String privateField = "private";
	}
	
	ModifierSample modifierSample = new ModifierSample();
	        Gson gson4 = new GsonBuilder()
	                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.STATIC, Modifier.PRIVATE) //设置过滤的关键字
	                .create();
	        System.out.println(gson4.toJson(modifierSample));
结果：  

	{"publicField":"public","protectedField":"protected","defaultField":"default"}

**PS:Gson默认情况会排除带有static和transient关键字属性,**

通过 .excludeFieldsWithModifiers(Modifier.FINAL),如果指定了关键字过滤，static关键字默认就不会排除，只有设置了以后才会排除

![源码定位](/assets/blogImg/gson/gson_static.png)


## ④ 自定义排除策略 ##

利用Gson提供的ExclusionStrategy接口，同样需要使用GsonBuilder,相关API 2个，分别是addSerializationExclusionStrategy 和addDeserializationExclusionStrategy 分别针对序列化和反序化时。

        Gson gson5 = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .addSerializationExclusionStrategy(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        // 这里作判断，决定要不要排除该字段,return true为排除
                        System.out.println("shouldSkipField name=" + f.getName());
                        if ("finalField".equals(f.getName()))
                            return true; //按字段名排除
                        Expose expose = f.getAnnotation(Expose.class);
                        if (expose != null && !expose.serialize())
                            return true; //按注解排除
                        return false;
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        // 直接排除某个类 ，return true为排除
                        return (clazz == int.class || clazz == Integer.class);
                    }
                })
                .addDeserializationExclusionStrategy(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return false;
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .create();

# 7.POJO与JSON的字段映射规则 #

GsonBuilder提供了FieldNamingStrategy接口和setFieldNamingPolicy和setFieldNamingStrategy 两个方法。

## ① 默认规则： ##

GsonBuilder.setFieldNamingPolicy 方法与Gson提供的另一个枚举类FieldNamingPolicy配合使用，该枚举类提供了5种实现方式分别为：

	FieldNamingPolicy 结果（仅输出emailAddress字段）
	IDENTITY {"emailAddress":"ikidou@example.com"}
	LOWER_CASE_WITH_DASHES {"email-address":"ikidou@example.com"}
	LOWER_CASE_WITH_UNDERSCORES {"email_address":"ikidou@example.com"}
	UPPER_CAMEL_CASE {"EmailAddress":"ikidou@example.com"}
	UPPER_CAMEL_CASE_WITH_SPACES {"Email Address":"ikidou@example.com"}

	Gson gson6 = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
                .create();

## ②自定义，setFieldNamingStrategy  ##

GsonBuilder.setFieldNamingStrategy 方法需要与Gson提供的FieldNamingStrategy接口配合使用，用于实现将POJO的字段与JSON的字段相对应。

	Gson gson7 = new GsonBuilder()
                .setFieldNamingStrategy(new FieldNamingStrategy() {
                    @Override
                    public String translateName(Field f) {
                        if ("name".equals(f.getName())){//POJO的变量名
                            return "t_name";//对应JSON字段
                        }
                        return f.getName();
                    }
                })
                .serializeNulls()
                .create();

        String user1s = gson7.toJson(user_);
        String jsonStrings = "{\"t_name\":\"xhh22\",\"age\":24,email_address:\"aaa\"}";

        System.out.println("p1="+user1s);
        System.out.println("p2="+gson7.fromJson(jsonStrings, User.class).toString());
结果：  
p1={"t_name":"xhh","age":24,"emailAddress":"abc"}  
p2=User{name='xhh22', age=24, emailAddress='null'}


**注意： @SerializedName注解拥有最高优先级，在加有@SerializedName注解的字段上FieldNamingStrategy不生效！**

# 8.TypeAdapter (最高优先级规则) #

用于接管某种类型的序列化和反序列化过程，包含两个注要方法 write(JsonWriter,T) 和 read(JsonReader) 其它的方法都是final方法并最终调用这两个抽象方法。

**注意：TypeAdapter 以及 JsonSerializer 和 JsonDeserializer 都需要与 GsonBuilder.registerTypeAdapter 或GsonBuilder.registerTypeHierarchyAdapter中注册。**

        Gson gson8 = new GsonBuilder()
                //为User注册TypeAdapter
                .registerTypeAdapter(User.class, new TypeAdapter<User>() {//指定规则对应对象类型

                    //toJson POJO->JSON 序列化过程
                    @Override
                    public void write(JsonWriter out, User value) throws IOException {
                        out.beginObject();
                        out.name("name").value(value.name);
                        out.name("age").value(value.age);
                        out.name("email").value(value.emailAddress);
                        out.endObject();
                    }

                    //fromJson JSON->POJO 反序列化过程
                    @Override
                    public User read(JsonReader in) throws IOException {
                        User user = new User();
                        in.beginObject();
                        while (in.hasNext()) {
                            switch (in.nextName()) {
                                case "name":
                                case "t_name":
                                    user.name = in.nextString();
                                    break;
                                case "age":
                                    user.age = in.nextInt();
                                    break;
                                case "email":
                                case "email_address":
                                case "emailAddress":
                                    user.emailAddress = in.nextString();
                                    break;
                            }
                        }
                        in.endObject();
                        return user;
                    }
                })
                .create();

		String jsonStrings = "{\"t_name\":\"xhh22\",\"age\":24,email_address:\"aaa\"}";
        user1s = gson8.toJson(user_);
        System.out.println("p8z="+user1s);
        System.out.println("p8="+gson8.fromJson(jsonStrings, User.class).toString());

结果：

p8z={"name":"xhh","age":24,"email":"abc"}  
p8=User{name='xhh22', age=24, emailAddress='aaa'}

对INT做字符串空兼容： 

	Gson gson = new GsonBuilder()
	        .registerTypeAdapter(Integer.class, new TypeAdapter<Integer>() {
	            @Override
	            public void write(JsonWriter out, Integer value) throws IOException {
	                out.value(String.valueOf(value)); 
	            }
	            @Override
	            public Integer read(JsonReader in) throws IOException {
	                try {
	                    return Integer.parseInt(in.nextString());
	                } catch (NumberFormatException e) {
	                    return -1;
	                }
	            }
	        })
	        .create();
	System.out.println(gson.toJson(100)); // 结果："100" ?为什么多了个引号
	System.out.println(gson.fromJson("\"\"",Integer.class)); // 结果：-1

注：测试空串的时候一定是"\"\""而不是""，""代表的是没有json串("null")，"\"\""才代表json里的""。

# 9.自定义序列化JsonSerializer和反序列化JsonDeserializer接口 #

对INT做字符串空兼容：

	Gson gson10 = new GsonBuilder()
                .registerTypeAdapter(Integer.class, new JsonDeserializer<Integer>() {
                    @Override
                    public Integer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        try {
                            return json.getAsInt();
                        } catch (NumberFormatException e) {
                            return -1;
                        }
                    }
                })
                .create();
        System.out.println(gson10.toJson(100)); //结果：100 //没有""?
        System.out.println(gson10.fromJson("\"\"", Integer.class)); //结果 -1

所有数字都转成序列化为字符串的例子  

	JsonSerializer<Number> numberJsonSerializer = new JsonSerializer<Number>() {
    	@Override
	    public JsonElement serialize(Number src, Type typeOfSrc, JsonSerializationContext context) {
	        return new JsonPrimitive(String.valueOf(src));
	    }
	};
	Gson gson = new GsonBuilder()
        .registerTypeAdapter(Integer.class, numberJsonSerializer)
        .registerTypeAdapter(Long.class, numberJsonSerializer)
        .registerTypeAdapter(Float.class, numberJsonSerializer)
        .registerTypeAdapter(Double.class, numberJsonSerializer)
        .create();
	System.out.println(gson.toJson(100.0f));//结果："100.0" ?为什么多了个引号

**registerTypeHierarchyAdapter**就可以使用Number.class而不用一个一个的当独注册  
注：如果一个被序列化的对象本身就带有泛型(List<T>)，且注册了相应的TypeAdapter，那么必须调用Gson.toJson(Object,Type)，明确告诉Gson对象的类型。

        Type type0 = new TypeToken<List<User>>() {
        }.getType();
        TypeAdapter typeAdapter = new TypeAdapter<List<User>>() {
            @Override
            public void write(JsonWriter out, List<User> value) throws IOException {
                out.beginArray();
                for (User u : value) {
                    out.beginObject();
                    out.name("name").value(u.name);
                    out.name("age").value(u.age);
                    out.name("email").value(u.emailAddress);
                    out.endObject();
                }
                out.endArray();
            }

            @Override
            public List<User> read(JsonReader in) throws IOException {
                List<User> list = new ArrayList<User>();
                in.beginArray();
                while (in.hasNext()) {
                    User user = new User();
                    in.beginObject();
                    while (in.hasNext()) {
                        switch (in.nextName()) {
                            case "name":
                            case "t_name":
                                user.name = in.nextString();
                                break;
                            case "age":
                                user.age = in.nextInt();
                                break;
                            case "email":
                            case "email_address":
                            case "emailAddress":
                                user.emailAddress = in.nextString();
                                break;
                        }
                    }
                    in.endObject();
                    list.add(user);
                }
                in.endArray();
                return list;
            }
        };

        Gson gson12 = new GsonBuilder()
                .registerTypeAdapter(type0, typeAdapter)
                .create();
        List<User> list = new ArrayList<>();
        list.add(new User("a", 11));
        list.add(new User("b", 22));
        //注意，多了个type参数
        String result = gson12.toJson(list, type0);
        System.out.println("result=" + result);

        List<User> lists = gson12.fromJson(result, type0);
        for (User u : lists) {
            System.out.println("u=" + u);
        }

结果：  

	result=[{"name":"a","age":11,"email":"abc"},{"name":"b","age":22,"email":"abc"}]  
	u=User{name='a', age=11, emailAddress='abc'}  
	u=User{name='b', age=22, emailAddress='abc'}  

# 10.TypeAdapterFactory #

见名知意，用于创建TypeAdapter的工厂类，通过对比Type，确定有没有对应的TypeAdapter，没有就返回null，与GsonBuilder.registerTypeAdapterFactory配合使用。

Gson gson = new GsonBuilder()
    .registerTypeAdapterFactory(new TypeAdapterFactory() {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            return null;
        }
    })
    .create();

**PS:用来针对不同的type匹配不同的TypeAdapter规则**！

# 11.@JsonAdapter注解 #

 在Bean类上声明，用来和对应的TypeAdapter或TypeAdapterFactory链接，免去GsonBuilder注册！

        Gson gson0 = new Gson();
        jsonStrings = "{\"t_name\":\"xhh22\",\"age\":24,email_address:\"aaa\"}";
        String user1ss = gson0.toJson(user_);
        System.out.println("e=" + user1ss);
        System.out.println("e=" + gson0.fromJson(jsonStrings, User.class).toString());

结果：

	e={"name":"xhh","age":24,"email":"abc"}
	e=User{name='xhh22', age=24, emailAddress='aaa'}

**注：@JsonAdapter 仅支持 TypeAdapter或TypeAdapterFactory**  
**注意：JsonAdapter的优先级比GsonBuilder.registerTypeAdapter的优先级更高。**

	class UserTypeAdapter extends TypeAdapter<User> {
	
	    //toJson POJO->JSON
	    @Override
	    public void write(JsonWriter out, User value) throws IOException {
	        out.beginObject();
	        out.name("name").value(value.name);
	        out.name("age").value(value.age);
	        out.name("email").value(value.emailAddress);
	        out.endObject();
	    }
	
	    //fromJson JSON->POJO
	    @Override
	    public User read(JsonReader in) throws IOException {
	        User user = new User();
	        in.beginObject();
	        while (in.hasNext()) {
	            switch (in.nextName()) {
	                case "name":
	                case "t_name":
	                    user.name = in.nextString();
	                    break;
	                case "age":
	                    user.age = in.nextInt();
	                    break;
	                case "email":
	                case "email_address":
	                case "emailAddress":
	                    user.emailAddress = in.nextString();
	                    break;
	            }
	        }
	        in.endObject();
	        return user;
	    }
	}
	
	@JsonAdapter(UserTypeAdapter.class)
	class User {
	    //省略其它
	    public String name;
	    public int age;
	
	    //    @SerializedName("email_address")
	    @SerializedName(value = "email_address", alternate = {"email", "emailAddress"})
	    public String emailAddress = "abc";
	
	    public User(String name, int age) {
	        this.name = name;
	        this.age = age;
	    }
	
	    public User(String name, int i, String s) {
	        this.name = name;
	        this.age = i;
	        this.emailAddress = s;
	    }
	
	    public User() {
	
	    }
	
	    @Override
	    public String toString() {
	        return "User{" +
	                "name='" + name + '\'' +
	                ", age=" + age +
	                ", emailAddress='" + emailAddress + '\'' +
	                '}';
	    }
	}

# 12.TypeAdapter与 JsonSerializer、JsonDeserializer对比 #

1 | TypeAdapter | JsonSerializer、JsonDeserializer
---|---|---
引入版本 | 2.0 | 1.x
Stream API | 支持 | 不支持*，需要提前生成JsonElement
内存占用 | 小 | 比TypeAdapter大
效率 | 高 | 比TypeAdapter低
作用范围 | 序列化**和**反序列化 | 序列化**或**反序列化


参考链接：
https://www.jianshu.com/p/e740196225a4



