package com.dfim.app.fragment.mymusic;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dfim.app.adapter.AlbumListAdapter;
import com.dfim.app.bean.LocalAlbums;
import com.dfim.app.common.BroadcastManager;
import com.dfim.app.common.UpnpApp;
import com.dfim.app.data.VirtualData;
import com.dfim.app.domain.Album;
import com.dfim.app.domain.AlbumDetail;
import com.dfim.app.fragment.TabMusicFragment;
import com.dfim.app.util.LoadImageAysnc;
import com.dfim.app.util.SingletonUtil;
import com.dfim.app.widget.ScrollOverListView;
import com.union.cellremote.R;
import com.union.cellremote.service.impl.AlbumDetailServiceImpl;

public class AlbumListFragment extends BaseFragment {

	private static final String TAG = AlbumListFragment.class.getSimpleName();

	private LinearLayout llLvAlbums;
	private TextView tvNoData;
//	private int sx = -1, sy = -1;// 记录listview的滚动位置
	private int sx = -1;// 记录listview的滚动位置
	private int scrollTop;

	private ListView albumsListView;
	public static ListAdapter adapter;
	private View view;
	
//	private LocalAlbums localAlbums;

	private int end;// 滑动一屏的结束位置
	private View vitem;// 视图listview item
	private Album album;
	private View imageView;// 专辑图片
	private int albumItemCount;

//	private final int MSG_INIT_BTN_PLAYER = 23;

	private Activity attachedActivity;
	private TabMusicFragment tabMusicFragment;
	
	public AlbumListFragment(Activity parentActivity) {
		attachedActivity = parentActivity;
	}
	public AlbumListFragment(Activity parentActivity, TabMusicFragment fragment) {
		attachedActivity = parentActivity;
		tabMusicFragment = fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");
		view = LayoutInflater.from(attachedActivity).inflate(R.layout.album_list_fragment, null);
		initComponents();
		initArguments();
		initListeners();
		registerReceivers();
		return view;
	}
	
	public String getFragmentName(){
		return TAG;
	}

	private void initComponents() {
		llLvAlbums = (LinearLayout) view.findViewById(R.id.ll_lv_album);
		tvNoData = (TextView) view.findViewById(R.id.tv_no_data);
		albumsListView = new ScrollOverListView(attachedActivity);
		llLvAlbums.addView(albumsListView, 0);

	}

