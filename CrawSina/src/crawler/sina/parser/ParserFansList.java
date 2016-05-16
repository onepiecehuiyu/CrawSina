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

public class ParserFansList {

	/**
	 * @author whp
	 * @param text 包含需要解析的粉丝列表的正文html
	 * @return 返回解析得到的粉丝列表uid
	 */
	public static List<String> getFansList(String text){
		List<String> fansList = new ArrayList<String>();
		String str = "";
		String regex = "\"domid\":\"Pl_Official_HisRelation_(.*)html\":\"(.*)\"\\}\\)<\\/script>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
   		while (matcher.find()) {
   			str = matcher.group(2);
   		}
   		str = str.replaceAll("\\\\t|\\\\r|\\\\n|\\\\", "");
   		Parser fansListParser  = new Parser();
   		try {
   			fansListParser.setInputHTML("<html><body>"+str+"</body></html>");
   			NodeFilter filter = new HasAttributeFilter("class","follow_item S_line2");
			NodeList list = fansListParser.extractAllNodesThatMatch(filter);
			for(int i=0; i<list.size();i++){
                Node node = (Node)list.elementAt(i);
                String fan = node.getText();
                String regexFan = "uid=(.*)&fnick";
                Pattern patternFan = Pattern.compile(regexFan);
                Matcher matcherFan = patternFan.matcher(fan);
           		while (matcherFan.find()) {
           			fan = matcherFan.group(1);
           		}
           		fansList.add(fan);
            }
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fansList;
	}
}
