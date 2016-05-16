package crawler.sina.bean;

import java.util.ArrayList;
import java.util.List;

public class SinaUserMessage {
	
	//用户uid
	public String uid;
	
	//用户发布的信息
	public String message;
	
	//发布时间
	public String time;
	
	//是否为转发信息
	public boolean isRepost;
	
	//若为转发信息 记录原始url地址，否则为空
	public String url;
	
	//若为转发信息 记录转发uid链，逆序记录 eg.A用户转发B用户信息 list[0]为A用户 list[1]为B用户
	public List<String> repostList = new ArrayList<String>();
	
	//是否为长微博
	public boolean isLongMessage;
	
	//若为长微博，记录长微博内容
	public String longMessage;
	
}