	private void initListeners() {
		albumsListView.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// 不滚动时保存当前滚动到的位置
				if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
					sx = albumsListView.getFirstVisiblePosition();
					end = albumsListView.getLastVisiblePosition();
					vitem = albumsListView.getChildAt(0);
					scrollTop = (vitem == null) ? 0 : vitem.getTop();
					SingletonUtil.imagflag = true;
					LoadImageAysnc.unlock();
					if (end < albumItemCount - 4) {
						end += 2;
					}
					for (int i = 0; i < end - sx - 1; i++) {
						vitem = albumsListView.getChildAt(i + 1);
						if (vitem != null) {
							album = VirtualData.albums.get(sx + i);
							imageView = vitem.findViewById(R.id.iv_album_cover);
							imageView.setTag(album.getImgUrl());
							SingletonUtil.getSingletonUtil().loadAlbumImage(album, albumsListView, imageView);
						}
					}
				} else {
					SingletonUtil.imagflag = false;
					LoadImageAysnc.lock();

				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

				albumItemCount = totalItemCount;
			}
		});

		albumsListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
				try {
					Album album = (Album) albumsListView.getItemAtPosition(position);
					AlbumDetail albumDetail = new AlbumDetailServiceImpl().getAblumDetail(album.getId());
					if (albumDetail != null && albumDetail.getDisklist() != null && albumDetail.getDisklist().size() > 0) {
						AlbumDetailFragment detailFragment = new AlbumDetailFragment(attachedActivity, tabMusicFragment, albumDetail);
						tabMusicFragment.showAlbumDetailFragment(detailFragment);
					}else {
//						CustomToast.makeText(attachedActivity, "该专辑数据有误", Toast.LENGTH_SHORT).show();
						Log.e(TAG, UpnpApp.mainHandler.getString(R.string.album_data_error));
					}
				} catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG, UpnpApp.mainHandler.getString(R.string.album_data_error) + e.getMessage());
				}
			}
		});
	}

	@Override
	public void onResume() {
		// 恢复listview的卷动位置
		if (sx != -1) {
			albumsListView.setSelectionFromTop(sx, scrollTop);
		}
		super.onResume();
	}

	@Override
	public void onDetach() {
		unregisterReceivers();
		super.onDetach();
	}

	private BroadcastReceiver updateListReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			boolean refreshAlbumList = intent.getBooleanExtra(BroadcastManager.EXTRA_INCLUDE_LOCALALBUM, BroadcastManager.EXTRA_BOOLEAN_DEFAULT);
			Log.i(TAG, "updateListReceiver onReceive, notAlbumList:" + refreshAlbumList);
			if (refreshAlbumList) {
				initArguments();
				if (adapter != null) {
					Log.i(TAG, "updateListReceiver onReceive, adapter notifyDataSetChanged");
					((BaseAdapter) adapter).notifyDataSetChanged();
				}
				// 恢复listview的卷动位置
				if (sx != -1) {
					albumsListView.setSelectionFromTop(sx, scrollTop);
				}
			}
		}
	};
	
	
	private BroadcastReceiver onCacheStateMapEstablishedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// 初始化 缓存状态
			if(VirtualData.albums!=null && VirtualData.albums.size()>0){
				VirtualData.localAlbums = LocalAlbums.translateAlbumList(VirtualData.albums);
				if (adapter != null) {
					((BaseAdapter) adapter).notifyDataSetChanged();
				}
			}
		}
	};

	private void registerReceivers() {
		attachedActivity.registerReceiver(updateListReceiver, new IntentFilter(BroadcastManager.FILTER_UPDATE_LOCALLIST));
		attachedActivity.registerReceiver(onCacheStateMapEstablishedReceiver, new IntentFilter("onCacheStateMapEstablishedReceiver"));
	}

	private void unregisterReceivers() {
		attachedActivity.unregisterReceiver(updateListReceiver);
		attachedActivity.unregisterReceiver(onCacheStateMapEstablishedReceiver);
	}

	private void initArguments() {
		// initBtnPlayer();
		tabMusicFragment.refreshPlayStatus();

//		if (VirtualData.albums == null || VirtualData.albums.size() == 0) {
		if (VirtualData.localAlbums == null || VirtualData.localAlbums.size()==0){
			showNoData();
		} else {
			showListData();
			
			//translate localAlbums
//			localAlbums = LocalAlbums.translateAlbumList( VirtualData.albums);
//			localAlbums = VirtualData.localAlbums;
			
			adapter = new AlbumListAdapter(attachedActivity, albumsListView);
			albumsListView.setAdapter(adapter);
			LinearLayout li = (LinearLayout) albumsListView.findViewById(R.id.pulldown_footer_li);
			if (adapter.getCount() > 20) {
				if (li != null) {
					li.findViewById(R.id.pulldown_footer_loading).setVisibility(View.GONE);
					TextView tv = (TextView) li.findViewById(R.id.pulldown_footer_text);
					tv.setText("已加载完成");

				}
			} else {
				li.setVisibility(View.GONE);
				li.findViewById(R.id.pulldown_footer_loading).setVisibility(View.GONE);
				TextView tv = (TextView) li.findViewById(R.id.pulldown_footer_text);
				tv.setVisibility(View.GONE);
			}
			// lvAlbums.findViewById(R.id.pulldown_footer_li).setVisibility(View.GONE);
		}
	}

	private void showNoData() {
		// 显示暂无数据
		llLvAlbums.setVisibility(View.GONE);
		tvNoData.setVisibility(View.VISIBLE);
	}

	private void showListData() {
		llLvAlbums.setVisibility(View.VISIBLE);
		tvNoData.setVisibility(View.GONE);
	}

}
