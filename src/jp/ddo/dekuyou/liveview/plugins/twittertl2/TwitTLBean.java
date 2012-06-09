package jp.ddo.dekuyou.liveview.plugins.twittertl2;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.User;
import twitter4j.http.AccessToken;
import android.graphics.Bitmap;
import android.graphics.Canvas;


public class TwitTLBean {
	
	
	  private static TwitTLBean instance = new TwitTLBean();

	  private TwitTLBean() {}

	  public static TwitTLBean getInstance() {
	    return instance;
	  }
	
	private List<Status> statuses = null;
	private List<DirectMessage> dms = null;
	private int no = 0;
	private AccessToken accessToken = null;
	private Twitter twitter = null;
	private Paging paging = new Paging(1, Const.ROWS);
	private int page = 1;
	private Bitmap bitmap = null;
	private int msgrow = 0;
	private TL nowMode = TL.HOME;
	
	
	private String modeString = "";
	private String screenNameString = "";
	private String msgString = "";
	private String msgtime = "";
	private User user = null;
	
	private Canvas canvas;
	
	private LinkedHashMap<Integer, Bitmap > profileImg = new LinkedHashMap<Integer, Bitmap >();
	
	
	private Bitmap drawBitmap;
	

	
	
	public LinkedHashMap<Integer, Bitmap> getProfileImg() {
		return profileImg;
	}

	public void setProfileImg(LinkedHashMap<Integer, Bitmap> profileImg) {
		this.profileImg = profileImg;
	}

	public Canvas getCanvas() {
		return canvas;
	}

	public void setCanvas(Canvas canvas) {
		this.canvas = canvas;
	}

	private 		BigDecimal fonthight;

	public BigDecimal getFonthight() {
		return fonthight;
	}

	public void setFonthight(BigDecimal fonthight) {
		this.fonthight = fonthight;
	}

	public String getModeString() {
		return modeString;
	}

	public void setModeString(String modeString) {
		this.modeString = modeString;
	}

	public String getScreenNameString() {
		return screenNameString;
	}

	public void setScreenNameString(String screenNameString) {
		this.screenNameString = screenNameString;
	}

	public String getMsgString() {
		return msgString;
	}

	public void setMsgString(String msgString) {
		this.msgString = msgString;
	}

	public String getMsgtime() {
		return msgtime;
	}

	public void setMsgtime(String msgtime) {
		this.msgtime = msgtime;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public List<Status> getStatuses() {
		return statuses;
	}

	public void setStatuses(List<Status> statuses) {
		this.statuses = statuses;
	}

	public List<DirectMessage> getDms() {
		return dms;
	}

	public void setDms(List<DirectMessage> dms) {
		this.dms = dms;
	}

	public int getNo() {
		return no;
	}

	public void setNo(int no) {
		this.no = no;
	}

	public AccessToken getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(AccessToken accessToken) {
		this.accessToken = accessToken;
	}

	public Twitter getTwitter() {
		return twitter;
	}

	public void setTwitter(Twitter twitter) {
		this.twitter = twitter;
	}

	public Paging getPaging() {
		return paging;
	}

	public void setPaging(Paging paging) {
		this.paging = paging;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	public int getMsgrow() {
		return msgrow;
	}

	public void setMsgrow(int msgrow) {
		this.msgrow = msgrow;
	}

	public TL getNowMode() {
		return nowMode;
	}

	public void setNowMode(TL nowMode) {
		this.nowMode = nowMode;
	}

	public Bitmap getDrawBitmap() {
		return drawBitmap;
	}

	public void setDrawBitmap(Bitmap drawBitmap) {
		this.drawBitmap = drawBitmap;
	}





	public void destroy(){
		instance = null;
	}

	
	
}
