package crawler.sina.parser;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdbc.MySQL.control.JdbcConnection;

import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import crawler.sina.bean.SinaHotWord;
import crawler.sina.bean.SinaUserMessage;
import crawler.sina.craw.CrawSina;
import crawler.sina.utils.HttpUtils;

public class ParserMessage {

	public static String userNick = "";
	
	/**将抓取到的用户发布信息进行分析，并存入数据库
	 * @author whp
	 * @param text 网页正文html
	 * @param uid 用户uid
	 */
	public static void parserUserMessage(String text, String uid){
		String str = getRegex("Pl_Official_MyProfileFeed(.*)html\":\"(.*)\"\\}\\)<\\/script>", text, 2);
   		str = str.replaceAll("\\\\t|\\\\r|\\\\n|\\\\", "");
   		String url = "";
   		Parser messageParser = new Parser();
   		try {
			messageParser.setInputHTML("<html><body>"+str+"</body></html>");
			NodeFilter filter = new HasAttributeFilter("class","WB_detail");
			NodeList list = messageParser.extractAllNodesThatMatch(filter);
			
			for(int i=0; i<list.size(); i++){
				SinaUserMessage messageBean = new SinaUserMessage();
				Node node = (Node)list.elementAt(i);
				messageBean.time = getTime(node);
				NodeFilter filterMessage = new HasAttributeFilter("class","WB_text W_f14");
				NodeList listMessage = new NodeList();
				node.collectInto(listMessage, filterMessage);
				messageBean.message = listMessage.elementAt(0).toPlainTextString().trim();
				messageBean.uid = uid;
				String longMsg = listMessage.elementAt(0).toHtml();
				messageBean.isLongMessage = false;
				if(longMsg.contains("href=\"http://t.cn/")){
					String longMessage = getRegex("href=\"http:\\/\\/t.cn(.*)\"",longMsg,1);
					longMessage = "http://t.cn"+longMessage.substring(0, longMessage.indexOf('"'));
					longMessage = getLongMessage(longMessage);
					if(!longMessage.equals("error")){
						messageBean.isLongMessage = true;
						messageBean.longMessage = longMessage;
					}
				}
				if(node.toHtml().contains("WB_feed_expand")){
					messageBean.isRepost = true;
					messageBean.repostList.add(uid);
					filterMessage = new HasAttributeFilter("extra-data","type=atname");
					NodeList uidList = new NodeList();
					listMessage.elementAt(0).collectInto(uidList, filterMessage);
					String userList[] = messageBean.message.split("//@");
					HashSet<String> set = new HashSet<String>();
					for(int userLoop=1; userLoop<userList.length; userLoop++){
						set.add(userList[userLoop].substring(0, userList[userLoop].indexOf(':')));
					}
					for(int j=0; j<uidList.size(); j++){
						Node nodeUid = uidList.elementAt(j);
						url = getRegex("href=\"(.*)\" usercard", nodeUid.toHtml(), 1);
						HttpGet request = new HttpGet(url);
						request.setHeader("Cookie", CrawSina.Cookie);
						request.getParams().setParameter("http.protocol.handle-redirects", false);
						request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:39.0) Gecko/20100101 Firefox/39.0");
						HttpClient client = new DefaultHttpClient();
						HttpResponse response = client.execute(request);
						request.abort();
						client.getConnectionManager().shutdown();
						String urlUid = response.getFirstHeader("Location").getValue();
						if(urlUid.contains("/u/")){
							urlUid = getMothed("http://www.weibo.com/"+urlUid.substring(urlUid.indexOf('u')+2,urlUid.indexOf('?'))+"/nifo");
							if(set.contains(userNick)){
								messageBean.repostList.add(urlUid);
								userNick = "";
							}
						}
						else{
							urlUid = urlUid.substring(0, urlUid.indexOf("?from=feed&"))+"/info";
							urlUid = getMothed(urlUid);
							if(set.contains(userNick)){
								messageBean.repostList.add(urlUid);
								userNick = "";
							}
						}
					}
					NodeFilter filterUrl = new HasAttributeFilter("class","WB_handle W_fr");
					NodeList nodeList = new NodeList();
					node.collectInto(nodeList, filterUrl);
					filterUrl = new TagNameFilter("a");
					NodeList hrefList = new NodeList();
					nodeList.elementAt(0).collectInto(hrefList, filterUrl);
					messageBean.url = getRegex("href=\"(.*)\" class", hrefList.elementAt(1).toHtml(), 1);
					messageBean.repostList.add(getRegex("weibo.com\\/(.*)\\/", messageBean.url, 1));
				}
				
				JdbcConnection messageJdbc = new JdbcConnection();
				messageJdbc.userMessage(messageBean);
			}
		} catch (ParserException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
	   		System.out.println(url);
			e.printStackTrace();
		} 
	}
	//获取发布的长微博内容
	public static String getLongMessage(String url){
		String ret = "";
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			request.setHeader("Cookie", CrawSina.Cookie);
			client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,50000);
			client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,50000);
			request.getParams().setParameter(HttpMethodParams.SO_TIMEOUT,50000);
			request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:39.0) Gecko/20100101 Firefox/39.0");
			HttpResponse response = client.execute(request);
			if(response.containsHeader("Server") && response.getFirstHeader("Server").getValue().contains("WeiBo")){
				String responseText = HttpUtils.getStringFromResponse(response);
				responseText = getRegex("\"domid\":\"Pl_Official_CardLongFeedv6_(.*)html\":\"(.*)\"\\}\\)<\\/script>", responseText, 2);
				responseText = responseText.replaceAll("\\\\t|\\\\r|\\\\n|\\\\", "");
				Parser longMessageParser = new Parser();
				longMessageParser.setInputHTML("<html><body>"+responseText+"</body></html>");
				NodeFilter filter = new HasAttributeFilter("class","WBA_content");
				NodeList list = longMessageParser.extractAllNodesThatMatch(filter);
				responseText = list.elementAt(0).toPlainTextString();
				ret =  responseText;
			}
			else
				ret = "error";
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
	//获取信息发布时间
	public static String getTime(Node node){
		NodeFilter filter = new HasAttributeFilter("class","WB_from S_txt2");
		NodeList list = new NodeList();
		node.collectInto(list, filter);
		if(list.size() > 1)
			return list.elementAt(1).toPlainTextString().trim();
		else
			return list.elementAt(0).toPlainTextString().trim();
	}
	
	//正则表达式匹�?
	public static String getRegex(String regex, String text, int index){
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
   		while (matcher.find()) {
   			return matcher.group(index);
   		}
   		return "";
	}
	public static String getMothed(String url){
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(url);
		request.setHeader("Cookie", CrawSina.Cookie);
		request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:39.0) Gecko/20100101 Firefox/39.0");
		String uid = "";
		try {
			HttpResponse response = client.execute(request);
			String responseText = HttpUtils.getStringFromResponse(response);
			uid = responseText.substring(responseText.indexOf("$CONFIG['oid']='")+16,responseText.indexOf("$CONFIG['oid']='")+26);
			userNick = getRegex("\\$CONFIG\\['onick'\\]='(.*)';",responseText, 1);
		} catch (ClientProtocolException e) { 
			e.printStackTrace();
		} catch (IOException e) { 
			e.printStackTrace();
		}
		return uid;
	}
}
