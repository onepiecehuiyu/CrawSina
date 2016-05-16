package crawler.sina.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdbc.MySQL.control.JdbcConnection;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import crawler.sina.bean.SinaHotWord;

public class ParserHotList {

	public static List<SinaHotWord> parserHotList(String key, String text) {
		List<SinaHotWord> hotList = new ArrayList<SinaHotWord>();
		String str = decodeUnicode(text);
		str = str.replaceAll("\\\\t|\\\\n|\\\\r|\\\\", "");
		String regex = "\"pid\":\"pl_top_total(.*)html\":\"(.*)\"\\}\\)<\\/script>";
		if(key.equals("realtimehot"))
			regex = "\"pid\":\"pl_top_realtimehot(.*)html\":\"(.*)\"\\}\\)<\\/script>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
   		while (matcher.find()) {
   			str = matcher.group(2);
   		}
   		String hotWordType[] = {"all", "films", "person", "friends", "realtimehot"};
		JdbcConnection jdbc = new JdbcConnection();
   		if(!key.equals("realtimehot"))
	   		for(int i=0; i<4; i++){
	   			List<SinaHotWord> list = parserHot(hotWordType[i],str);
	   			hotList.addAll(list);
	   			jdbc.hotWordList(hotWordType[i], list);
	   		}
   		else{
   			writeText(str);
   			hotList = parserHot(hotWordType[4],str);
   			jdbc.hotWordList(hotWordType[4], hotList);
   		}
		return hotList;
	}
	public static List<SinaHotWord> parserHot(String type, String text){
		List<SinaHotWord> listHotWord = new ArrayList<SinaHotWord>();
		try {
			Parser hotList = new Parser();
			hotList.setInputHTML("<html><body>"+text+"</body></html>");
			NodeFilter filter = new HasAttributeFilter("tab",type);
			NodeList list = hotList.extractAllNodesThatMatch(filter);
			String str = list.elementAt(0).toHtml();
			hotList.setInputHTML("<html><body>"+str+"</body></html>");
			NodeFilter filter2 = new HasAttributeFilter("action-type","hover");
			list = hotList.extractAllNodesThatMatch(filter2);
			for(int i=0; i<list.size(); i++){
				filter = new TagNameFilter("a");
				filter2 = new HasAttributeFilter("class","td_03");
				Node node = list.elementAt(i);
				NodeList nodeA = new NodeList();
				NodeList nodeP = new NodeList();
				node.collectInto(nodeA, filter);
				node.collectInto(nodeP, filter2);
				SinaHotWord sinaHotWord = new SinaHotWord();
				sinaHotWord.searchTimes = nodeP.elementAt(0).toPlainTextString();
				sinaHotWord.word = nodeA.elementAt(0).toPlainTextString();
				listHotWord.add(sinaHotWord);
			}
		} catch (ParserException e) {
			e.printStackTrace();
		}
		return listHotWord;
	}

	public static void writeText(String text) {
		File f = new File("C:\\Documents and Settings\\zll\\桌面\\test.html");
		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(f));
			output.write(text);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String decodeUnicode(String str) {
		Charset set = Charset.forName("UTF-16");
		Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
		Matcher m = p.matcher(str);
		int start = 0;
		int start2 = 0;
		StringBuffer sb = new StringBuffer();
		while (m.find(start)) {
			start2 = m.start();
			if (start2 > start) {
				String seg = str.substring(start, start2);
				sb.append(seg);
			}
			String code = m.group(1);
			int i = Integer.valueOf(code, 16);
			byte[] bb = new byte[4];
			bb[0] = (byte) ((i >> 8) & 0xFF);
			bb[1] = (byte) (i & 0xFF);
			ByteBuffer b = ByteBuffer.wrap(bb);
			sb.append(String.valueOf(set.decode(b)).trim());
			start = m.end();
		}
		start2 = str.length();
		if (start2 > start) {
			String seg = str.substring(start, start2);
			sb.append(seg);
		}
		return sb.toString();
	}
}
