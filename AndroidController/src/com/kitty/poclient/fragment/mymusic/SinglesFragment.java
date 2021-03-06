package com.kitty.poclient.fragment.mymusic;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kitty.poclient.R;
import com.kitty.poclient.adapter.SinglesAdapter;
import com.kitty.poclient.bean.LocalSingle;
import com.kitty.poclient.bean.LocalSingles;
import com.kitty.poclient.common.BroadcastManager;
import com.kitty.poclient.common.Constant;
import com.kitty.poclient.common.UIHelper;
import com.kitty.poclient.common.UpnpApp;
import com.kitty.poclient.common.WatchDog;
import com.kitty.poclient.dao.AlbumDao;
import com.kitty.poclient.data.VirtualData;
import com.kitty.poclient.domain.Album;
import com.kitty.poclient.domain.Music;
import com.kitty.poclient.fragment.PlayerFragment;
import com.kitty.poclient.upnp.BoxControl;
import com.kitty.poclient.upnp.Player;
import com.kitty.poclient.util.MediaUtil;
import com.kitty.poclient.widget.StandardCustomDialog;

@SuppressLint("HandlerLeak")
public class SinglesFragment extends BaseFragment {
	private final String TAG = SinglesFragment.class.getSimpleName();

	
	private LinearLayout llLvMusics;
	private TextView tvNoData;
	private int firstVisiblePosition = -1;// 记录listview的滚动位置
	private int scrollTop;
	private boolean dontChangeCurrentPosition = false;// 随机播放时使用

	private ListView lvMusics;
	private SinglesAdapter adapter;
	private View view;
	
	private LinearLayout playAndManage; //播放和管理区域
	private LinearLayout llPlaymode;	//“隨機播放”
	private LinearLayout cacheManage; //“管理”
	
	public static int SINGLE_MODE = 0; // current single mode
	public static final int SINGLE_MODE_PLAY = 0;
	public static final int SINGLE_MODE_MANAGE = 1;
	
	private LinearLayout cancelAndClearcache;
	private LinearLayout cancelClearCache;		 // 取消缓存
	private CancelClearListener cancelClearListener;
	private LinearLayout clearCache;	 // 确定清楚缓存
	
	private LinearLayout selectAllBtn;// 全选linearLayout
	private CheckBox selectAllCheckBox;
	
//	private TextView tvPlaymode; //随机播放
	
	// 侧滑菜单属性
	private FrameLayout flMainUI;
	private FrameLayout flSlidingMenu;
	private RelativeLayout rlParent;
	private long currentAnimationTime;
	private int currentPosition = 0;
	private int menuWidth;
	private int scale;
	private final int ANIMATION_DURATION = 20;
	private final int MSG_ANIM = 0;
	private boolean isShown = false;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			doAnimation();
		}
	};
	
	@SuppressLint("HandlerLeak")
	private Handler deleteHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
				case BoxControl.CLEAR_CACHE_SUCESS:
					UpnpApp.mainHandler.showInfo(R.string.mymusic_cache_clear_success_info);
					String idStr = (String) msg.obj;
					removeSingles(idStr);
					adapter.notifyDataSetChanged();
					break;
				case BoxControl.CLEAR_CACHE_FAILURE:
					UpnpApp.mainHandler.showAlert(R.string.mymusic_cache_clear_failure_alert);
//					adapter.notifyDataSetChanged();
					break;
			}
			cancelClearListener.setEnableCancel(true);
