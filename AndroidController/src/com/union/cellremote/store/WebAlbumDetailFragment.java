package com.union.cellremote.store;

import java.util.HashMap;
import java.util.List;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dfim.app.activity.TabWebActivity;
import com.dfim.app.activity.WebListenActivity;
import com.dfim.app.common.Constant;
import com.dfim.app.common.UpnpApp;
import com.dfim.app.common.WatchDog;
import com.dfim.app.dao.AlbumDao;
import com.dfim.app.data.VirtualData;
import com.dfim.app.domain.AlbumDetail;
import com.dfim.app.domain.Disk;
import com.dfim.app.domain.Music;
import com.dfim.app.http.HttpGetter;
import com.dfim.app.http.HttpPoster;
import com.dfim.app.interfaces.NobleMan;
import com.dfim.app.thread.Pools;
import com.dfim.app.upnp.BoxControl;
import com.dfim.app.upnp.Player;
import com.dfim.app.util.BitmapUtil;
import com.dfim.app.util.JsonUtil;
import com.dfim.app.util.LoadImageAysnc.ImageCallBack;
import com.dfim.app.widget.CustomToast;
import com.dfim.app.widget.StandardCustomDialog;
import com.union.cellremote.R;

//notifyData,连接中断，确定
public class WebAlbumDetailFragment extends Fragment implements NobleMan {

	private final String TAG = "WebAlbumDetailFragment: ";
	private Context context;

	/*
	 * 控件
	 */
	private View view;
	private LinearLayout llLoading;
	private LinearLayout llNoData;
	private LinearLayout llContent;
	private FrameLayout flAlbumInfo;
	private ImageView ivAlbumCover;
	private TextView tvAlbumName;
	private TextView tvArtistName;
	private Button btnBuy;
	private ExpandableListView expandablelistview;
	private BaseExpandableListAdapter adapter;
	private StandardCustomDialog dialog = null;

	/*
	 * 数据
	 */
	private long albumId = -1L;
	private AlbumDetail albumdetail;
	private AlbumDetail localAlbumdetail;
	private String albumName = "";
	private List<Disk> diskli;
	private List<Disk> localDisks;
	private int location = -1;// 0=在云端，5=在本地
	private int albumLocationState = -1;
	private List<HashMap<Long, Integer>> musicsStateList;
	private Bitmap bitmap;
	private double balance = -1L;
	private long idToBuy = -1L;
	private Music musicToBuy = null;
	private int groupPositionInListen = -1;
	private int childPositionInListen = -1;
	// private Music musicToBuy;
	// private Album albumToBuy;
	private boolean albumIsBought = false;
	private boolean albumIsForSale = false;

	/*
	 * handler&receivers
	 */
	private final int MSG_BALANCE_4_MUSIC = 1;
	private final int MSG_BALANCE_4_ALBUM = 2;
	private final int MSG_PURCHASE_SUCCESS = 3;
	private final int MSG_PURCHASE_SUCCESS_MUSIC = 4;
	private final int MSG_DATA_GOT_ALBUMDETAIL = 5;
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_BALANCE_4_MUSIC:
				balance = msg.getData().getDouble("balance", -1L);
				int groupPosition = msg.getData().getInt("groupPosition", -1);
				int childPosition = msg.getData().getInt("childPosition", -1);
				if (groupPosition != -1 && childPosition != -1) {
					launchWebBuy(groupPosition, childPosition);
				} else {
//					CustomToast.makeText(context, "未能获取商品信息", Toast.LENGTH_SHORT).show();
					UpnpApp.mainHandler.showAlert(R.string.album_data_error);
				}
				break;

			case MSG_BALANCE_4_ALBUM:
				balance = msg.getData().getDouble("balance", -1L);
				if (albumdetail.getAlbumId() != -1) {
					launchWebBuy();
				} else {
//					CustomToast.makeText(context, "未能获取商品信息", Toast.LENGTH_SHORT).show();
					UpnpApp.mainHandler.showAlert(R.string.album_data_error);
				}
				break;

			case MSG_PURCHASE_SUCCESS:
				// notifyBoxToSyn();// 通知盒子更新数据
				WatchDog.hasNewBought = true;// 本地数据更新标记
				albumIsBought = true;

				btnBuy.setText(getResources().getString(R.string.willCache));
				btnBuy.setEnabled(false);
				adapter.notifyDataSetChanged();
				break;

