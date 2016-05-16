package crawler.sina.utils;

import net.sf.json.JSONObject;

public class JsonUtils {
	/**
	 * 
	 * @param jsonString
	 * @return
	 */
	public static PreLoginResponseMessage jsontoPreLoginResponseMessage(
			String jsondata) {
		JSONObject jsonobj = JSONObject.fromObject(jsondata);
		PreLoginResponseMessage message = (PreLoginResponseMessage) JSONObject
				.toBean(jsonobj, PreLoginResponseMessage.class);
		return message;
	}
}