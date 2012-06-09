package jp.ddo.dekuyou.liveview.plugins.twittertl2;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import twitter4j.Paging;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.http.AccessToken;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.sonyericsson.extras.liveview.plugins.AbstractPluginService;
import com.sonyericsson.extras.liveview.plugins.PluginConstants;
import com.sonyericsson.extras.liveview.plugins.PluginUtils;

public class TwitterTLPluginService extends AbstractPluginService {

	private static final int READ_TIMEOUT = 5 * 1000;

	private static final int CONNECT_TIMEOUT = 2 * 1000;

	// Our handler.
	private Handler mHandler = null;

	// private WakeLock wakelock;

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		// Create handler.
		if (mHandler == null) {
			mHandler = new Handler();
		}

	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		stopWork();
	}

	boolean sandbox = true;

	/**
	 * Plugin is sandbox.
	 */
	protected boolean isSandboxPlugin() {
		return sandbox;
	}

	/**
	 * Must be implemented. Starts plugin work, if any.
	 */
	protected void startWork() {

		mLiveViewAdapter.screenOn(mPluginId);
		PluginUtils.sendTextBitmap(mLiveViewAdapter, mPluginId, "TwitTL!", 128,
				10);

		new Thread(new Runnable() {
			@Override
			public void run() {
				TwitTLBean bean = TwitTLBean.getInstance();

				InputStream in;
				try {
					in = openFileInput(Const.FILE_NAME);
					ObjectInputStream ois;
					ois = new ObjectInputStream(in);
					bean.setAccessToken((AccessToken) ois.readObject());

					ConfigurationBuilder cb = new ConfigurationBuilder();

					cb.setHttpConnectionTimeout(CONNECT_TIMEOUT);
					cb.setHttpReadTimeout(READ_TIMEOUT);

					bean.setTwitter(new TwitterFactory(cb.build())
							.getOAuthAuthorizedInstance(Const.CONSUMER_KEY,
									Const.CONSUMER_SERCRET, bean
											.getAccessToken()));
					if (bean.getTwitter() == null) {
						throw new FileNotFoundException();
					}

					if (((bean.getStatuses() == null && (TL.HOME.equals(bean
							.getNowMode()) || TL.Mentions.equals(bean
							.getNowMode()))) || (bean.getDms() == null && TL.DMs
							.equals(bean.getNowMode())))
							|| mSharedPreferences.getBoolean(
									"ReloadAtTheStart", true)) {
						bean.setStatuses(null);
						bean.setDms(null);
						bean.setNo(0);
						bean.setPaging(new Paging(1, Const.ROWS));
						bean.setPage(1);

						getTimeline(bean.getPaging());
					}

					bean.setMsgrow(0);
					makeMsgBitmap();

					doDraw();

					fail = false;

				} catch (FileNotFoundException e) {
					// 
					mHandler.postDelayed(new Runnable() {
						public void run() {
							// First message to LiveView
							try {
								mLiveViewAdapter.clearDisplay(mPluginId);
							} catch (Exception e) {
								Log.e(e);
							}
							PluginUtils.sendTextBitmap(mLiveViewAdapter,
									mPluginId, "Please do OAuth!", 128,
									Const.TITE_DELAY);
						}
					}, Const.TITE_DELAY);
				} catch (Exception e) {

					Log.e(e);
					mHandler.postDelayed(new Runnable() {
						public void run() {
							// First message to LiveView
							try {
								mLiveViewAdapter.clearDisplay(mPluginId);
							} catch (Exception e) {
								Log.e(e);
							}
							PluginUtils.sendTextBitmap(mLiveViewAdapter,
									mPluginId, "network is unavailable!", 128,
									Const.TITE_DELAY);
						}
					}, Const.TITE_DELAY);

					fail = true;
				}

			}
		}).start();

	}

	private void getTimeline(Paging paging) throws TwitterException {

		mHandler.postDelayed(new Runnable() {
			public void run() { //
				try {
					mLiveViewAdapter.vibrateControl(mPluginId, 0, 50);
				} catch (Exception e) {
					Log.e(e);
				}
			}
		}, Const.TITE_DELAY);

		TwitTLBean bean = TwitTLBean.getInstance();

		switch (bean.getNowMode()) {
		case HOME:
			if (bean.getStatuses() == null) {
				bean.setStatuses(bean.getTwitter().getHomeTimeline(paging));

			} else {
				bean.getStatuses().addAll(
						bean.getTwitter().getHomeTimeline(paging));
			}
			break;
		case Mentions:
			if (bean.getStatuses() == null) {
				bean.setStatuses(bean.getTwitter().getMentions(paging));

			} else {
				bean.getStatuses()
						.addAll(bean.getTwitter().getMentions(paging));
			}
			break;
		case DMs:
			if (bean.getDms() == null) {
				bean.setDms(bean.getTwitter().getDirectMessages(paging));

			} else {
				bean.getDms().addAll(
						bean.getTwitter().getDirectMessages(paging));
			}
			break;

		case Lists:

			if (slugId == null) {
				for (UserList ulist : bean.getTwitter().getUserLists(
						bean.getTwitter().getScreenName(), -1)) {

					Log.d(ulist.getSlug());
					if ("twittl".equals(ulist.getSlug())) {
						slugId = ulist.getId();
						Log.d(String.valueOf(ulist.getId()));
						break;

					}
				}

			}
			if (slugId != null) {

				if (bean.getStatuses() == null) {
					bean.setStatuses(bean.getTwitter().getUserListStatuses(
							bean.getTwitter().getScreenName(), slugId, paging));

				} else {
					bean.getStatuses().addAll(
							bean.getTwitter().getUserListStatuses(
									bean.getTwitter().getScreenName(), slugId,
									paging));
				}
				break;
			}
		default:
			bean.setNowMode(TL.HOME);
			if (bean.getStatuses() == null) {
				bean.setStatuses(bean.getTwitter().getHomeTimeline(paging));

			} else {
				bean.getStatuses().addAll(
						bean.getTwitter().getHomeTimeline(paging));
			}
			break;
		}

	}

	Integer slugId = null;

	private void doDraw() {
		TwitTLBean bean = TwitTLBean.getInstance();
		Bitmap drawBitmap = null;
		try {

			Log.d("bitmap.getHeight():   " + bean.getBitmap().getHeight());
			Log.d("bitmap.getRowBytes(): " + bean.getBitmap().getRowBytes());
			Log.d("bitmap.getDensity(): " + bean.getBitmap().getDensity());

			int scrollpixel = 16;

			if (bean.getBitmap().getHeight() < scrollpixel
					* (bean.getMsgrow() + 1) + 128
					&& bean.getMsgrow() > 0) {
				bean.setMsgrow(bean.getMsgrow() - 1);
				return;

			}
			Log.d("msgrow: " + bean.getMsgrow());

			drawBitmap = Bitmap.createBitmap(bean.getBitmap(), 0, scrollpixel
					* bean.getMsgrow(), 128, 128);

			bean.setDrawBitmap(drawBitmap);

		} catch (IllegalArgumentException e) {
			Log.e(e);
			return;
		}

		mHandler.postDelayed(new Runnable() {
			public void run() { //
				try {
					TwitTLBean bean = TwitTLBean.getInstance();

					Bitmap sendBitmap1 = Bitmap.createBitmap(bean
							.getDrawBitmap(), 0, 0, 128, 64);

					mLiveViewAdapter.sendImageAsBitmap(mPluginId, 0, 0,
							sendBitmap1);

					sendBitmap1.recycle();
				} catch (Exception e) {
					Log.e(e);
				}

			}
		}, Const.DRAW_MSG_DELAY);

		// mHandler.postDelayed(new Runnable() {
		// public void run() { //
		// try {
		// TwitTLBean bean = TwitTLBean.getInstance();
		//
		// Bitmap sendBitmap2 = Bitmap.createBitmap(
		// bean.getDrawBitmap(), 0, 32, 128, 32);
		//
		// mLiveViewAdapter.sendImageAsBitmap(mPluginId, 0, 32,
		// sendBitmap2);
		//					
		// sendBitmap2.recycle();
		// } catch (Exception e) {
		// Log.e(e);
		// }
		//
		//
		// }
		// }, Const.DRAW_MSG_DELAY2);

		mHandler.postDelayed(new Runnable() {
			public void run() { //
				try {
					TwitTLBean bean = TwitTLBean.getInstance();

					Bitmap sendBitmap2 = Bitmap.createBitmap(bean
							.getDrawBitmap(), 0, 64, 128, 64);

					mLiveViewAdapter.sendImageAsBitmap(mPluginId, 0, 64,
							sendBitmap2);

					sendBitmap2.recycle();
				} catch (Exception e) {
					Log.e(e);
				}

			}
		}, Const.DRAW_MSG_DELAY3);

		// mHandler.postDelayed(new Runnable() {
		// public void run() { //
		// try {
		// TwitTLBean bean = TwitTLBean.getInstance();
		//
		// Bitmap sendBitmap2 = Bitmap.createBitmap(
		// bean.getDrawBitmap(), 0, 96, 128, 32);
		//
		// mLiveViewAdapter.sendImageAsBitmap(mPluginId, 0, 96,
		// sendBitmap2);
		//					
		// sendBitmap2.recycle();
		// } catch (Exception e) {
		// Log.e(e);
		// }
		//
		//
		// }
		// }, Const.DRAW_MSG_DELAY4);

		mHandler.postDelayed(new Runnable() {
			public void run() { //
				mLiveViewAdapter.screenOnAuto(mPluginId);

			}
		}, Const.TITE_DELAY);

	}

	private void makeMsgBitmap() throws IllegalStateException, TwitterException {
		TwitTLBean bean = TwitTLBean.getInstance();

		String modeString = "";
		String screenNameString = "";
		String msgString = "";
		String msgtime = "";
		User user = null;
		boolean isRt = false;
		switch (bean.getNowMode()) {
		case HOME:
			if (bean.getStatuses() == null || bean.getStatuses().size() == 0) {
				return;
			}
			if (bean.getStatuses().get(bean.getNo()).isRetweet()) {
				user = bean.getStatuses().get(bean.getNo())
						.getRetweetedStatus().getUser();
				isRt = true;
			} else {
				user = bean.getStatuses().get(bean.getNo()).getUser();

			}
			modeString = "Home";
			screenNameString = user.getScreenName();
			msgString = bean.getStatuses().get(bean.getNo()).getText();
			msgtime = bean.getStatuses().get(bean.getNo()).getCreatedAt()
					.toLocaleString();
			break;

		case Mentions:
			if (bean.getStatuses() == null || bean.getStatuses().size() == 0) {
				return;
			}
			// modeString = "@"+twitter.getScreenName();
			modeString = "@Mt";
			screenNameString = bean.getStatuses().get(bean.getNo()).getUser()
					.getScreenName();
			msgString = bean.getStatuses().get(bean.getNo()).getText();
			msgtime = bean.getStatuses().get(bean.getNo()).getCreatedAt()
					.toLocaleString();
			user = bean.getStatuses().get(bean.getNo()).getUser();
			break;
		case DMs:
			if (bean.getDms() == null || bean.getDms().size() == 0) {
				return;
			}
			modeString = "DMs";
			screenNameString = bean.getDms().get(bean.getNo())
					.getSenderScreenName();
			msgString = bean.getDms().get(bean.getNo()).getText();
			msgtime = bean.getDms().get(bean.getNo()).getCreatedAt()
					.toLocaleString();
			user = bean.getDms().get(bean.getNo()).getSender();
			break;
		case Lists:
			if (bean.getStatuses() == null || bean.getStatuses().size() == 0) {
				return;
			}
			modeString = "List/twittl";
			screenNameString = bean.getStatuses().get(bean.getNo()).getUser()
					.getScreenName();
			msgString = bean.getStatuses().get(bean.getNo()).getText();
			msgtime = bean.getStatuses().get(bean.getNo()).getCreatedAt()
					.toLocaleString();
			user = bean.getStatuses().get(bean.getNo()).getUser();
			break;
		default:
			break;
		}

		// Msg String
		String msg = msgtime + "\n" + msgString.replaceAll("\n", " ");

		Log.d("mSharedPreferences.getString(FontSize, 12): "
				+ mSharedPreferences.getString("FontSize", "12"));

		// Set the text properties in the canvas
		TextPaint textPaint = new TextPaint();
		textPaint.setTextSize(new Integer(mSharedPreferences.getString(
				"FontSize", "12")));

		textPaint.setColor(Color.WHITE);

		// Create the text layout and draw it to the canvas
		StaticLayout textLayout = new StaticLayout(msg, textPaint, 128,
				Layout.Alignment.ALIGN_NORMAL, 1, 0, false);

		Log.d("textLayout.getLineCount(): " + textLayout.getLineCount());
		Log.d("textPaint.getFontSpacing(): " + textPaint.getFontSpacing());

		BigDecimal bitmaphight = new BigDecimal("128");
		BigDecimal fonthight = new BigDecimal(new Double(textPaint
				.getFontSpacing()).toString()).setScale(0, BigDecimal.ROUND_UP);
		BigDecimal linehight = new BigDecimal(new Long(textLayout
				.getLineCount() + 2).toString()).multiply(fonthight);
		Log.d("textLayout.getLineCount() * textPaint.getFontSpacing() :"
				+ linehight.toString());
		BigDecimal bitmaprows = linehight.divide(bitmaphight, 0,
				BigDecimal.ROUND_UP);
		Log.d("bitmaprows :" + bitmaprows);

		Bitmap msgbitmap = null;

		try {
			msgbitmap = Bitmap.createBitmap(128, bitmaprows.multiply(
					new BigDecimal("128")).intValue(), Bitmap.Config.RGB_565);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return;
		}

		Canvas canvas1 = new Canvas(msgbitmap);
		textLayout.draw(canvas1);

		Log.d("bitmap.getScaledHeight(canvas): "
				+ msgbitmap.getScaledHeight(canvas1));

		// screen name

		String username = modeString
				+ " "
				+ new Integer(bean.getNo()).toString()
				+ " :"
				+ (isRt ? "RT" : "")
				+ "\n"
				+ screenNameString
				+ (isRt ? " : "
						+ bean.getStatuses().get(bean.getNo()).getUser()
								.getScreenName() : "");

		StaticLayout textLayout2 = new StaticLayout(username, textPaint, 128,
				Layout.Alignment.ALIGN_NORMAL, 1, 0, false);

		Bitmap usernamebitmap = null;
		try {
			usernamebitmap = Bitmap.createBitmap(128, fonthight.multiply(
					new BigDecimal("2")).intValue(), Bitmap.Config.RGB_565);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return;
		}

		Log.d("bitmaphights :" + msgbitmap.getHeight()
				+ usernamebitmap.getHeight());

		try {
			bean.setBitmap(Bitmap.createBitmap(128, msgbitmap.getHeight()
					+ usernamebitmap.getHeight(), Bitmap.Config.RGB_565));
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return;
		}

		Canvas canvas = new Canvas(bean.getBitmap());

		Canvas canvas2 = new Canvas(usernamebitmap);
		textLayout2.draw(canvas2);

		BitmapDrawable drawable2 = new BitmapDrawable(usernamebitmap);
		drawable2
				.setBounds(
						fonthight.multiply(new BigDecimal("2")).intValue() + 1,
						0, usernamebitmap.getWidth()
								+ (fonthight.multiply(new BigDecimal("2"))
										.intValue() + 1), usernamebitmap
								.getHeight());
		drawable2.draw(canvas);

		BitmapDrawable drawable3 = new BitmapDrawable(msgbitmap);
		drawable3.setBounds(0, usernamebitmap.getHeight(),
				msgbitmap.getWidth(), msgbitmap.getHeight()
						+ usernamebitmap.getHeight());
		drawable3.draw(canvas);

		bean.setFonthight(fonthight);
		bean.setUser(user);

		msgbitmap.recycle();
		usernamebitmap.recycle();

		// icon
		if (bean.getProfileImg().containsKey(new Integer(user.getId()))) {

			BitmapDrawable drawable = new BitmapDrawable(bean.getProfileImg()
					.get(new Integer(user.getId())));
			drawable.setBounds(0, 0, bean.getFonthight().multiply(
					new BigDecimal("2")).intValue(), bean.getFonthight()
					.multiply(new BigDecimal("2")).intValue());
			drawable.draw(canvas);

		} else {

			mHandler.postDelayed(new Runnable() {
			public void run() {
//			new Thread(new Runnable() {
//				public void run() {
					// First message to LiveView

					TwitTLBean bean = TwitTLBean.getInstance();

					Canvas canvas = new Canvas(bean.getBitmap());

					BitmapDrawable drawable = new BitmapDrawable(
							getProfileImage(bean.getUser()));
					drawable.setBounds(0, 0, bean.getFonthight().multiply(
							new BigDecimal("2")).intValue(), bean
							.getFonthight().multiply(new BigDecimal("2"))
							.intValue());
					drawable.draw(canvas);

					if (bean.getMsgrow() == 0) {

//						mHandler.postDelayed(new Runnable() {
//							public void run() {

//								TwitTLBean bean = TwitTLBean.getInstance();

								mLiveViewAdapter.sendImageAsBitmap(mPluginId,
										0, 0, Bitmap.createBitmap(bean
												.getBitmap(), 0, 0, bean
												.getFonthight().multiply(
														new BigDecimal("2"))
												.intValue(), bean
												.getFonthight().multiply(
														new BigDecimal("2"))
												.intValue()));

//							}
//						}, Const.DRAW_ICON_DELAY);
					}

				}

				private Bitmap getProfileImage(User user) {
					Bitmap icon = null;
					TwitTLBean bean = TwitTLBean.getInstance();

					if (bean.getProfileImg().containsKey(
							new Integer(user.getId()))) {
						return bean.getProfileImg().get(
								new Integer(user.getId()));

					}

					// get icon
					HttpURLConnection c = null;
					InputStream is = null;
					try {
						// HTTP接続のオープン
						URL url = user.getProfileImageURL();
						c = (HttpURLConnection) url.openConnection();
						c.setRequestMethod("GET");
						c.setConnectTimeout(CONNECT_TIMEOUT);
						c.setReadTimeout(READ_TIMEOUT);
						c.connect();
						is = c.getInputStream();

						icon = BitmapFactory.decodeStream(is);

						// HTTP接続のクローズ
						is.close();
						c.disconnect();

					} catch (Exception e) {
						try {
							if (c != null)
								c.disconnect();
							if (is != null)
								is.close();
						} catch (Exception e2) {
						}
					} finally {

					}

					if (bean.getProfileImg().size() >= 5 && icon != null) {
						Set<Integer> keySet = bean.getProfileImg().keySet(); // すべてのキー値を取得
						Iterator<Integer> keyIte = keySet.iterator();
						keyIte.hasNext();
						Integer key = (Integer) keyIte.next();

						bean.getProfileImg().remove(key);
					}

					if (icon != null) {

						bean.getProfileImg().put(new Integer(user.getId()),
								icon);
					}

					return icon;
				}

//			});
		
			}, Const.DRAW_ICON_DELAY);

		}

	}

	/**
	 * Must be implemented. Stops plugin work, if any.
	 */
	protected void stopWork() {

	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has done connection and registering to the LiveView
	 * Service.
	 * 
	 * If needed, do additional actions here, e.g. starting any worker that is
	 * needed.
	 */
	protected void onServiceConnectedExtended(ComponentName className,
			IBinder service) {

	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has done disconnection from LiveView and service has been
	 * stopped.
	 * 
	 * Do any additional actions here.
	 */
	protected void onServiceDisconnectedExtended(ComponentName className) {

	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has checked if plugin has been enabled/disabled.
	 * 
	 * The shared preferences has been changed. Take actions needed.
	 */
	protected void onSharedPreferenceChangedExtended(SharedPreferences prefs,
			String key) {

	}

	protected void startPlugin() {
		Log.d("startPlugin");

		wakeUp();

		startWork();
	}

	protected void stopPlugin() {
		Log.d("stopPlugin");

		toSleep();

		stopWork();
	}

	boolean fail;

	protected void button(String buttonType, boolean doublepress,
			boolean longpress) {
		Log.d("button - type " + buttonType + ", doublepress " + doublepress
				+ ", longpress " + longpress);

		TwitTLBean bean = TwitTLBean.getInstance();
		if (bean.getTwitter() == null) {
			return;
		}

		switch (bean.getNowMode()) {
		case DMs:
			if (bean.getDms() == null) {
				return;
			}

			break;

		default:
			if (bean.getStatuses() == null) {
				return;
			}
			break;
		}

		if (fail) {
			bean.setNo(0);
			buttonType = PluginConstants.BUTTON_LEFT;
			fail = false;
		}

		try {

			if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_UP)) {
				// UP Scroll
				if (bean.getMsgrow() > 0) {
					bean.setMsgrow(bean.getMsgrow() - 1);
					doDraw();
				}

			} else if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_DOWN)) {
				// DOWN Scroll

				bean.setMsgrow(bean.getMsgrow() + 1);
				doDraw();

			} else if (buttonType
					.equalsIgnoreCase(PluginConstants.BUTTON_RIGHT)) {
				// Msg OLD
				switch (bean.getNowMode()) {
				case DMs:
					if (bean.getDms().size() - 1 > bean.getNo()) {
						bean.setNo(bean.getNo() + 1);
					} else {
						bean.setPage(bean.getPage() + 1);
						bean.setPaging(new Paging(bean.getPage(), Const.ROWS));
						// paging.setSinceId(dms.get(no).getId());
						getTimeline(bean.getPaging());

						bean.setNo(bean.getNo() + 1);

						if (bean.getDms().size() - 1 <= bean.getNo()) {
							bean.setNo(bean.getDms().size() - 1);

						}
						// no = statuses.size() - 1;

					}
					break;

				default:
					if (bean.getStatuses().size() - 1 > bean.getNo()) {
						bean.setNo(bean.getNo() + 1);
					} else {
						bean.setPage(bean.getPage() + 1);
						bean.setPaging(new Paging(bean.getPage(), Const.ROWS));
						// paging.setMaxId(statuses.get(no).getId());
						getTimeline(bean.getPaging());

						bean.setNo(bean.getNo() + 1);

						if (bean.getStatuses().size() - 1 <= bean.getNo()) {
							bean.setNo(bean.getStatuses().size() - 1);

						}
						// no = statuses.size() - 1;

					}
					break;
				}
				bean.setMsgrow(0);
				makeMsgBitmap();
				doDraw();

			} else if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_LEFT)) {
				// Msg NEW

				if (bean.getNo() > 0 && !longpress) {
					bean.setNo(bean.getNo() - 1);
				} else {
					bean.setNo(0);
					bean.setStatuses(null);
					bean.setDms(null);
					bean.setPage(1);
					bean.setPaging(new Paging(bean.getPage(), Const.ROWS));
					getTimeline(bean.getPaging());
				}
				bean.setMsgrow(0);
				makeMsgBitmap();

				doDraw();

			} else if (buttonType
					.equalsIgnoreCase(PluginConstants.BUTTON_SELECT)) {
				// TLChange
				mHandler.postDelayed(new Runnable() {
					public void run() {
						try {
							mLiveViewAdapter.vibrateControl(mPluginId, 0, 50);
						} catch (Exception e) {
							Log.e(e);
						}
					}
				}, Const.TITE_DELAY);

				bean.setStatuses(null);

				changeMode();

				bean.setPage(1);
				bean.setPaging(new Paging(bean.getPage(), Const.ROWS));
				bean.setNo(0);
				getTimeline(bean.getPaging());
				bean.setMsgrow(0);

				makeMsgBitmap();
				doDraw();

			}
		} catch (TwitterException e) {
			// 
			Log.e(e);
			mHandler.postDelayed(new Runnable() {
				public void run() {
					// First message to LiveView
					try {
						mLiveViewAdapter.clearDisplay(mPluginId);
					} catch (Exception e) {
						Log.e(e);
					}
					PluginUtils.sendTextBitmap(mLiveViewAdapter, mPluginId,
							"network is unavailable!", 128, 10);
				}
			}, Const.TITE_DELAY);

			fail = true;

		}

	}

	private void changeMode() {
		TwitTLBean bean = TwitTLBean.getInstance();

		Log.d("changeMode");
		switch (bean.getNowMode()) {
		case HOME:
			if (mSharedPreferences.getBoolean("MentionsEnabled", true)) {
				bean.setNowMode(TL.Mentions);
				bean.setStatuses(null);
				Log.d("change->Mentions");

				break;
			}
		case Mentions:
			if (mSharedPreferences.getBoolean("DMsEnabled", true)) {
				bean.setNowMode(TL.DMs);
				bean.setDms(null);
				Log.d("change->DMs");

				break;
			}
		case DMs:
			if (mSharedPreferences.getBoolean("ListsEnabled", false)) {
				bean.setNowMode(TL.Lists);
				bean.setStatuses(null);
				Log.d("change->Lists");

				break;
			}
		case Lists:
			if (mSharedPreferences.getBoolean("HOMETLEnabled", true)) {
				bean.setNowMode(TL.HOME);
				bean.setStatuses(null);
				Log.d("change->HOME");

				break;
			}

		default:
			bean.setNowMode(TL.HOME);
			bean.setStatuses(null);
			break;
		}
	}

	protected void displayCaps(int displayWidthPx, int displayHeigthPx) {
		Log.d("displayCaps - width " + displayWidthPx + ", height "
				+ displayHeigthPx);
	}

	protected void onUnregistered() throws RemoteException {
		Log.d("onUnregistered");
		stopWork();
	}

	protected void openInPhone(String openInPhoneAction) {
		Log.d("openInPhone: " + openInPhoneAction);
	}

	protected void screenMode(int mode) {
		Log.d("screenMode: screen is now " + ((mode == 0) ? "OFF" : "ON"));

		if (mode == PluginConstants.LIVE_SCREEN_MODE_OFF) {
			toSleep();

		} else if (mode == PluginConstants.LIVE_SCREEN_MODE_ON) {
			wakeUp();
		}
	}

	private void wakeUp() {
		// スリープしていたら解除する。
		// wakelock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
		// .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
		// | PowerManager.ACQUIRE_CAUSES_WAKEUP
		// | PowerManager.ON_AFTER_RELEASE, "disableLock");
		// wakelock.acquire();
	}

	private void toSleep() {
		// try {
		// if (wakelock != null) {
		// wakelock.release();
		// }
		// } catch (Exception e) {
		// Log.e(e);
		// }
	}

}