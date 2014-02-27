package com.union.cellremote.store;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.dfim.app.activity.TabWebActivity;
import com.dfim.app.common.Constant;
import com.dfim.app.common.WatchDog;
import com.dfim.app.interfaces.NobleMan;
import com.dfim.app.thread.Pools;
import com.dfim.app.util.BitmapUtil;
import com.dfim.app.util.LoadImageAysnc.ImageCallBack;
import com.dfim.app.util.PowerfulBigMan;
import com.union.cellremote.R;
import com.union.cellremote.adapter.ThemeListAdapter4Web;
import com.union.cellremote.domain.Pack;
import com.union.cellremote.http.HttpGetter;

public class ThemesFragment extends Fragment implements NobleMan {

	// Looper.prepare,showNoData()

	private final String TAG = "BotiqueFragment";
	private Context context;

	private View view;
	private LinearLayout llLoading;
	private LinearLayout llNoData;
	private LinearLayout llContent;
	private ListView lvThemes;
	private ThemeListAdapter4Web adapter;
	private List<Pack> themes = new ArrayList<Pack>();
	private int maxDataItems = 50;

	private AnimationDrawable ad;
	private boolean loadingRunning = false;
	private boolean fragmentIsActive = false;

	private int firstVisibleItemPosition = 0;// 记录停止卷动时第一个ITEM的序号
	private int lastVisibleItemPosition = 2;// 记录停止卷动时第一个ITEM的序号
	// private int scrollTop = 0;// 记录停止卷动时第一个ITEM距离顶端的偏移量

	private final int MSG_LETS_GET_DATA = 0;
	private final int MSG_DATA_GOT = 1;
	private final int MSG_IMG_GOT = 2;
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_LETS_GET_DATA:
				getThemesList();
				break;

			case MSG_DATA_GOT:
				updateUI();
				paintPacksWithinSightAndFreeOthers(System.currentTimeMillis());
				break;

