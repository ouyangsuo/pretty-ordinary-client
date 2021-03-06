package com.kitty.poclient.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import com.kitty.poclient.R;
import com.kitty.poclient.activity.LoginActivity;
import com.kitty.poclient.activity.WebListenActivity;
import com.kitty.poclient.domain.Album;
import com.kitty.poclient.domain.Artist;
import com.kitty.poclient.domain.Column;
import com.kitty.poclient.domain.Music;
import com.kitty.poclient.domain.SearchDataObject;
import com.kitty.poclient.fragment.MainFragment;
import com.kitty.poclient.fragment.PlayerFragment;
import com.kitty.poclient.fragment.TabWebFragment;
import com.kitty.poclient.interfaces.OnCurrentPlayingInfoChangedListener;
import com.kitty.poclient.interfaces.OnCurrentPlayingStateChangedListener;
import com.kitty.poclient.interfaces.SelfReloader;
import com.kitty.poclient.models.PlayingInfo;
import com.kitty.poclient.models.StateModel;
import com.kitty.poclient.util.ListviewDataPositionRecorder;

public class WatchDog {

	// 用户名和密码
	public static String currentUserId = "244987";// WatchDog.currentUserId=,WatchDog.currentUserId
													// =
	public static String currentPassword = "123456";

	public static boolean getDataFinished = false;
	public static String sd = "0";
	public static String musicIndex = "";
	public static String currentDevice = "";
	// public static String currentHost = "http://192.168.1.230/";// 后台服务器地址
	public static String currentHost = "http://music1.zhenxian.fm/";// 后台服务器地址
	// public static String currentHostTemp = "http://192.168.1.120/";//
	// 后台服务器地址测试版
	public static String macAddress = "";// 物理地址
	public static int buySubState = 0;// 用户表示第一次订阅主动请求盒子
	public static Object builder;// 对话框builder对象
	public static int synBoxOrservice = 1; // 0表示是从服务器中取，1表示是从box中取
	public static Handler handler;// 主线程handler
	public static int unbindplayerstatic = 0;// 用于缓存模式取消播放状态
	public static Map<String, String> clearcachemap = new HashMap<String, String>();// 用于保存需要隐藏的商品
	public static int clearCacheProductType = 1;// 用于标识清除商品的类型
												// ，默认是1表示专辑，5表示单曲，10表示主题
	public static int reboxcontrolservice = 1;// 重连，不启动服务；如果订阅已死就要重启服务！1是用于表示第一次启动服务开启订阅
												// ,5表示已经开启过订阅
	public static boolean flag = true;// 终端操作 false表示是从控制端全部同步云端数据
	public static Activity activity;
	public static long l = 0L;
	// public static int dbdatastate=0;//if database on create or updata while
	// it's update
	public static List<Activity> currentActivities = new ArrayList<Activity>();

	public static String currentUri = "";
	public static int currentPlaymode = PlayerFragment.MODE_ORDER;

	public static ArrayList<Music> currentList;// WatchDog.currentList=,WatchDog.currentList
												// =

	public static String currentPlayingUSBDir = null; // add by Eason Fong

	public static int currentListType = -1;// WatchDog.currentListType=
	public static long currentListId = 0L;// WatchDog.currentListId=

