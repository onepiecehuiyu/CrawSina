package crawler.sina.craw;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;

import crawler.sina.bean.SinaHotWord;
import crawler.sina.parser.ParserBangList;
import crawler.sina.parser.ParserFansList;
import crawler.sina.parser.ParserHotList;
import crawler.sina.parser.ParserMessage;
import crawler.sina.parser.ParserUserInfo;
import crawler.sina.utils.HttpUtils;


public class CrawSina {
	
	public static String CookieTC = "";
	public static String Cookie = "";
	public static String ticket = "";
	public static String loginUrl = "http%3A%2F%2Fweibo.com%2Fajaxlogin.php%3Fframelogin%3D1%26callback%3Dparent.sinaSSOController.feedBackUrlCallBack%26sudaref%3Dweibo.com";
	
	/**
	 * @author whp
	 * @param uid 用户uid
	 * @param page 页码
	 * @param times 当前页第几次抓取
	 * @return 抓取用户发布的信息
	 */
	public String getUserMessage(String uid, String page, String times) {
		String url = "";
		if(times.equals("1"))
			url = "http://weibo.com/u/" + uid
				+ "?page="+page+"&is_search=0&_t=FM_143723067216932&ajaxpagelet_v6=1";
		if(times.equals("2"))
			url = "http://weibo.com/u/"+ uid +"?page="+ page +"&pagebar=0&pre_page="+page;
		if(times.equals("3"))
			url = "http://weibo.com/u/"+ uid +"?page="+ page +"&pagebar=1&pre_page="+page;
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Cookie", CrawSina.Cookie);
		String responseText = getMethod(url, headers);
		if(responseText.contains("Sina Visitor System")){
			String specialUrl = getSpecialUrl(url);
			responseText = getMethod(specialUrl, headers);
		}
		ParserMessage.parserUserMessage(responseText,uid);
		return responseText;
	}
	/**
	 * @author whp
	 * @param bangType风云榜类别,分别是renwu yisheng shishang jiaoyu licai jianshen lvxing qinggan dianying 
	 * @param dataType排行时间类型,分三种:day week month
	 * @return 返回风云榜排行榜100人
	 */
	public List<String> getBangList(String bangType, String dataType) {
		HttpClient client = new DefaultHttpClient();
		String data = getBangTime(dataType);
		String url = "http://bang.weibo.com/aj/getrank";
		HttpPost request = new HttpPost(url);
		request.setHeader("Referer", "http://bang.weibo.com/");
		request.setHeader("Host", "bang.weibo.com");
		request.setHeader("Accept:", "application/json");
		request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:39.0) Gecko/20100101 Firefox/39.0");
		client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,50000);
		client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,50000);
		request.getParams().setParameter(HttpMethodParams.SO_TIMEOUT,50000);
		List<NameValuePair> parameters = new ArrayList<NameValuePair>();
		parameters.add(new BasicNameValuePair("ctime", data));
		parameters.add(new BasicNameValuePair("page", "1"));
		parameters.add(new BasicNameValuePair("pagesize", "100"));
		parameters.add(new BasicNameValuePair("space", dataType));
		parameters.add(new BasicNameValuePair("type", bangType));
		UrlEncodedFormEntity formEntiry;
		String responseText = "";
		try {
			formEntiry = new UrlEncodedFormEntity(parameters);
			request.setEntity(formEntiry);
			HttpResponse response = client.execute(request);
			responseText = HttpUtils.getStringFromResponse(response);
			request.abort();
			client.getConnectionManager().shutdown();
		} catch (UnsupportedEncodingException e) { 
			e.printStackTrace();
		} catch (ClientProtocolException e) { 
			e.printStackTrace();
		} catch (IOException e) { 
			e.printStackTrace();
		}
		return ParserBangList.parserBangList(responseText);
	}
	/**
	 * @author whp
	 * @param uid 用户uid
	 * @param page 页码
	 * @return 抓取用户关注的人列表
	 */
	public String getFollowListByUid(String uid, String page) {
		String url = "http://weibo.com/"+ uid +"/follow?page="+page;
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Cookie", CrawSina.Cookie);
		String responseText = getMethod(url, headers);
		return responseText;
	}
	/**
	 * @author whp
	 * @param uid 用户uid
	 * @param page 页码 (最大数为5)
	 * @return 抓取用户粉丝列表
	 */
	public List<String> getFansListByUid(String uid, String page) {
		List<String> uidList = null;
		try {
			Random random = new Random();
			long s = random.nextInt(9)+1;
			Thread.sleep(s*1000);
			String url = "http://weibo.com/"+ uid +"/fans?&uid=&tag=&page="+ page;
			HashMap<String, String> headers = new HashMap<String, String>();
			headers.put("Cookie", CrawSina.Cookie);
			String responseText = getMethod(url, headers);
			uidList = ParserFansList.getFansList(responseText);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return uidList;
	}
	/**
	 * @author whp
	 * @param uid 用户uid
	 * @return 抓取用户基本信息页面 将信息传递给ParserUserInfo.userInfo中,并存入数据库
	 */
	public String getUserInfo(String uid) {
		String url = "http://weibo.com/"+ uid +"/info";
		try {
			Random random = new Random();
			long s = random.nextInt(9)+1;
			Thread.sleep(s*1000);
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			request.setHeader("Cookie", CrawSina.Cookie);
			client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,50000);
			client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,50000);
			request.getParams().setParameter(HttpMethodParams.SO_TIMEOUT,50000);
			request.getParams().setParameter("http.protocol.handle-redirects", false);
			request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:39.0) Gecko/20100101 Firefox/39.0");
			HttpResponse response;
			response = client.execute(request);
			String responseText = HttpUtils.getStringFromResponse(response);
			request.abort();
			client.getConnectionManager().shutdown();
			if(response.getFirstHeader("Location").getValue().contains("pagenotfound")){
				HashMap<String, String> headers = new HashMap<String, String>();
				headers.put("Cookie", CrawSina.Cookie);
				url = "http://weibo.com/"+uid+"/about";
				responseText = getMethod(url, headers);
				ParserUserInfo.officialInfo(responseText, uid);
				return responseText;
			}
			if(!responseText.contains("error_back") && !responseText.contains("http://weibo.com/sorry"))
				ParserUserInfo.userInfo(responseText, uid);
			return responseText;
		}catch (IOException e) {
			e.printStackTrace();
			System.out.println("连接错误:"+url);
			getUserInfo(uid);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return "error.";
	}
	public List<SinaHotWord> getHotList(String key){
		List<SinaHotWord> hotList = null;
		String url = "";
		if(key.equals("realtimehot"))
			url = "http://s.weibo.com/top/summary?cate=realtimehot";
		else
			url = "http://s.weibo.com/top/summary?cate=total&key=" + key;
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put("Cookie", CrawSina.Cookie);
		String responseText = getMethod(url, headers);
		hotList = ParserHotList.parserHotList(key, responseText);
		return hotList;
	}
	

	/**
	 * @author whp
	 * @param dataType:风云榜时间类型 day week month
	 * @return 具体时间 yyyyMMdd
	 */
	public String getBangTime(String dataType){
		String data = "";
		if(dataType.equals("day")){
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, -1);
			data = new SimpleDateFormat("yyyyMMdd").format(cal.getTime());
		}
		else if(dataType.equals("week")){
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, -1*7);
			cal.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
			data = new SimpleDateFormat("yyyyMMdd").format(cal.getTime());
		}
		else{
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.add(Calendar.MONTH, -1);
	        data = new SimpleDateFormat("yyyyMMdd").format(cal.getTime());
		}
		return data;
	}
	/**由于用户拥有个性域名，通过拼凑的url获取重定向的个性域名
	 * @author whp
	 * @param url  拼凑的url
	 * @return  个性域名路径
	 */
	public String getSpecialUrl(String url){
		HttpClient client = new DefaultHttpClient();
		String cookie = CrawSina.Cookie;
		HttpGet request = new HttpGet(url);
		String retUrl = "";
		request.setHeader("Cookie", cookie);
		client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,50000);
		client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,50000);
		request.getParams().setParameter(HttpMethodParams.SO_TIMEOUT,50000);
		request.getParams().setParameter("http.protocol.handle-redirects", false);
		request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:39.0) Gecko/20100101 Firefox/39.0");
		try {
			HttpResponse response = client.execute(request);
			retUrl = "http://weibo.com" + response.getFirstHeader("Location").getValue(); 
			request.abort();
			client.getConnectionManager().shutdown();
		} catch (ClientProtocolException e) { 
			e.printStackTrace();
		} catch (IOException e) { 
			e.printStackTrace();
		}
		return retUrl;
	}
	/**
	 * 处理HttpGet请求
	 * @param url url地址
	 * @param headers 请求头部
	 * @return  返回请求正文html
	 */
	public String getMethod(String url, HashMap<String, String> headers){
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(url);
		for(String key:headers.keySet()){
			request.addHeader(key, headers.get(key));
        }
		request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:39.0) Gecko/20100101 Firefox/39.0");
		client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,50000);
		client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,50000);
		request.getParams().setParameter(HttpMethodParams.SO_TIMEOUT,50000);
		String responseText = "";
		try {
			HttpResponse response = client.execute(request);
			responseText = HttpUtils.getStringFromResponse(response);
		} catch (ClientProtocolException e) { 
			e.printStackTrace();
		} catch (IOException e) { 
			e.printStackTrace();
		}
		request.abort();
		client.getConnectionManager().shutdown();
		return responseText;
	}
}