			case MSG_IMG_GOT:
				// endLoading();
				if (msg.obj != null && msg.obj instanceof Long && !((Long) (msg.obj)).equals(WatchDog.currentScrollMillis)) {
					// do nothing
				} else if (adapter != null) {
					adapter.notifyDataSetChanged();
				}
				break;
			}
			super.handleMessage(msg);
		}
	};

	private BroadcastReceiver fitThemesTitleReceier = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			parentActivityChangeButton();
			parentActivityChangeTitle();
		}
	};

	protected void updateUI() {
		if (adapter != null && themes != null && themes.size() != 0) {
			if (loadingRunning == true) {
				endLoading();
			}

			adapter.notifyDataSetChanged();

			/*
			 * System.out.println("adapter.getThemes().size()=" +
			 * adapter.getThemes().size()); System.out.println("themes.size()="
			 * + themes.size());
			 * System.out.println("adapter.getThemes()==themes?" +
			 * (adapter.getThemes() == themes));
			 */
		} else {
			showNoData();
		}
	}

	protected void showNoData() {
		llLoading.setVisibility(View.GONE);
		llContent.setVisibility(View.GONE);
		llNoData.setVisibility(View.VISIBLE);
	}

	private void getThemesList() {
		Pools.executorService1.submit(new Runnable() {

			@Override
			public void run() {
				themes.clear();
				themes.addAll(new HttpGetter(context).getThemesList(0, maxDataItems));

				if (themes != null && themes.size() != 0) {
					handler.sendEmptyMessage(MSG_DATA_GOT);
					WatchDog.themesDataGot = true;
				}
			}
		});
	}

	public ThemesFragment() {

	}

	public ThemesFragment(Context context) {
		this.context = context;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		parentActivityChangeButton();
		parentActivityChangeTitle();
		view = LayoutInflater.from(context).inflate(R.layout.themes_fragment, null);

		initComponents();
		startLoading();
		getDataWhenActive();
		initListeners();
		registerReceivers();

		return view;
	}

	private void getDataWhenActive() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (fragmentIsActive == false) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				handler.sendEmptyMessage(MSG_LETS_GET_DATA);
			}
		}).start();
	}

	private void startLoading() {
		llContent.setVisibility(View.INVISIBLE);
		llLoading.setVisibility(View.VISIBLE);
		llNoData.setVisibility(View.INVISIBLE);

		if (ad == null) {
			ad = (AnimationDrawable) getResources().getDrawable(R.anim.animatior_list);
		}
		llLoading.findViewById(R.id.iv_loading).setBackgroundDrawable(ad);
		ad.start();

		loadingRunning = true;
	}

	private void parentActivityChangeTitle() {
		TabWebActivity.tvTitle.setText("音乐主题");
		TabWebActivity.currentMenuItem = "音乐主题";
		((TabWebActivity) context).useTitleStyle(TabWebActivity.TITLE_STYLE_NORMAL);
	}

	private void parentActivityChangeButton() {
		TabWebActivity.changeButton("btnMenu");
	}

	protected void endLoading() {
		Log.e(TAG, "--endLoading()--");
		if (ad != null && ad.isRunning()) {
			ad.stop();
		}

		llContent.setVisibility(View.VISIBLE);
		llLoading.setVisibility(View.INVISIBLE);
		llNoData.setVisibility(View.INVISIBLE);

		loadingRunning = false;
	}

	@Override
	public void onResume() {
		super.onResume();
		// listViewGetFormerPosition();
		fragmentIsActive = true;
	}

	private void initComponents() {
		llLoading = (LinearLayout) view.findViewById(R.id.ll_loading);
		llNoData = (LinearLayout) view.findViewById(R.id.ll_no_data);
		llContent = (LinearLayout) view.findViewById(R.id.ll_content);
		lvThemes = (ListView) view.findViewById(R.id.lv_themes);
		initListView();
	}

	private void initListeners() {

	}

	@Override
	public void onDetach() {
		super.onDetach();
		unregisterReceivers();
		if (themes.size() != 0) {
			recycleBitmaps();
		}
	}

	private void registerReceivers() {
		context.registerReceiver(fitThemesTitleReceier, new IntentFilter("fitThemesTitleReceier"));
	}

	private void initListView() {
		adapter = new ThemeListAdapter4Web(context, this);
		adapter.setThemes(themes);
		lvThemes.setAdapter(adapter);

		lvThemes.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// 加载主题详情页
				if (PowerfulBigMan.testClickInterval() == false) {
					return;
				}

				Pack pack = themes.get(position);

				long packid = pack.getId();
				String packname = pack.getName();
				int musicCount = pack.getMcount();
				String imgUrl = pack.getImgurl();
				System.out.println("imgUrl=" + imgUrl);

				showPackContent(packid, packname, musicCount, imgUrl);
			}
		});

		lvThemes.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {

				if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
					// WatchDog.formerScrollMillis =
					// WatchDog.currentScrollMillis;
					WatchDog.currentScrollMillis = System.currentTimeMillis();

					firstVisibleItemPosition = lvThemes.getFirstVisiblePosition();
					lastVisibleItemPosition = lvThemes.getLastVisiblePosition();
					// if (lvThemes.getChildAt(0) != null) {
					// scrollTop = lvThemes.getChildAt(0).getTop();
					// }

					// adapter.notifyDataSetChanged();
					paintPacksWithinSightAndFreeOthers(WatchDog.currentScrollMillis);
				}

			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

			}
		});

	}

	protected void paintPacksWithinSightAndFreeOthers(Long myMillis) {
		for (int i = 0; i < themes.size(); i++) {
			if (i >= firstVisibleItemPosition && i <= lastVisibleItemPosition) {
				downloadImage(themes.get(i), myMillis);
			} else {
				Bitmap bitmap = themes.get(i).getBitmap();
				if (bitmap != null && !bitmap.equals(Constant.packCover)) {
					bitmap.recycle();
					bitmap = null;
				}
			}
		}
	}

	protected void downloadImage(final Pack pack, final Long myMillis) {
		Pools.executorService2.submit(new Runnable() {
			@Override
			public void run() {
				// Looper.prepare();
				String imageKey = pack.getImgurl() + "150";
				Bitmap bitmap = BitmapUtil.loadImageAysnc.loadImageNohandler(imageKey, pack.getImgurl(), 150, false, new ImageCallBack() {

					// 下载得到专辑封面后刷新界面
					@Override
					public void imageLoaded(Bitmap bitmap) {
						if (bitmap != null && !bitmap.isRecycled()) {
							pack.setBitmap(new SoftReference<Bitmap>(bitmap));
							// handler.sendEmptyMessage(MSG_IMG_GOT);
							Message msg = handler.obtainMessage(MSG_IMG_GOT, myMillis);
							handler.sendMessage(msg);
						}
					}
				});

				// 从缓存中得到封面后刷新界面
				if (bitmap != null && !bitmap.isRecycled()) {
					pack.setBitmap(new SoftReference<Bitmap>(bitmap));
					// handler.sendEmptyMessage(MSG_IMG_GOT);
					Message msg = handler.obtainMessage(MSG_IMG_GOT, myMillis);
					handler.sendMessage(msg);
				}

				bitmap = null;
				// Looper.loop();
			}
		});

	}

	protected void showPackContent(long id, String name, int musicCount, String imgUrl) {
		((TabWebActivity) context).goPackDetail(id, name, musicCount, imgUrl);
	}

	private void unregisterReceivers() {
		context.unregisterReceiver(fitThemesTitleReceier);
	}

	@Override
	public void letsSeeHeaven() {
		// TODO Auto-generated method stub

	}

	@Override
	public void recordCurrentDataAndPosition() {
		// TODO Auto-generated method stub

	}

	@Override
	public void getSavedDataAndPosition() {
		// TODO Auto-generated method stub

	}

	@Override
	public int getFistVisiblePosition() {
		return firstVisibleItemPosition;
	}

	@Override
	public int getLastVisiblePosition() {
		return lastVisibleItemPosition;
	}

	@Override
	public void recycleBitmaps() {
		if (themes != null) {
			for (Pack pack : themes) {
				pack.recyleBitmap();
			}
		}
	}

}