	public static Music currentPlayingMusic;
	public static Music currentListeningMusic;// 当前试听歌曲
	public static Long currentPlayingId = 0L;
	// public static String currentPlayingMusicCacheState="";
	public static int currentPlayingIndex = -1;
	public static String currentPlayingName = "未知曲目";
	public static String currentArtistName = "";
	public static Bitmap currentPlayingAlbumPic;
	public static String dialogtext = "搜索设备";// 搜索对话框title提示
	public static String dialogcontent = "";// 搜索对话框内容提示
	public static int ifdeviceListView = 0;// 0 表示已经发现设备并加载到设备发现列表里面 1表示没有发现
	public static Object choseboxObj;// 选中的对象
	// 记录用户的最近一次操作:next,prev
	public static String latestOperation = "";
	public static int isfirstclearcache = 0; // 是否是第一次清除缓存信息
	public static HashMap<Long, String> cacheStateMap = new HashMap<Long, String>();
	public static HashMap<String, List<Long>> clearCacheStateMap = new HashMap<String, List<Long>>();// 保存需要显示的商品
	public static WatchDog instance;
	public static boolean babyNotMine = false;
	public static int isPullFlag = 1;// 是否有上拉提示
	public static boolean isBoxAlive = true;// UPNP生命属性，默认为真；
	public static boolean mediaOutOfService = false;// 当盒子在播放网络试听曲目或歌单时，屏蔽播放器；
	public static String mediaOutOfServiceReson = "";
	public static boolean keybackRefersExitPro = true;// 定义键盘返回键：true指向退出程序，false指向界面回退
	public static boolean hasNewBought = false;
	public static int upnpActionPerformCount = 0;
	public static boolean isSlidingMenuShown = false;// 记录侧滑菜单是否滑出
	public static boolean isSlidingMenuShownInTabWeb = false;// 记录侧滑菜单是否滑出（音乐商店）
	public static int previousLayout = -1;// 记录应该回退至的Fragment布局
	public static int currentTabPosition = MainFragment.TAB_MUSIC;

	public static boolean botiquesDataGot = false;// 精品聚焦主页数据是否加载完毕
	public static boolean topsDataGot = false;// TOP100主页数据是否加载完毕
	public static boolean genresDataGot = false;// 类型主页数据是否加载完毕
	public static boolean artistsDataGot = false;// 演出者主页数据是否加载完毕
	public static boolean themesDataGot = false;// 演出者主页数据是否加载完毕

	/* 记录栏目详情在跳转前的数据和位置 */
	public static List<Album> albumsInColumnDetail = null;
	public static int fvipInColumnDetail = -1;
	public static int lvipInColumnDetail = -1;
	public static int stInColumnDetail = -1;
	public static boolean flagInColumnDetail = false;

	/* 记录榜单详情在跳转前的数据和位置 */
	public static List<Album> albumsInTopDetail = null;
	public static int fvipInTopDetail = -1;
	public static int lvipInTopDetail = -1;
	public static int stInTopDetail = -1;
	public static boolean flagInTopDetail = false;

	public static HashMap<String, ListviewDataPositionRecorder> listviewPositionMap = new HashMap<String, ListviewDataPositionRecorder>();

	/* 记录搜索结果在跳转前的数据和位置 */
	public static SearchDataObject sdoInSearchResult = null;
	public static String selectedSearchResultTabText = "全部";
	// public static Integer fvipInSearchResult = -1;
	// public static Integer stInSearchResult = -1;
	public static boolean flagInSearchResult = false;
	public static String keywordInSearchResult = "请输入搜索关键字";

	/* 记录精品聚焦在跳转前的数据和位置 */
	public static List<Column> columnsInBotiques = null;
	public static int fvipInBotiques = -1;
	public static int lvipInBotiques = -1;
	public static int stInBotiques = -1;
	public static boolean flagInBotiques = false;

	/* 记录TOP100在跳转前的数据和位置 */
	public static List<Column> columnsInTops = null;
	public static int fvipInTops = -1;
	public static int lvipInTops = -1;
	public static int stInTops = -1;
	public static boolean flagInTops = false;

	/* 记录类型在跳转前的数据和位置 */
	public static List<Column> columnsInGenres = null;
	public static int fvipInGenres = -1;
	public static int lvipInGenres = -1;
	public static int stInGenres = -1;
	public static boolean flagInGenres = false;

	/* 记录艺术家在跳转前的数据和位置 */
	public static List<Artist> formerArtists = null;
	public static String currentChosenLetter = "all";
	public static int fvipInArtists = -1;
	public static int lvipInArtists = -1;
	public static int stInArtists = -1;
	public static boolean flagInArtists = false;

	public static boolean isWebListenActivityRunning = false;// 用于结束网络试听
	public static WebListenActivity runningWebListenActivity = null;
	public static boolean searchResultFragmentRunning = false;

	public static boolean webListenActivityRunning = false;// 用于结束网络试听

	// public static boolean localMusicUILoadOK = false;
	// public static boolean isLocalMusicPlayingBeforWebListen = false;

