package crawler.sina.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;


public class ParserBangList {

	/**从风云榜中提取出排行榜列表
	 * @author whp
	 * @param text 风云榜html内容
	 * @return 100个用户榜单uid
	 */
	public static List<String> parserBangList(String text){
		List<String> bangList = new ArrayList<String>();
		String str = "";
		String regex = "\\{\"code\":\"100000\",\"msg\":\"success\",\"data\":\\{\"html\":\"(.*)\",\"pages\":(.*)\\}\\}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
   		while (matcher.find()) {
   			str = matcher.group(1);
   		}
   		str = str.replaceAll("\\\\t|\\\\r|\\\\n|\\\\", "");
   		Parser bangListParser  = new Parser();
   		try {
   			bangListParser.setInputHTML("<html><body>"+str+"</body></html>");
   			NodeFilter filter = new HasAttributeFilter("class","list_info");
			NodeList list = bangListParser.extractAllNodesThatMatch(filter);
			for(int i=0; i<list.size();i++){
                Node node = (Node)list.elementAt(i);
                String bangUser = node.getText();
                String regexFan = "http:\\/\\/weibo.com\\/u\\/(.*)\"";
                Pattern patternFan = Pattern.compile(regexFan);
                Matcher matcherFan = patternFan.matcher(bangUser);
           		while (matcherFan.find()) {
           			bangUser = matcherFan.group(1);
           		}
           		bangList.add(bangUser);
            }
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bangList;
	}
}
