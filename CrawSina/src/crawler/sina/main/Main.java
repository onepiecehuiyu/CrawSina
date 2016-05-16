package crawler.sina.main;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import crawler.sina.craw.CrawSina;
import crawler.sina.login.Constant;
import crawler.sina.login.LoginSina;

public class Main {

	public static HttpClient client = new DefaultHttpClient();
	/**
	 * @author whp
	 * @param args
	 */
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		LoginSina ls = new LoginSina(Constant.weiboUsername, Constant.weiboPassword);
		ls.dologinSina();
//		BangDispatch.bang();
//		BangDispatch.bangUserFans("shishang", "day");
		CrawSina crawSina = new CrawSina();
//		crawSina.getHotList("all");
//		crawSina.getHotList("realtimehot");
		// 获取用户信息
//			String userMain = crawSina.getUserInfo("2778292197");
		// 获取风云榜类别排行榜信息
//			String bangInfo = crawSina.getBangList("shishang","month");
		// 获取关注的人列表
//			String followList = crawSina.getFollowListByUid("3125046087", "1");
		// 获取粉丝列表
//			crawSina.getFansListByUid("3125046087", "2");
		// 获得用户发布的微博信息 
			String context1 = crawSina.getUserMessage("1275280670", "1", "2");
//			String context2 = crawSina.getUserMessage("2293577434", "1", "1");
//			String context3 = crawSina.getUserMessage("1742987497", "2", "3");
		client.getConnectionManager().shutdown();
		long endTime = System.currentTimeMillis();
		long useTime = endTime - startTime;
		System.out.println("共用时:" + useTime);
	}

}
