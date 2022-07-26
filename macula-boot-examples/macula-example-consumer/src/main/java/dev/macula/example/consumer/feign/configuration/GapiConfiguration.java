package dev.macula.example.consumer.feign.configuration;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.codec.digest.HmacUtils;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static java.time.ZoneOffset.UTC;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.codec.digest.HmacAlgorithms.HMAC_SHA_256;

public class GapiConfiguration implements RequestInterceptor {

    // 消费者名称与密钥
    private static final String CUSTOMER_USERNAME = "applicationA";
    private static final String CUSTOMER_SECRET = "0UXNHZGKPUNUIJMVQRURULAEG2M0Y96R";
    private static final DateTimeFormatter RFC_7231_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O").withLocale(Locale.ENGLISH);

    @Override
    public void apply(RequestTemplate requestTemplate) {
        // 时间
        String date = RFC_7231_FORMATTER.format(ZonedDateTime.now(UTC));

        // 请求体摘要
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hash = messageDigest.digest(requestTemplate.body());
        String digest = "SHA-256=" + encodeBase64String(hash);
        // 签名，计算方法为 hmac_sha_256(key, 日期 + 方法 + 路径 + 摘要)
        HmacUtils hmacSha256 = new HmacUtils(HMAC_SHA_256, CUSTOMER_SECRET);
        String signature = encodeBase64String(hmacSha256.hmac("date: " + date + "\n" + requestTemplate.method() + " " + requestTemplate.url() + " HTTP/1.1\ndigest: " + digest));
        // HMAC 授权
        String authorization = "hmac username=\"" + CUSTOMER_USERNAME + "\", algorithm=\"hmac-sha256\", headers=\"date request-line digest\", signature=\"" + signature + "\"";

        requestTemplate.header("Authorization", authorization);
        requestTemplate.header("Date", date);
        requestTemplate.header("Digest", digest);
    }
}
