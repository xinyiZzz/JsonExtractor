package data_analysis2016;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

/**
 * @author ? 
 * @version 1.0.0
 * @since 创建时间：2016年12月10日 功能描述：json与java对象转换 邮箱地址：609610350@qq.com
 */
public class JsonFile {

	public static String ReadFile(String Path) {
		BufferedReader reader = null;
		String laststr = "";
		try {
			FileInputStream fileInputStream = new FileInputStream(
					Path);
			InputStreamReader inputStreamReader = new InputStreamReader(
					fileInputStream,
					"UTF-8");
			reader = new BufferedReader(
					inputStreamReader);
			String tempString = null;
			while ((tempString = reader
					.readLine()) != null) {
				laststr += tempString;
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return laststr;
	}

}