			case MSG_PURCHASE_SUCCESS_MUSIC:
				// notifyBoxToSyn();// 通知盒子更新数据
				WatchDog.hasNewBought = true;// 本地数据更新标记
				if (musicToBuy != null) {
					musicToBuy.setPurchaseState("已购买");
					musicToBuy = null;
				}

				adapter.notifyDataSetChanged();
				break;

			case MSG_DATA_GOT_ALBUMDETAIL:
				if (albumdetail == null || albumdetail.getDisklist() == null || albumdetail.getDisklist().size() == 0) {
					showNoData();
				} else {
					endLoding();
					initView();
				}
				break;
			}
			super.handleMessage(msg);
		}
	};
	// private BroadcastReceiver updateListReceiver = new BroadcastReceiver() {
	// @Override
	// public void onReceive(Context context, Intent intent) {
	// if (adapter!=null) {
	// adapter.notifyDataSetChanged();
	// }
	// }
	// };
	private BroadcastReceiver buyMusicReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (groupPositionInListen != -1 && childPositionInListen != -1) {
				getBalanceNLanunchBuy(groupPositionInListen, childPositionInListen);
			}
		}
	};

	private BroadcastReceiver webDetailPageUpdateUIReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			initBtnBuy();
			adapter.notifyDataSetChanged();
		}
	};

	// private BroadcastReceiver collectTheFuckingGarbageReceiver = new
	// BroadcastReceiver() {
	//
	// @Override
	// public void onReceive(Context context, Intent intent) {
	// letsSeeTheHeaven();
	// }
	// };

	public WebAlbumDetailFragment() {

	}

	protected void endLoding() {
		llLoading.setVisibility(View.GONE);
		llNoData.setVisibility(View.GONE);
		llContent.setVisibility(View.VISIBLE);
	}

	protected void showNoData() {
		llLoading.setVisibility(View.GONE);
		llContent.setVisibility(View.GONE);
		llNoData.setVisibility(View.VISIBLE);
	}

	protected void notifyBoxToSyn() {
		new BoxControl().notifyBoxToSyn();
	}

	public WebAlbumDetailFragment(Context context, long albumId, String albumName, Bitmap bitmap, int location, AlbumDetail albumDetail) {
		this.context = context;
		this.bitmap = bitmap;
		this.location = location;
		this.albumId = albumId;
		this.albumName = albumName;
		this.albumdetail = albumDetail;
		if (location != -1) {
			albumIsBought = true;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		parentActivityChangeButton("btnBack");
		parentActivityChangeTitle();
		view = LayoutInflater.from(UpnpApp.context).inflate(R.layout.web_album_detail, null);

		initComponents();
		startLoading();
		getData();
		// initView();
		initListeners();
		registerReceivers();

		return view;
	}

	private void startLoading() {
		llLoading.setVisibility(View.VISIBLE);
		llNoData.setVisibility(View.GONE);
		llContent.setVisibility(View.GONE);
		AnimationDrawable ad = (AnimationDrawable) getResources().getDrawable(R.anim.animatior_list);
		llLoading.findViewById(R.id.iv_loading).setBackgroundDrawable(ad);
		ad.start();
	}

	private void getData() {
		if (location == -1) {
			getDataFromWeb();
		} else {
			getDataFromLocal();
		}
	}

	private void getDataFromWeb() {
		if (albumId != -1) {
			Pools.executorService1.submit(new Runnable() {
				@Override
				public void run() {
					String json = new HttpGetter(context).getAlbumDetail(albumId);
					// System.out.println("jsonAlbumDetail="+json);
					albumdetail = new JsonUtil().getAlbumDetail(albumId, json);

					handler.sendEmptyMessage(MSG_DATA_GOT_ALBUMDETAIL);
				}
			});
		}
	}

	private void getDataFromLocal() {
		handler.sendEmptyMessage(MSG_DATA_GOT_ALBUMDETAIL);
	}

	@Override
	public void onResume() {
		super.onResume();
//		((TabWebActivity) ExitApplication.getInstance().getTabWebActivity()).activate();
	}

	@Override
	public void onPause() {
		if (WatchDog.hasNewBought) {
			notifyBoxToSyn();
//			CustomToast.makeText(context, "正在同步新购买的音乐...", Toast.LENGTH_SHORT).show();
			UpnpApp.mainHandler.showAlert(R.string.store_syn_new_music_info);
		}
		super.onPause();
	}

	@Override
	public void onDetach() {
		unregisterReceivers();
		// recycleBitmaps();
		super.onDetach();
	}

	private void parentActivityChangeTitle() {
		TabWebActivity.tvTitle.setText(albumName);
		((TabWebActivity) context).useTitleStyle(TabWebActivity.TITLE_STYLE_NORMAL);
	}

	private void parentActivityChangeButton(String which) {
		TabWebActivity.changeButton(which);
	}

	private void initComponents() {

		llLoading = (LinearLayout) view.findViewById(R.id.ll_loading);
		llNoData = (LinearLayout) view.findViewById(R.id.ll_no_data);
		llContent = (LinearLayout) view.findViewById(R.id.ll_content);
		// flAlbumInfo=(FrameLayout) view.findViewById(R.id.fl_album_info);
		// flAlbumInfo.setVisibility(View.GONE);
		ivAlbumCover = (ImageView) view.findViewById(R.id.iv_album_cover);
		tvAlbumName = (TextView) view.findViewById(R.id.tv_album_name);
		tvArtistName = (TextView) view.findViewById(R.id.tv_artist_name);
		btnBuy = (Button) view.findViewById(R.id.btn_buy);

		expandablelistview = (ExpandableListView) view.findViewById(R.id.expandlistview);
		expandablelistview.setGroupIndicator(null);

	}

	private void initListeners() {

		btnBuy.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (albumdetail != null) {
					getBalanceNLanunchBuy();
				} else {
					CustomToast.makeText(context, "未能获取专辑信息", Toast.LENGTH_SHORT).show();
				}
			}
		});

		/*
		 * expandablelistview.setOnChildClickListener(new OnChildClickListener()
		 * {
		 * 
		 * @Override public boolean onChildClick(ExpandableListView parent, View
		 * v, int groupPosition, int childPosition, long id) { if
		 * (PowerfulBigMan.testClickInterval() == false) { return false; }
		 * 
		 * if (v.getId() == R.id.btn_buy) { // 启动购买流程
		 * 
		 * } else if (v.getId() == R.id.ll_listen) { // 发起试听 Music music =
		 * (Music) diskli.get(groupPosition).getMusicList().get(childPosition);
		 * String uri = music.getMediaurl(); playMusic(uri);
		 * 
		 * Intent intent = new Intent(context, WebListenActivity.class);
		 * intent.putExtra("musicName", music.getName());
		 * intent.putExtra("artist", music.getArtistName());
		 * intent.putExtra("bitmap", bitmap); context.startActivity(intent); }
		 * 
		 * return true; } });
		 */

	}

	private void playMusic(String uri) {
		Player p = new Player();
		p.play(uri);
	}

	private void initView() {

		diskli = albumdetail.getDisklist();
		localAlbumdetail = new AlbumDao().getAlbumDetailData(albumId);

		// 显示图片、名称、演出者
		ivAlbumCover.setImageBitmap(bitmap);
		tvAlbumName.setText(albumdetail.getAlbumname());
		tvArtistName.setText(albumdetail.getArtistName());

		if (bitmap.equals(Constant.albumCover)) {
			downloadImage();
		}

		initBtnBuy();
		initMusicsListView();
	}

	private void initMusicsListView() {
		adapter = new BaseExpandableListAdapter() {

			TextView getTextView() {
				AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 64);
				TextView textView = new TextView(WebAlbumDetailFragment.this.context);
				textView.setLayoutParams(lp);
				textView.setBackgroundResource(R.color.groupview_bg);
				textView.setGravity(Gravity.CENTER_VERTICAL);
				textView.setPadding(40, 0, 0, 0);
				textView.setTextSize(30);
				textView.setTextColor(Color.WHITE);

				return textView;
			}

			// 重写expandlistadapter中的各个方法
			@Override
			public int getGroupCount() {
				return diskli != null ? diskli.size() : 0;
			}

			@Override
			public int getChildrenCount(int groupPosition) {
				if (diskli != null) {
					if (diskli.get(groupPosition) != null) {
						List<Music> li = diskli.get(groupPosition).getMusicList();
						if (li != null) {
							return li.size();
						}
					}
				}
				return 0;
			}

			@Override
			public Object getGroup(int groupPosition) {
				return diskli.get(groupPosition).getName();
			}

			@Override
			public Object getChild(int groupPosition, int childPosition) {
				if (diskli != null) {
					if (diskli.get(groupPosition) != null) {
						List<Music> li = diskli.get(groupPosition).getMusicList();
						if (li != null) {
							return li.get(childPosition).getName();
						}
					}
				}
				return null;
			}

			@Override
			public long getGroupId(int groupPosition) {
				return groupPosition;
			}

			@Override
			public long getChildId(int groupPosition, int childPosition) {
				return childPosition;
			}

			@Override
			public boolean hasStableIds() {
				return true;
			}

			@Override
			public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
				convertView = LayoutInflater.from(context).inflate(R.layout.albums_group_item, null);
				TextView tvDiscNo = (TextView) convertView.findViewById(R.id.tv_disc_no);
				tvDiscNo.setText("Disc" + (groupPosition + 1));

				// LinearLayout ll = new
				// LinearLayout(AlbumDetailFragment.this.context);
				// ll.setOrientation(0);
				// TextView textView = getTextView();
				// textView.setText("Disc" + (groupPosition + 1));
				// textView.setTextSize(15);
				// ll.addView(textView);

				return convertView;
			}

			@Override
			public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

				ChildHolder holder;
				List<Music> li = diskli.get(groupPosition).getMusicList();
				Music music = albumdetail.getDisklist().get(groupPosition).getMusicList().get(childPosition);
				Long musicId = music.getId();
				boolean btnBuyEnabled = false;
				String btnBuyText = "";

				if (convertView == null || convertView.getTag() == null) {
					convertView = LayoutInflater.from(context).inflate(R.layout.web_musics_item, null);
					holder = new ChildHolder(convertView);
					convertView.setTag(holder);

					if (li == null) {
						return convertView;
					}

				} else {
					holder = (ChildHolder) convertView.getTag();
				}

				// 显示单曲编号、名称、演出者
				holder.tvNum.setText((childPosition + 1) + "");
				holder.tvName.setText(li.get(childPosition).getName());
				holder.tvArtist.setText("null".equals(li.get(childPosition).getArtistName()) ? "未知演出者" : li.get(childPosition).getArtistName());

				/* 判断本地单曲状态 */
				if (location != -1) {

					music.setPurchaseState("已购买");
					if (music.getIscloud() == 0) {
						btnBuyText = "在云端";
					} else if (music.getIscloud() == 5) {
						btnBuyText = "在本地";
					}
					btnBuyEnabled = false;

					holder.btnBuy.setText(btnBuyText);
					holder.btnBuy.setEnabled(btnBuyEnabled);
				}

				/* 判断网络单曲状态 */
				else {
					// 整张专辑为缓存中，或缓存中的单曲包含该单曲时，显示缓存中
					if (WatchDog.purchasingAlbums.containsKey(albumId) || WatchDog.purchasingMusics.containsKey(musicId)) {
						btnBuyText = getResources().getString(R.string.willCache);
						btnBuyEnabled = false;
						holder.btnBuy.setText(btnBuyText);
						holder.btnBuy.setEnabled(btnBuyEnabled);
					}

					// 整张专辑为已购买时分类讨论
					else if (albumIsBought == true) {

						switch (albumLocationState) {

						// 专辑在本地时从本地数据库读取位置信息
						case 5:
							// holder.btnBuy.setText("在本地");
							int musicLocation = localDisks.get(groupPosition).getMusicList().get(childPosition).getIscloud();
							if (musicLocation == 5) {
								btnBuyText = "在本地";
								// holder.btnBuy.setText("在本地");
							} else if (musicLocation == 0) {
								btnBuyText = "在云端";
								// holder.btnBuy.setText("在云端");
							} else if (musicLocation == -1) {
								btnBuyText = "已购买";
								// holder.btnBuy.setText("已购买");
							}
							holder.btnBuy.setText(btnBuyText);
							break;

						// 整张专辑在云端则单曲全部在云端
						case 0:
							btnBuyText = "在云端";
							holder.btnBuy.setText(btnBuyText);
							break;

						default:
							btnBuyText = "已购买";
							holder.btnBuy.setText(btnBuyText);
							break;
						}

						holder.btnBuy.setEnabled(false);
						btnBuyEnabled = false;
						li.get(childPosition).setPurchaseState("已购买");
					}

					// 本地音乐中包含该单曲时显示为在本地
					else if (VirtualData.musics != null && VirtualData.musics.contains(li.get(childPosition))) {
						btnBuyText = "在本地";
						holder.btnBuy.setText(btnBuyText);
						holder.btnBuy.setEnabled(false);
						btnBuyEnabled = false;
						li.get(childPosition).setPurchaseState("已购买");
					}

					// 刚刚购买的显示为在本地
					else if ("已购买".equals(li.get(childPosition).getPurchaseState())) {
						btnBuyText = "在本地";
						holder.btnBuy.setText(btnBuyText);
						holder.btnBuy.setEnabled(false);
						btnBuyEnabled = false;
						li.get(childPosition).setPurchaseState("已购买");
					}

					// json数据中的状态值为45时显示为不单卖
					else if ("45".equals(li.get(childPosition).getPurchaseState())) {
						btnBuyText = "不单卖";
						holder.btnBuy.setText(btnBuyText);
						holder.btnBuy.setEnabled(false);
						btnBuyEnabled = false;
						// li.get(childPosition).setPurchaseState("不单卖");
					}

					// json数据中的状态值为30时显示为即将上架
					else if ("10".equals(li.get(childPosition).getPurchaseState()) || "30".equals(li.get(childPosition).getPurchaseState()) || "35".equals(li.get(childPosition).getPurchaseState()) || "40".equals(li.get(childPosition).getPurchaseState())) {
						btnBuyText = "即将上架";
						holder.btnBuy.setText(btnBuyText);
						holder.btnBuy.setEnabled(false);
						btnBuyEnabled = false;
						// li.get(childPosition).setPurchaseState("不单卖");
					}

					// 其它状态显示价格
					else {
						String price = li.get(childPosition).getPrice();
						if (!price.equals("0.0")) {
							btnBuyText = "￥ " + li.get(childPosition).getPrice() + "0";
							holder.btnBuy.setText(btnBuyText);
						} else {
							btnBuyText = getResources().getString(R.string.freeBtnText);
							holder.btnBuy.setText(btnBuyText);
						}

						holder.btnBuy.setEnabled(true);
						btnBuyEnabled = true;
					}

				}

				final String btnBuyText2 = btnBuyText;
				final boolean btnBuyEnabled2 = btnBuyEnabled;

				// 点击文字区域发起试听
				holder.llListen.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						groupPositionInListen = groupPosition;
						childPositionInListen = childPosition;
						launchWebListen(groupPosition, childPosition, btnBuyText2, btnBuyEnabled2);
					}
				});

				// 点击价格按钮发起购买
				holder.btnBuy.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						getBalanceNLanunchBuy(groupPosition, childPosition);
					}
				});

				return convertView;
			}

			@Override
			public boolean isChildSelectable(int groupPosition, int childPosition) {
				return true;
			}

		};

		expandablelistview.setAdapter(adapter);
		for (int i = 0; i < adapter.getGroupCount(); i++) {
			expandablelistview.expandGroup(i);
		}
	}

	private void setAlbumPurchaseStateII() {
		int purchaseState = new AlbumDao().getAlbumStateById(albumId, albumdetail.getMusicCount());
		switch (purchaseState) {
		case 5:
			albumIsBought = true;
			btnBuy.setText("在本地");
			btnBuy.setEnabled(false);
			break;

		case 0:
			albumIsBought = true;
			btnBuy.setText("在云端");
			btnBuy.setEnabled(false);
			break;

		case -1:
			albumIsBought = false;
			btnBuy.setText("￥ " + albumdetail.getPrice() + "0");
			btnBuy.setEnabled(true);
			break;
		}
	}

	private void initBtnBuy() {

		/* 直接由已购音乐传值判断购买状态 */
		if (location != -1) {
			// 显示本地专辑状态
			if (location == 0) {
				btnBuy.setText("在云端");
			} else if (location == 5) {
				btnBuy.setText("在本地");
			}
			btnBuy.setEnabled(false);
		}

		/* 网络专辑通过查询本地数据库接口查询是否已购买和位置信息 */
		else {
			System.out.println("albumdetail.getState()=" + albumdetail.getState());

			albumIsForSale = true;
			albumLocationState = new AlbumDao().getAlbumStateById(albumId, albumdetail.getMusicCount());
			switch (albumLocationState) {

			case 5:
				albumIsBought = true;
				btnBuy.setText("在本地");
				localDisks = new AlbumDao().getAlbumDetailForPurchased(albumId).getDisklist();
				btnBuy.setEnabled(false);
				break;

			case 0:
				albumIsBought = true;
				btnBuy.setText("在云端");
				btnBuy.setEnabled(false);
				break;

			default:
				albumIsBought = false;

				if (WatchDog.purchasingAlbums.containsKey(albumId)) {
					albumIsForSale = true;
					btnBuy.setText(getResources().getString(R.string.willCache));
					btnBuy.setEnabled(false);
				}

				else if ("20".equals(albumdetail.getState())) {
					albumIsForSale = true;
					double price = albumdetail.getPrice();
					if (price != 0d) {
						btnBuy.setText("￥ " + albumdetail.getPrice() + "0");
					} else {
						btnBuy.setText(getResources().getString(R.string.freeBtnText));
					}

					btnBuy.setEnabled(true);
				}

				else if ("10".equals(albumdetail.getState()) || "30".equals(albumdetail.getState()) || "35".equals(albumdetail.getState()) || "40".equals(albumdetail.getState())) {
					albumIsForSale = false;
					btnBuy.setText("即将上架");
					btnBuy.setEnabled(false);
				}

				else {
					albumIsForSale = true;
					double price = albumdetail.getPrice();
					if (price != 0d) {
						btnBuy.setText("￥ " + albumdetail.getPrice() + "0");
					} else {
						btnBuy.setText(getResources().getString(R.string.freeBtnText));
					}

					btnBuy.setEnabled(true);
				}

				break;
			}

			/*
			 * else if ( "10".equals(albumdetail.getState())
			 * ||"30".equals(albumdetail.getState())
			 * ||"35".equals(albumdetail.getState())
			 * ||"40".equals(albumdetail.getState()) ) { albumIsForSale = false;
			 * btnBuy.setText("即将上架"); btnBuy.setEnabled(false); }
			 */

		}

	}

	protected void downloadImage() {
		Pools.executorService2.submit(new Runnable() {
			@Override
			public void run() {
				String imageKey = albumdetail.getSmallImg() + "150";
				BitmapUtil.loadImageAysnc.loadImageNohandler(imageKey, albumdetail.getSmallImg(), 150, false, new ImageCallBack() {

					// 得到专辑封面后刷新界面
					@Override
					public void imageLoaded(final Bitmap bitmap) {
						handler.post(new Runnable() {
							@Override
							public void run() {
								ivAlbumCover.setImageBitmap(bitmap);
							}
						});
					}
				});

			}
		});

	}

	/*
	 * private void launchWebListen(int groupPosition, int childPosition) { //
	 * 发起试听 Music music = (Music)
	 * diskli.get(groupPosition).getMusicList().get(childPosition); String uri =
	 * music.getMediaurl(); if (!uri.startsWith("xxbox://listen?id=")) { uri =
	 * "xxbox://listen?id=" + music.getId(); } playMusic(uri);
	 * 
	 * Intent intent = new Intent(getActivity(), WebListenActivity.class);
	 * intent.putExtra("musicName", music.getName()); intent.putExtra("artist",
	 * music.getArtistName()); intent.putExtra("bitmap", bitmap);
	 * intent.putExtra("musicIsBought", music.getPurchaseState());
	 * intent.putExtra("price", music.getPrice().equals("0.0") ? getResources().getString(R.string.freeBtnText) : "￥ " +
	 * music.getPrice() + "0"); getActivity().startActivity(intent); }
	 */

	private void launchWebListen(int groupPosition, int childPosition, String btnBuyText, boolean btnBuyEnabled) {
		// 发起试听
		Music music = (Music) diskli.get(groupPosition).getMusicList().get(childPosition);
		String uri = music.getMediaurl();
		if (!uri.startsWith("xxbox://listen?id=")) {
			uri = "xxbox://listen?id=" + music.getId();
		}
		playMusic(uri);

		Intent intent = new Intent(getActivity(), WebListenActivity.class);
		intent.putExtra("musicName", music.getName());
		intent.putExtra("artist", music.getArtistName());
		intent.putExtra("bitmap", bitmap);
		intent.putExtra("musicIsBought", music.getPurchaseState());
		intent.putExtra("btnBuyText", btnBuyText);
		intent.putExtra("btnBuyEnabled", btnBuyEnabled);
		// intent.putExtra("price", music.getPrice().equals("0.0") ? getResources().getString(R.string.freeBtnText) : "￥ "
		// + music.getPrice() + "0");
		getActivity().startActivity(intent);
	}

	private void launchWebBuy(int groupPosition, int childPosition) {
		// 发起购买
		// Music music = (Music)
		// diskli.get(groupPosition).getMusicList().get(childPosition);
		musicToBuy = (Music) diskli.get(groupPosition).getMusicList().get(childPosition);

		if (balance != -1L) {
			String msg = musicToBuy.getName() + " \n价格：" + musicToBuy.getPrice() + "\t当前余额：" + balance + " 元\n\n确认购买吗？";
			idToBuy = musicToBuy.getId();
			if (idToBuy != -1) {
				showPurchaseReassureDialog(Constant.ordertype_audio, msg);
			} else {
//				CustomToast.makeText(context, "未能获取商品信息", Toast.LENGTH_SHORT).show();
				UpnpApp.mainHandler.showAlert(R.string.album_data_error);
			}
			balance = -1L;// 归零余额以备下一次查询
		} else {
//			CustomToast.makeText(context, "未能读取余额，请检查您的网络", Toast.LENGTH_SHORT).show();
			UpnpApp.mainHandler.showAlert(R.string.store_balance_read_alert);
		}
	}

	private void launchWebBuy() {
		// 发起购买
		if (balance != -1) {
			String msg = albumdetail.getAlbumname() + " \n价格：" + albumdetail.getPrice() + "\t当前余额：" + balance + " 元\n\n确认购买吗？";
			idToBuy = albumdetail.getAlbumId();
			if (idToBuy != -1) {
				showPurchaseReassureDialog(Constant.ordertype_album, msg);
			} else {
//				CustomToast.makeText(context, "未能获取商品信息", Toast.LENGTH_SHORT).show();
				UpnpApp.mainHandler.showAlert(R.string.album_data_error);
			}
			balance = -1L;// 归零余额以备下一次查询
		} else {
//			CustomToast.makeText(context, "未能读取余额，请检查您的网络", Toast.LENGTH_SHORT).show();
			UpnpApp.mainHandler.showAlert(R.string.store_balance_read_alert);
		}
	}

	private void getBalanceNLanunchBuy(final int groupPosition, final int childPosition) {
		Pools.executorService1.submit(new Runnable() {
			@Override
			public void run() {
				String json = new HttpGetter(context).getBalance();
				double _balance = new JsonUtil().getBalance(json);

				Message msg = new Message();
				msg.what = MSG_BALANCE_4_MUSIC;
				Bundle bundle = new Bundle();
				bundle.putDouble("balance", _balance);
				bundle.putInt("groupPosition", groupPosition);
				bundle.putInt("childPosition", childPosition);
				msg.setData(bundle);
				handler.sendMessage(msg);
			}
		});
	}

	private void getBalanceNLanunchBuy() {
		Pools.executorService1.submit(new Runnable() {
			@Override
			public void run() {
				String json = new HttpGetter(context).getBalance();
				double _balance = new JsonUtil().getBalance(json);

				Message msg = new Message();
				msg.what = MSG_BALANCE_4_ALBUM;
				Bundle bundle = new Bundle();
				bundle.putDouble("balance", _balance);
				msg.setData(bundle);
				handler.sendMessage(msg);
			}
		});

	}

	private void clearDialog() {
		if (dialog != null && dialog.isShowing()) {
			dialog.dismiss();
			dialog = null;
		}
	}

	private void showPurchaseReassureDialog(final String ordertype, String msg) {
		clearDialog();

		final StandardCustomDialog.Builder builder = new StandardCustomDialog.Builder(context);
		builder.setTitle("确认购买");
		builder.setMessage(msg);

		builder.setNegativeButton("确定", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showBuyingDialog();
				buy(ordertype, idToBuy);
			}
		});
		builder.setPositiveButton("取消", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		dialog = builder.create();
		dialog.show();
	}

	private void showBuyingDialog() {
		clearDialog();

		final StandardCustomDialog.Builder builder = new StandardCustomDialog.Builder(context);
		builder.setTitle("正在购买");
		builder.setMessage(null);

		AnimationDrawable ad = (AnimationDrawable) getResources().getDrawable(R.anim.animatior_searchbox_list);
		String str = "正在购买...";
		LinearLayout llContent = createDialogContent(ad, str);
		builder.setContentView(llContent);

		dialog = builder.create();
		dialog.show();
	}

	private void showBuyResultDialog(String msg) {
		clearDialog();

		final StandardCustomDialog.Builder builder = new StandardCustomDialog.Builder(context);
		builder.setTitle("完成购买");
		builder.setMessage(msg);

		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		dialog = builder.create();
		dialog.show();
	}

	protected void buy(final String ordertype, final long id) {
		Pools.executorService1.submit(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				String json = new HttpPoster().buyAlbumOrMusic(ordertype, id);
				String resultcode = new JsonUtil().getOrderFeedback(json);
				finishThisBuy(ordertype, id, resultcode);
				Looper.loop();
			}
		});
	}

	protected void finishThisBuy(String ordertype, long id, String resultcode) {
		if ("30".equals(resultcode)) {
//			showBuyResultDialog("操作成功\n\n即将为您缓存曲目");

			if (ordertype.equals(Constant.ordertype_album)) {
				WatchDog.purchasingAlbums.put(id, 0);
				handler.sendEmptyMessage(MSG_PURCHASE_SUCCESS);// 通知主线程发起同步
			} else if (ordertype.equals(Constant.ordertype_audio)) {
				WatchDog.purchasingMusics.put(id, 0);
				handler.sendEmptyMessage(MSG_PURCHASE_SUCCESS_MUSIC);// 通知主线程发起同步
			}

		} else if ("1".equals(resultcode)) {
			// 提示余额不足
			showBuyResultDialog("购买失败：余额不足");
		} else if ("5".equals(resultcode)) {
			// 提示已购买
			showBuyResultDialog("购买失败：您已经购买了该商品");
			if (ordertype.equals(Constant.ordertype_album)) {
				handler.sendEmptyMessage(MSG_PURCHASE_SUCCESS);// 通知主线程发起同步
			} else if (ordertype.equals(Constant.ordertype_audio)) {
				handler.sendEmptyMessage(MSG_PURCHASE_SUCCESS_MUSIC);// 通知主线程发起同步
			}

		} else if ("10".equals(resultcode)) {
			// 不是在售商品
			showBuyResultDialog("购买失败：不是在售商品");
		} else if ("15".equals(resultcode)) {
			// 绑定用户无效
			showBuyResultDialog("购买失败：不是有效用户");
		} else if ("20".equals(resultcode)) {
			// 密码错误
			showBuyResultDialog("购买失败：密码错误");
		} else if ("25".equals(resultcode)) {
			// 未知错误
			showBuyResultDialog("购买失败：未知错误");
		} else if ("-1".equals(resultcode)) {
			// 通信失败
			showBuyResultDialog("购买失败：通信失败");
		}
	}

	protected LinearLayout createDialogContent(Drawable drawable, String str) {
		LinearLayout llContent = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.common_dialog_content, null);
		ImageView iv = (ImageView) llContent.findViewById(R.id.iv_common);
		TextView tv = (TextView) llContent.findViewById(R.id.tv_common);

		iv.setBackgroundDrawable(drawable);
		if (drawable instanceof AnimationDrawable) {
			((AnimationDrawable) drawable).start();
		}
		tv.setText(str);

		return llContent;
	}

	private void registerReceivers() {
		context.registerReceiver(buyMusicReceiver, new IntentFilter("buyMusicReceiver"));
		context.registerReceiver(webDetailPageUpdateUIReceiver, new IntentFilter("webDetailPageUpdateUIReceiver"));
		// context.registerReceiver(collectTheFuckingGarbageReceiver, new
		// IntentFilter("collectTheFuckingGarbageReceiver"));
	}

	private void unregisterReceivers() {
		// context.unregisterReceiver(updateListReceiver);
		context.unregisterReceiver(buyMusicReceiver);
		context.unregisterReceiver(webDetailPageUpdateUIReceiver);
		// context.unregisterReceiver(collectTheFuckingGarbageReceiver);
	}

	private boolean isThisListPlaying(List<Music> list) {
		if (WatchDog.currentList == null) {
			return false;
		} else {
			String name1 = list.get(0).getName();
			String name2 = WatchDog.currentList.get(0).getName();
			return name1.equals(name2);
		}
	}

	class ChildHolder {
		private TextView tvNum;
		private TextView tvName;
		private TextView tvArtist;
		private Button btnBuy;
		private LinearLayout llListen;

		ChildHolder(View convertView) {
			tvNum = (TextView) convertView.findViewById(R.id.tv_num);
			tvName = (TextView) convertView.findViewById(R.id.tv_music_name);
			tvArtist = (TextView) convertView.findViewById(R.id.tv_music_artist);
			btnBuy = (Button) convertView.findViewById(R.id.btn_buy);
			llListen = (LinearLayout) convertView.findViewById(R.id.ll_listen);
		}
	}

	@Override
	public void letsSeeHeaven() {
		bitmap.recycle();

		albumdetail = null;
		diskli = null;
		bitmap = null;
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
		return -1;
	}

	@Override
	public int getLastVisiblePosition() {
		return -1;
	}

	@Override
	public void recycleBitmaps() {
		// if (bitmap!=null && !bitmap.equals(Constant.albumCover)) {
		// bitmap.recycle();
		// }
	}

}
