package crawler.sina.login;

import java.io.IOException;
import java.io.InputStream; 
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import crawler.sina.craw.CrawSina;
import crawler.sina.main.Main;
import crawler.sina.utils.Base64Encoder;
import crawler.sina.utils.EncodeUtils;
import crawler.sina.utils.HttpUtils;
import crawler.sina.utils.JsonUtils;
import crawler.sina.utils.PreLoginResponseMessage;

public class LoginSina {
    private String username;
    private String password;
    private String rsakv;
    private String pubkey;
    
    //servertime和nonce都是在登录时需要使用的,用于post信息的加密
    private String servertime;//服务器的时间
    private String nonce;//一次性字符串
    private String userid;//用户微博ID
    private String pcid;//若需要输入验证码时用到
    private String userdomainname;//用于域名
    private String door;//验证码
    
    private Map<String,String> headers=null;
    
    private List<Cookie> cookies=null;
    
    
    public LoginSina(String username,String password){
        this.username=username;
        this.password=password;
        init();
    }
    
    public Map<String,String> getHeaders(){
        Map<String,String> hds=null;
        if(headers!=null && headers.keySet().size()>0){
            hds=new HashMap<String,String>();
            for(String key:headers.keySet()){
                hds.put(key,headers.get(key));
            }
        }
        return hds;
    }
    
    public List<Cookie> getCookies(){
        List<Cookie> cc=null;
        if(cookies!=null && cookies.size()>0){
            cc=new ArrayList<Cookie>();
            for(int i=0;i<cookies.size();i++){
                cc.add(cookies.get(i));
            }
        }
        return cc;
    }
    //登录微博
    public void dologinSina(){
        String url="http://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.5)";//v1.3.17
        Map<String,String> headers=new HashMap<String,String>();
        Map<String,String> params=new HashMap<String,String>();
  
        headers.put("Accept", "text/html, application/xhtml+xml, */*");//Accept: */*\r\n
        headers.put("Referer", "http://login.sina.com.cn/member/my.php?entry=sso");
        headers.put("Accept-Language", "zh-cn");
        headers.put("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; BOIE9;ZHCN");
        headers.put("Host", "login.sina.com.cn");
        headers.put("Connection", "Keep-Alive");
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Cache-Control", "no-cache");
        params.put("encoding", "UTF-8");
        params.put("entry", "weibo");
        params.put("from", "");
        params.put("prelt", "112");
        params.put("gateway", "1");
        params.put("nonce", nonce);
        params.put("pwencode", "rsa2");//wsse
        params.put("returntype", "META");
        params.put("pagerefer", "");
        params.put("savestate", "7");    
        params.put("servertime", servertime);
        params.put("rsakv", rsakv);
        params.put("service", "miniblog");
        params.put("sp", getEncryptedP());//encode the password
        params.put("ssosimplelogin", "1");
        params.put("su", getEncodedU());//encode the username
        params.put("url", "http://weibo.com/ajaxlogin.php?framelogin=1&callback=parent.sinaSSOController.feedBackUrlCallBack");
        params.put("useticket", "1");
        params.put("vsnf", "1");
        HttpResponse response=HttpUtils.doPost(url, headers, params);
        this.cookies=HttpUtils.getResponseCookies(response);
        this.headers=headers;
        String responseText=HttpUtils.getStringFromResponse(response, "GBK");
        String regex = "ticket=(.*)&";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(responseText);
   		while (matcher.find()) {
   			CrawSina.ticket = matcher.group(1);
   		}
   		getLoginCookie();
    }
    public void getLoginCookie(){
    	String url = "http://passport.weibo.com/wbsso/login?ticket="+CrawSina.ticket+"&url="+CrawSina.loginUrl+"&ssosavestate=1468932314&retcode=0";
    	HttpGet request = new HttpGet(url);
    	request.getParams().setParameter("http.protocol.allow-circular-redirects", false);
		HttpResponse response;
		try {
			request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:39.0) Gecko/20100101 Firefox/39.0");
			request.getParams().setParameter("http.protocol.handle-redirects", false);
			HttpClient client = new DefaultHttpClient();
			response = client.execute(request);
			HttpUtils.getResponseCookies(response);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    //对用户名进行编码
    private String getEncodedU() {
        if(username!=null && username.length()>0){
            return Base64Encoder.encode(EncodeUtils.encodeURL(username,"utf-8").getBytes());
        }
        return "";
    }
    //对密码进行编�?
    private String getEncryptedP(){
//        return EncodeSuAndSp.getEncryptedP(password, servertime, nonce);
        String data=servertime+"\t"+nonce+"\n"+password;
        String spT=rsaCrypt(pubkey, "10001", data);
        return spT;
    }
    public static String rsaCrypt(String pubkey, String exponentHex, String messageg) {
        KeyFactory factory=null;
        try {
            factory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e1) {
            return "";
        }
        BigInteger publicExponent = new BigInteger(pubkey, 16); /* public exponent */
        BigInteger modulus = new BigInteger(exponentHex, 16); /* modulus */
        RSAPublicKeySpec spec = new RSAPublicKeySpec(publicExponent, modulus);
        RSAPublicKey pub=null;
        try {
            pub = (RSAPublicKey) factory.generatePublic(spec);
        } catch (InvalidKeySpecException e1) {
            return "";
        }
        Cipher enc=null;
        byte[] encryptedContentKey =null;
        try {
            enc = Cipher.getInstance("RSA");
            enc.init(Cipher.ENCRYPT_MODE, pub);
            encryptedContentKey = enc.doFinal(messageg.getBytes());
        } catch (NoSuchAlgorithmException e1) {
            System.out.println(e1.getMessage());
            return "";
        } catch (NoSuchPaddingException e1) {
            System.out.println(e1.getMessage());
            return "";
        } catch (InvalidKeyException e1) {
            System.out.println(e1.getMessage());
            return "";
        } catch (IllegalBlockSizeException e1) {
            System.out.println(e1.getMessage());
            return "";
        } catch (BadPaddingException e1) {
            System.out.println(e1.getMessage());
            return "";
        } 
        return new String(Hex.encodeHex(encryptedContentKey));
    }
    private void init(){
        String url=compositeUrl();
        Map<String,String> headers=new HashMap<String,String>();
        headers.put("Accept", "*/*");
        headers.put("Referer", "http://weibo.com/");
        headers.put("Accept-Language", "zh-cn");
        headers.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; QQDownload 691)");
        headers.put("Host", "login.sina.com.cn");
        headers.put("Connection", "Keep-Alive");        
        HttpResponse response=HttpUtils.doGet(url, headers);
        String responseText=HttpUtils.getStringFromResponse(response);
        int begin=responseText.indexOf("{");
        int end=responseText.lastIndexOf("}");
        responseText=responseText.substring(begin,end+1);
        PreLoginResponseMessage plrmsg =JsonUtils.jsontoPreLoginResponseMessage(responseText);
        this.nonce=plrmsg.getNonce();
        this.servertime=plrmsg.getServertime()+"";
        this.pubkey=plrmsg.getPubkey();
        this.rsakv=plrmsg.getRsakv();
        this.pcid=plrmsg.getPcid();
    }
    //组合预登陆时的URL
    private String compositeUrl(){
        StringBuilder builder=new StringBuilder();
        builder.append("http://login.sina.com.cn/sso/prelogin.php?")
           .append("entry=weibo&callback=sinaSSOController.preloginCallBack&")
           .append("su="+getEncodedU())
           .append("&rsakt=mod&checkpin=1&client=ssologin.js(v1.4.5)&_="+System.currentTimeMillis());
        return builder.toString();
    }
}