	// //一组代替intent传值的实验数据
	// public static long albumId2send=-1;
	// public static String albumName2send="";
	// public static Bitmap albumBitmap2send=null;
	// public static String albumImgurl2send="";
	// public static int albumLayout2send=-1;

	/* 正在缓存中的数据 */
	public static HashMap<Long, Integer> purchasingAlbums = new HashMap<Long, Integer>();
	public static HashMap<Long, Integer> purchasingPacks = new HashMap<Long, Integer>();
	public static HashMap<Long, Integer> purchasingMusics = new HashMap<Long, Integer>();

	public static Long currentScrollMillis = 0L;
	// public static Long formerScrollMillis = 0L;
	public static int formerCacheSubSerialNumber = -1;

	public static int avSubFailCount = 0;
	public static int cacheSubFailCount = 0;
	public static int boxSubFailCount = 0;
	public static TabWebFragment tabWebFragment;

	public static SelfReloader currentSelfReloader = null;// 超时或异常出现时可重新加载的网络界面

	public static BitmapDrawable albumDetailBgBitmapDrawable;// 商店专辑详情页虚化背景图

	public static String boxVersionName = "未知设备版本";// 盒子版本号
	public static int boxVersionCode = -1;

	// 控制端版本相关
	public static String currentControllerVersion = "未知当前版本";
	public static String latestControllerVersion = "未知最新版本";
	public static String forcingUpdateCode = "-1";
	public static String latestVersionDescription = "暂无描述";
	public static String latestVersionapkDownloadUrl = "未知路径";
	public static boolean versionsUpdateNotificationShown = false;// 每次启动只提示一次

	// 重搜相关
	public static boolean researchFlag = false;// 是否有重搜需求
	public static ComponentName researchComponentName;
	public static IBinder researchIBinder;

	/*本地播放相关*/
	public static Intent runningMusicPlayServiceIntent = null;
	private static PlayingInfo currentPlayingInfo = null;
	public static List<OnCurrentPlayingInfoChangedListener> cpiListeners=new ArrayList<OnCurrentPlayingInfoChangedListener>();

	public static void setCurrentPlayingInfo(PlayingInfo playingInfo) {
		currentPlayingInfo = playingInfo;
		for(OnCurrentPlayingInfoChangedListener listener:cpiListeners){
			listener.onCurrentPlayingInfoChanged();
		}
	}
	
	
	
	/*播放状态监听*/
	public static String currentState = PlayerFragment.STOPPED;
	public static List<OnCurrentPlayingStateChangedListener> cpsListeners=new ArrayList<OnCurrentPlayingStateChangedListener>();
	public static void setCurrentPlayingState(String playingState) {
		currentState = playingState;
		for(OnCurrentPlayingStateChangedListener listener:cpsListeners){
			listener.onCurrentPlayingStateChanged();
		}
	}
	
	public static PlayingInfo getCurrentPlayingInfo() {
		return currentPlayingInfo;
	}

	private WatchDog() {

	}

	public static WatchDog getInstance() {
		if (instance == null) {
			instance = new WatchDog();
		}
		return instance;
	}

	public static void killThemAll() {
		for (Activity activity : currentActivities) {
			activity.finish();
		}
	}

