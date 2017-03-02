package data_analysis2016;

/**
 * @author ? http://lgscofield.iteye.com/blog/1849327
 * @version 1.0.0
 * @since 创建时间：2016年12月10日 功能描述：json与java对象转换 邮箱地址：609610350@qq.com
 */

import java.util.HashMap;   
import java.util.Map;   
import net.sf.json.JSONArray;   
import net.sf.json.JSONObject;  
public final class JSONUtil {  
   
    // 将String转换成JSON   
    public static String string2json(String key, String value) {   
        JSONObject object = new JSONObject();   
        object.put(key, value);   
        return object.toString();   
    }  
   
    // 将数组转换成JSON   
    public static String array2json(Object object) {   
        JSONArray jsonArray = JSONArray.fromObject(object);   
        return jsonArray.toString();   
    }  
   
    // 将Map转换成JSON   
    public static String map2json(Object object) {   
        JSONObject jsonObject = JSONObject.fromObject(object);   
        return jsonObject.toString();   
    }  
   
    // 将domain对象转换成JSON   
    public static String bean2json(Object object) {   
        JSONObject jsonObject = JSONObject.fromObject(object);   
        return jsonObject.toString();   
    }  
   
    // 将JSON转换成domain对象,其中beanClass为domain对象的Class   
    public static Object json2Object(String json, Class beanClass) {   
        return JSONObject.toBean(JSONObject.fromObject(json), beanClass);   
    }  
   
    // 将JSON转换成String   
    public static String json2String(String json, String key) {   
        JSONObject jsonObject = JSONObject.fromObject(json);   
        return jsonObject.get(key).toString();   
    }   
  
    // 将JSON转换成数组,其中valueClass为数组中存放对象的Class   
    public static Object json2Array(String json, Class valueClass) {   
        JSONArray jsonArray = JSONArray.fromObject(json);   
        return JSONArray.toArray(jsonArray, valueClass);   
    }  
  
    // 将JSON转换成Map,其中valueClass为Map中value的Class,keyArray为Map的key   
    public static Map json2Map(Object[] keyArray, String json, Class valueClass) {   
        JSONObject jsonObject = JSONObject.fromObject(json);   
        Map classMap = new HashMap();  
   
        for (int i = 0; i < keyArray.length; i++) {   
            classMap.put(keyArray[i], valueClass);   
        }   
        return (Map) JSONObject.toBean(jsonObject, Map.class, classMap);   
    }  
}