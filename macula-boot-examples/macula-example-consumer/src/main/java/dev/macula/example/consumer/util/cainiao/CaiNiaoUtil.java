package dev.macula.example.consumer.util.cainiao;

import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import java.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author caobo
 */
@Slf4j
public class CaiNiaoUtil {

    /**
     * 菜鸟请求参数加密配置
     *
     * @param content   加密字符串
     * @param secretKey 密钥
     * @param charset   utf-8
     * @return 加密后字符串
     */
    public static String getDataDigest(String content, String secretKey, String charset) {
        try {
            String message = content.concat(secretKey);
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(message.getBytes(charset));
            return Base64.getEncoder().encodeToString(md.digest());
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 菜鸟接口纠正省市区级编号-调用菜鸟接口实现
     *
     * @param msgType 消息类型
     * @return String
     */
    public static Result getCaiLink(String msgType, JSON body) {
        String addressJson = JSONUtil.toJsonStr(body);
        String dataDigest = CaiNiaoUtil.getDataDigest(addressJson, "2sf48559Wp2eI86757P1h7DFsICE54Hs", "utf-8");
        String messageId = String.valueOf(System.currentTimeMillis());
        Map<String, Object> map = new HashMap<>(6);
        map.put("logistics_interface", addressJson);
        map.put("data_digest", dataDigest);
        map.put("partner_code", "348295");
        map.put("logistic_provider_id", "746ef6ea271c4aecf69e6ed458964f9c");
        map.put("msg_type", msgType);
        map.put("msg_id", messageId);

        String result = HttpUtil.post("https://link.cainiao.com/gateway/link.do", map);

        Result obj = null;
        if ("CNDZK_CHINA_SUB_DIVISIONS".equals(msgType)) {
            obj = JSONUtil.toBean(result, DivisionResult.class);
        }
        if ("CNDZK_DIVISION_PARSE".equals(msgType)) {
            result = result.replaceAll("ParseDivisionResult", "parseDivisionResult");
            obj = JSONUtil.toBean(result, ParseDivsionResult.class);
        }
        return obj;
    }

    private static void getDivision(String divisionId, List<Division> list) {
        DivisionResult result = (DivisionResult)CaiNiaoUtil.getCaiLink("CNDZK_CHINA_SUB_DIVISIONS",
            JSONUtil.createObj().set("divisionId", divisionId).set("version", "20211231001"));

        if (result.isSuccess()) {
            list.addAll(result.getDivisionsList());
            for (Division division : result.getDivisionsList()) {
                if (division.getDivisionLevel() < 5) {
                    log.info("正在下载{}", division.getDivisionName());
                    getDivision(division.getDivisionId(), list);
                }
            }
        }
    }

    private static void writeToDivisonCsv() {
        log.info("开始下载Division");
        List<Division> list = new ArrayList<>();

        getDivision("1", list);

        log.info("开始写CSV文件");
        CsvWriter writer = CsvUtil.getWriter("~/Downloads/division.csv", CharsetUtil.CHARSET_UTF_8);
        writer.writeBeans(list);
        writer.close();
        log.info("完成下载Division");
    }

    public static void main(String[] args) {
//        ParseDivsionResult result = (ParseDivsionResult)getCaiLink("CNDZK_DIVISION_PARSE",
//            JSONUtil.createObj().set("address", "邢台市泉东街道公园东街1002号天业悦荣府")
//                .set("hint", JSONUtil.createObj().set("preferOriginDivision", "false")).set("version", "20211231001"));
//        log.info("返回值:{}", JSONUtil.toJsonStr(result));
        writeToDivisonCsv();
    }
}