	public static void clearData() {
		WatchDog.currentList = null;
		WatchDog.currentListType = 0;
		WatchDog.currentListId = 0L;
		WatchDog.currentPlayingIndex = 0;
		WatchDog.currentPlayingMusic = null;
		WatchDog.currentPlayingId = 0L;
		WatchDog.currentPlayingName = "";
		WatchDog.currentArtistName = "";
		WatchDog.currentState = PlayerFragment.STOPPED;// 先改为停止以停止播放器图标的跳动
		WatchDog.cacheStateMap.clear();

		UpnpApp.context.sendBroadcast(new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED));
	}

	// 如果上一曲未缓存完，而当前曲目将要进入下载状态，则修改上一曲缓存状态为等待
	public static void updateCachingState() {
		if (cacheStateMap != null && currentPlayingId != null) {
			System.out.println("cacheStateMap.get(currentPlayingId)=" + cacheStateMap.get(currentPlayingId));
		} else {
			return;
		}

		if ((Music.CACHE_WAIT).equals(cacheStateMap.get(currentPlayingId))) {// 空指针
			for (Long id : cacheStateMap.keySet()) {
				// 暂停上一曲缓存
				if (cacheStateMap.get(id).equals(Music.CACHE_DOWNLOADING) && !id.equals(currentPlayingId)) {
					cacheStateMap.put(id, Music.CACHE_WAIT);
				}

				// 立即开始本曲目缓存
				if (!cacheStateMap.get(id).equals(Music.CACHE_DOWNLOADED) && id.equals(currentPlayingId)) {
					cacheStateMap.put(id, Music.CACHE_DOWNLOADING);
				}
			}
		}
	}

	// 如果lastChange显示某支曲目开始缓存，则将其他正在缓存曲目全部置为等待
	public static void clearFormerCaching(long id) {
		if (cacheStateMap == null || !cacheStateMap.containsKey(id)) {
			return;
		} else {
			for (Long id0 : cacheStateMap.keySet()) {
				if (cacheStateMap.get(id0).equals(Music.CACHE_DOWNLOADING) && !id0.equals(id)) {
					cacheStateMap.put(id0, Music.CACHE_WAIT);
				}
			}
		}
	}

	public static void clearFormerCachingNospace(long id) {
		if (cacheStateMap == null || !cacheStateMap.containsKey(id)) {
			return;
		} else {
			for (Long id0 : cacheStateMap.keySet()) {
				if (cacheStateMap.get(id0).equals(Music.CACHE_FAILURE_NOSPACE) && !id0.equals(id)) {
					cacheStateMap.put(id0, Music.CACHE_WAIT);
				}
			}
		}
	}

	/*
	 * 手动同步数据
	 */
	public static void updateLocalData(Context context) {
		String host = "";
		if (WatchDog.currentHost != null) {
			host = WatchDog.currentHost.replaceAll("[.,/,:]", "");
		}
		String dbname = WatchDog.currentUserId + host;
		SharedPreferences prefs = context.getSharedPreferences("runcount", 0);
		String datatag = prefs.getString(dbname + "*", "0");
		if (!datatag.equals("0")) {
			Editor edi = prefs.edit();
			edi.putString(dbname + "*", "false");
			edi.commit();
		}

		clearPurchasingMaps();

		/*
		 * Intent intent = new Intent(context, MainActivity.class); //
		 * intent.addFlags(TRIM_MEMORY_BACKGROUND);
		 * context.startActivity(intent);
		 */

		Intent loginIntent = new Intent(context, LoginActivity.class);
		Bundle bundle = new Bundle();
		bundle.putInt(LoginActivity.BUNDLE_STATE, StateModel.STATE_SYNC);
		loginIntent.putExtras(bundle);
		context.startActivity(loginIntent);

	}

	/* 判断播放器是否可以进入 */
	public static boolean checkMediaReady() {
		if (PlayerFragment.STOPPED.equals(WatchDog.currentState)) {
			// CustomToast.makeText(UpnpApp.context,
			// UpnpApp.context.getResources().getString(R.string.noMusicPlaying),
			// Toast.LENGTH_SHORT).show();
			UpnpApp.mainHandler.showInfo(R.string.player_no_playingmusic_info);
			return false;
		}

		if (WatchDog.mediaOutOfService == true) {
			// 屏蔽提示“网络试听同步功能将在稍后开放”
			// CustomToast.makeText(UpnpApp.context,
			// WatchDog.mediaOutOfServiceReson, Toast.LENGTH_SHORT).show();
			return false;
		}

		if (WatchDog.babyNotMine == true) {
			// CustomToast.makeText(UpnpApp.context,
			// UpnpApp.context.getResources().getString(R.string.playlistNotInitiated),
			// Toast.LENGTH_SHORT).show();
			UpnpApp.mainHandler.showInfo(R.string.player_isused_info);
			return false;
		}

		return true;
	}

	/*
	 * 清空状态为正在缓存中的数据 这是一个不甚严密的方法
	 * 更为严密的做法应是：将purchasingMaps与本地数据库中逐个校对后再精确删除部分或全部对象
	 */
	public static void clearPurchasingMaps() {
		purchasingAlbums.clear();
		purchasingPacks.clear();
		purchasingMusics.clear();
	}

}
