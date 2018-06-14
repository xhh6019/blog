package com.xhh.client;

import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;


@Documented
@Target(ElementType.METHOD)
//@Target(
// {ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.CONSTRUCTOR, ElementType.LOCAL_VARIABLE}
// )

@Inherited
@Retention(RetentionPolicy.RUNTIME)
/*public*/ @interface MethodInfo {

    int revision() default 1;

    String comments() default "";

    String value() default "";
}


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


    public static void main(String[] args) {
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
    }
}
