package crawler.sina.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdbc.MySQL.control.JdbcConnection;

import net.sf.json.JSONArray;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public class ParserUserInfo {
	/**
	 * 将用户信息页面解析后存入数据库
	 * @param text 用户信息页面正文html
	 * @param uid  用户uid
	 */
	public static void userInfo(String text, String uid){
		Map<String, String> infoMap = new HashMap<String, String>();
		String  tags= "";
		String str = "";
		String regex = "domid\":\"Pl_Official_PersonalInfo(.*)\"html\":\"(.*)\"\\}\\)<\\/script>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
   		while (matcher.find()) {
   			str = matcher.group(2);
   		}
   		str = str.replaceAll("\\\\t|\\\\n|\\\\r|\\\\", "");
   		try {
   			Parser info = new Parser();
   			info.setInputHTML("<html><body>"+str+"</body></html>");
   			NodeFilter filter = new HasAttributeFilter("class","li_1 clearfix");
   			NodeList list = info.extractAllNodesThatMatch(filter);
			for(int i=0; i<list.size();i++){
				Node node = (Node)list.elementAt(i);
				String nodeStr = node.toPlainTextString();
				if(nodeStr.contains("标签")){
					// 得到标签值  将其分隔开
					info.setInputHTML("<html><body>"+str+"</body></html>");
					NodeFilter filter2 = new HasAttributeFilter("class","W_btn_b W_btn_tag");
					NodeList list2 = info.extractAllNodesThatMatch(filter2);
					for(int ii=0; ii<list2.size();ii++){
		                Node node2 = (Node)list2.elementAt(ii);
		                tags += node2.toPlainTextString() + "|";
		            }
					infoMap.put("标签", tags.substring(0, tags.length()-1));
				}
				else{
					int index = nodeStr.indexOf("：");
					infoMap.put(nodeStr.substring(0,index), nodeStr.substring(index+1));
				}
			}
		} catch (ParserException e) {
			e.printStackTrace();
		}
   		//将uid和userInfo写入数据库
   		JdbcConnection jdbc = new JdbcConnection();
   		jdbc.userInfo(uid, JSONArray.fromObject(infoMap).toString());
	}
	public static void officialInfo(String text, String uid){
		Map<String, String> infoMap = new HashMap<String, String>();
		String str = "";
		String regex = "domid\":\"Pl_Core_T3Simpletext(.*)\"html\":\"(.*)\"\\}\\)<\\/script>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
   		while (matcher.find()) {
   			str = matcher.group(2);
   		}
   		str = str.replaceAll("\\\\t|\\\\n|\\\\r|\\\\", "");
   		Parser info = new Parser();
		try {
			info.setInputHTML("<html><body>"+str+"</body></html>");
			NodeFilter filter = new HasAttributeFilter("class","obj_name");
			NodeList listName = info.extractAllNodesThatMatch(filter);
			filter = new HasAttributeFilter("class","WB_innerwrap");
			info.setInputHTML("<html><body>"+str+"</body></html>");
			NodeList listValue = info.extractAllNodesThatMatch(filter);
			for(int i=0; i<listName.size(); i++){
				infoMap.put(listName.elementAt(i).toPlainTextString().trim(), listValue.elementAt(i).toPlainTextString().trim());
			}
		} catch (ParserException e) {
			e.printStackTrace();
		}
		regex = "domid\":\"Pl_Core_T5MultiText(.*)\"html\":\"(.*)\"\\}\\)<\\/script>";
		pattern = Pattern.compile(regex);
		matcher = pattern.matcher(text);
   		while (matcher.find()) {
   			str = matcher.group(2);
   		}
   		str = str.replaceAll("\\\\t|\\\\n|\\\\r|\\\\", "");
   		try {
			info.setInputHTML("<html><body>"+str+"</body></html>");
			NodeFilter filter = new HasAttributeFilter("class","li_1 clearfix");
			NodeList list = info.extractAllNodesThatMatch(filter);
			for(int i=0; i<list.size(); i++){
				String strNow = list.elementAt(i).toPlainTextString().trim();
				infoMap.put(strNow.substring(0,strNow.indexOf('：')), strNow.substring(strNow.indexOf('：')+1));
			}
		} catch (ParserException e) {
			e.printStackTrace();
		}
   		JdbcConnection jdbc = new JdbcConnection();
   		jdbc.userInfo(uid, JSONArray.fromObject(infoMap).toString());
	}
}