//			cancelClearCache.setOnClickListener(cancelClearListener);
		}

	};
	
	public SinglesFragment() {
	}
	
	private void removeSingles(String idStr){
		String[] deleteIdArray = idStr.split(",");
		
		for(int i=0;i<deleteIdArray.length;i++){
			long id = Long.parseLong(deleteIdArray[i]);
			int position = VirtualData.localSingles.getPositionById(id);
			VirtualData.localSingles.remove(position);
			VirtualData.musics.remove(position);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.e(TAG, "onCreateView");
		
		view = LayoutInflater.from(UpnpApp.context).inflate(R.layout.singles_fragment, null);
		view.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		
		initComponents();
		initArguments();
		initListeners();
//		registerReceivers();
		return view;
	}
	
	public String getFragmentName(){
		return TAG;
	}

	private void initComponents() {
		llLvMusics = (LinearLayout) view.findViewById(R.id.ll_lv_music);
		tvNoData = (TextView) view.findViewById(R.id.tv_no_data);		
		lvMusics = (ListView) view.findViewById(R.id.lv_music);
	}

	private void initArguments() {

		if (VirtualData.musics == null || VirtualData.musics.size() == 0) {
			// 显示暂无数据
			llLvMusics.setVisibility(View.GONE);
			tvNoData.setVisibility(View.VISIBLE);
		} else {
			llLvMusics.setVisibility(View.VISIBLE);
			tvNoData.setVisibility(View.GONE);
			
			adapter = new SinglesAdapter(UpnpApp.context, VirtualData.musics);
			lvMusics.setAdapter(adapter);
		}
	}

	private void initListeners() {
		lvMusics.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				new MediaUtil(getActivity()).playLocally(VirtualData.musics.get(position).getId());
			}
		});
	}
	
	private void showDeleteSinglesDialog() {
		
		StandardCustomDialog.Builder builder = new StandardCustomDialog.Builder(getActivity());
		builder.setTitle("清除歌曲缓存");
		builder.setMessage("确认清除选中的" + VirtualData.localSingles.getSelectedItemNum() + "首歌曲？");
		builder.setPositiveButton("取消",
				new android.content.DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						dialog.dismiss();
						
					}

				});
		builder.setNegativeButton("确定",
				new android.content.DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						cancelClearListener.setEnableCancel(false);
//						cancelClearCache.setOnClickListener(cancelClearListener);
						
						dialog.dismiss();
						
						String musicIds = "";
						LocalSingles selectedSingles = VirtualData.localSingles.getSelectedSingles();
						if (selectedSingles.size() > 0) {
							for (int index = 0; index < selectedSingles.size();index++) {
								LocalSingle selectedItem = selectedSingles.get(index);
								
								if(index == 0){
									musicIds = "" + selectedItem.getId();
								} else {
									musicIds =  musicIds + "," + selectedItem.getId();
								}
							}
							Log.i("SinglesCache", "begin  delete, musicIds" + musicIds);
							new BoxControl().clearcache(musicIds, BoxControl.MEDIA_TYPE_SINGLE, deleteHandler);
							Log.i("SinglesCache", "finish delete, musicIds" + musicIds);
							
							//判断是否全选
							//TODO
							if(selectAllCheckBox.isChecked()){
								//如果全部单曲被清除，数据库中所有专辑的位置状态也应全部改为“在云端”
								for(Album album:VirtualData.albums){
									new AlbumDao().updateCloudState(album.getId(), Constant.LOCATION_STATE_REMOTE);
								}
								
								VirtualData.albums.clear();
								VirtualData.localAlbums.clear();
							} else {
								//通知专辑、主题列表更新
								UIHelper.refreshAllLocalView();
							}
						}
					}

				});

		StandardCustomDialog dialog = builder.create();
		dialog.setCancelable(false);
		dialog.show();
	}

	private void setSingleMode(int mode){
		SINGLE_MODE = mode;
		switch(SINGLE_MODE){
			case SINGLE_MODE_PLAY:
				playAndManage.setVisibility(View.VISIBLE);
				cancelAndClearcache.setVisibility(View.GONE);
				selectAllBtn.setVisibility(View.GONE);
				break;
			case SINGLE_MODE_MANAGE:
				// 初始化 管理模式
				selectAllCheckBox.setChecked(false);
				VirtualData.localSingles = LocalSingles.translateMusics(VirtualData.musics);;
				adapter.notifyDataSetChanged();
				
				playAndManage.setVisibility(View.GONE);
				cancelAndClearcache.setVisibility(View.VISIBLE);
				selectAllBtn.setVisibility(View.VISIBLE);
				break;
		}
	}

	protected void onSingleItemClick_PlayMode(int position) {

		// 初始化播放列表
		WatchDog.currentList = (ArrayList<Music>) VirtualData.musics;
		WatchDog.currentListType = Constant.URI_MUSIC;
		WatchDog.currentListId = 0L;

		// sendBoxCurrentList("list_musics");
		WatchDog.currentPlayingIndex = position;
		WatchDog.currentPlayingMusic = VirtualData.musics.get(position);
		WatchDog.currentPlayingId = WatchDog.currentPlayingMusic.getId();
		System.out.println("WatchDog.currentPlayingId=" + WatchDog.currentPlayingId);
		WatchDog.currentPlayingName = VirtualData.musics.get(position).getName();
		WatchDog.currentArtistName = VirtualData.musics.get(position).getArtistName();

		WatchDog.updateCachingState();// 如果上一曲未缓存完则修改其状态为等待
		WatchDog.updateCachingState();// 本曲目状态立即转为正在下载
		WatchDog.currentState = PlayerFragment.STOPPED;// 先改为停止以停止播放器图标的跳动

//		initBtnPlayer();
		
		UIHelper.refreshPlayerButton();
//		adapter.notifyDataSetChanged();

		String uri = VirtualData.musics.get(position).getUri(Constant.URI_MUSIC, 0);
		playMusic(uri);

		adapter.notifyDataSetChanged();
	}

	/*
	 * 定位至当前播放曲目
	 */
	protected void locateCurrentPlaying() {
		if (!WatchDog.currentPlayingId.equals(0L)) {
			for (int i = 0; i < VirtualData.musics.size(); i++) {
				if (VirtualData.musics.get(i).getId().equals(WatchDog.currentPlayingId)) {
					lvMusics.setSelection(i > 3 ? i - 3 : i);
				}
			}
		} else {
			return;
		}
	}

	private void playMusic(String uri) {
		Player p = new Player();
		p.play(uri);
	}

	private void moveLayout(int scale) {
		int left = flMainUI.getLeft();
		System.out.println("left=" + left);

		if ((left + scale) < menuWidth && (left + scale) > 0) {
			// 还没有越界：
			flMainUI.offsetLeftAndRight(scale);
			currentPosition += scale;
		} else if ((left + scale) >= menuWidth) {
			// 再偏移就要越界的情况下：
			flMainUI.offsetLeftAndRight(menuWidth - left);
			currentPosition += menuWidth - left;
			isShown = true;
			WatchDog.isSlidingMenuShown = true;
			System.out.println("now, isShown = true");
		} else {
			System.out.println("(left + scale)=" + (left + scale));
			// 归零矫正
			flMainUI.offsetLeftAndRight(0 - left);
			currentPosition += 0 - left;
		}
		rlParent.invalidate();
		flMainUI.forceLayout();
	}

	private void doAnimation() {
		if (isShown == true) {
			System.out.println("isShown=" + isShown);
			moveLayout(-scale);
			System.out.println("currentPosition=" + currentPosition);

			if (currentPosition <= 0) {
				handler.removeMessages(MSG_ANIM);
				isShown = false;
				WatchDog.isSlidingMenuShown = false;
				rlParent.removeView(flSlidingMenu);

			} else {
				currentAnimationTime += ANIMATION_DURATION;
				handler.sendMessageAtTime(handler.obtainMessage(MSG_ANIM), currentAnimationTime);
			}

		} else if (isShown == false) {
			System.out.println("isShown=" + isShown);
			moveLayout(scale);
			System.out.println("currentPosition=" + currentPosition);

			if (currentPosition >= menuWidth) {
				handler.removeMessages(MSG_ANIM);
			} else {
				currentAnimationTime += ANIMATION_DURATION;
				handler.sendMessageAtTime(handler.obtainMessage(MSG_ANIM), currentAnimationTime);
			}

		} else {
		}
	}

	@Override
	public void onResume() {
		System.out.println(TAG + " onResume");

		currentPosition = 0;
		isShown = false;
		WatchDog.isSlidingMenuShown = false;

		if (adapter != null) {
			adapter.notifyDataSetChanged();
		} else {
			System.out.println(TAG + "adapter==null");
		}

		System.out.println("sx=" + firstVisiblePosition);
		// 恢复listview的卷动位置
		if (firstVisiblePosition != -1 && dontChangeCurrentPosition == false) {
			lvMusics.setSelectionFromTop(firstVisiblePosition, scrollTop);
		}
		super.onResume();
	}

	float x = 0;
	float y = 0;
	boolean heWantsTheMenu = false;


	@Override
	public void onDetach() {
		unregisterReceivers();
		super.onDetach();
	}

	private BroadcastReceiver updateListReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (adapter != null && 
					!(WatchDog.currentListType == Constant.URI_USB || WatchDog.currentListType == Constant.URI_CUE)) {
				 initArguments();
			}

			System.out.println("sx=" + firstVisiblePosition);
			// 恢复listview的卷动位置
			if (firstVisiblePosition != -1 && dontChangeCurrentPosition == false) {
				lvMusics.setSelectionFromTop(firstVisiblePosition, scrollTop);
			}
		}
	};

	private void registerReceivers() {
		getActivity().registerReceiver(updateListReceiver, new IntentFilter(BroadcastManager.FILTER_UPDATE_LOCALLIST));
	}

	private void unregisterReceivers() {
		getActivity().unregisterReceiver(updateListReceiver);
	}
	
	private class CancelClearListener implements OnClickListener {
		private boolean enable = true;
		
		public void setEnableCancel(boolean isEnable){
			enable = isEnable;
		}
		
		@Override
		public void onClick(View v) {
			//切换到状态： play
			if(enable){
				setSingleMode(SINGLE_MODE_PLAY);
			}
		}
	}
	
}
