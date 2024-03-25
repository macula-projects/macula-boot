package dev.macula.boot.starter.web.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;
import java.io.IOException;

/**
 * {@code BigNumberSerializer} 长数字序列化
 *
 * @author michael.tan
 * @since 2024/3/25 11:16:20
 */
@JacksonStdImpl
public class BigNumberSerializer extends NumberSerializer {
    private static final long JS_NUM_MAX = 9007199254740992L;
    private static final long JS_NUM_MIN = -9007199254740992L;
    public static final BigNumberSerializer instance = new BigNumberSerializer(Number.class);

    public BigNumberSerializer(Class<? extends Number> rawType) {
        super(rawType);
    }

    public void serialize(Number value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        long longValue = value.longValue();
        if (longValue >= JS_NUM_MIN && longValue <= JS_NUM_MAX) {
            super.serialize(value, gen, provider);
        } else {
            gen.writeString(value.toString());
        }

    }
}
