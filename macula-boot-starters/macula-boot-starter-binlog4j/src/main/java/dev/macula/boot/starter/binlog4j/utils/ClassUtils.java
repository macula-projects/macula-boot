package dev.macula.boot.starter.binlog4j.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ClassUtils {

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getGenericType(Class<?> cls) {
        Type type = cls.getGenericInterfaces()[0];
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType)type;
            Type[] argTypes = paramType.getActualTypeArguments();
            if (argTypes.length > 0) {
                return (Class<T>)argTypes[0];
            }
        }
        return null;
    }
}
