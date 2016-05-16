package crawler.sina.dispatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import jdbc.MySQL.control.JdbcConnection;

import org.apache.http.client.ClientProtocolException;

import crawler.sina.craw.CrawSina;

public class BangDispatch {
	/**
	 * 榜单信息爬取过程,收集榜单用户列表存入数据库，
	 * 并将榜单上的人的前100个粉丝信息抓取到，存入数据库
	 * 主要用于控制整个流程调度
	 * @author whp
	 * @param bangType 榜单类型 eg. renwu shishang
	 * @param dataType 时间类型 eg. day week month
	 */
	public static void bangUserFans(String bangType, String dataType){
		CrawSina crawSina = new CrawSina();
		JdbcConnection jdbcConnection = new JdbcConnection();
		List<String> bangUidList = crawSina.getBangList(bangType, dataType);
		for(int loopInBangList=0; loopInBangList<bangUidList.size(); loopInBangList++){
			for(int pageNum=1; pageNum<=5; pageNum++){
				List<String> fansUidList = crawSina.getFansListByUid(bangUidList.get(loopInBangList), String.valueOf(pageNum));
				jdbcConnection.userFansList(bangUidList.get(loopInBangList), fansUidList);
				if(fansUidList.size() != 0){
					for(int loopInFansList=0; loopInFansList<fansUidList.size(); loopInFansList++){
						crawSina.getUserInfo(fansUidList.get(loopInFansList));
					}
				}
				else{
					break;
				}
			}
		}
	}
	/**
	 * 将所有排行榜的前100人抓取到，并存入数据库
	 * @param dataType 时间类型 eg.day week month
	 */
	public static void bang(){
		CrawSina crawSina = new CrawSina();
		JdbcConnection jdbcConnection = new JdbcConnection();
		HashMap<String, String> bangType = new HashMap<String, String>();
		String dataType[] = {"day","week","month"};
		bangType.put("renwu", "0");bangType.put("yisheng", "2");bangType.put("shishang", "3");
		bangType.put("jiaoyu", "4");bangType.put("licai", "5");bangType.put("jianshen", "6");
		bangType.put("lvxing", "7");bangType.put("qinggan", "8");bangType.put("dianying", "9");
		for(int iLoop=0; iLoop<3; iLoop++)
			for(Entry<String, String> entry : bangType.entrySet()){
				List<String> userList = crawSina.getBangList(entry.getValue(), dataType[iLoop]);
				jdbcConnection.bangList(entry.getKey(), dataType[iLoop], userList);
			}
		
	}
}
