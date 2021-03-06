package com.kitty.poclient.util;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.util.Log;

import com.kitty.poclient.R;
//import com.dfim.app.widget.stickygridheader.BotiqueItem;
import com.kitty.poclient.activity.LoginActivity;
import com.kitty.poclient.bean.CurrentCacheChangeInfo;
import com.kitty.poclient.common.Constant;
import com.kitty.poclient.common.UIHelper;
import com.kitty.poclient.common.UpnpApp;
import com.kitty.poclient.common.WatchDog;
import com.kitty.poclient.domain.Album;
import com.kitty.poclient.domain.AlbumDetail;
import com.kitty.poclient.domain.Artist;
import com.kitty.poclient.domain.Column;
import com.kitty.poclient.domain.ColumnDetail;
import com.kitty.poclient.domain.Disk;
import com.kitty.poclient.domain.Music;
import com.kitty.poclient.domain.MusicDetail;
import com.kitty.poclient.domain.Pack;
import com.kitty.poclient.domain.PackDetail;
import com.kitty.poclient.domain.SearchDataObject;

public class JsonUtil {

	private final String TAG = "JsonUtil: ";

	public void dealCacheInfo(String jsonCacheInfo, String cacheUri) {
		System.out.println("dealCacheInfo");

		if (cacheUri.equals(Constant.cacheUriAllMusic)) {
			dealCacheInfoAllMusic(jsonCacheInfo);
		}
		// else if (cacheUri.equals(Constant.cacheUriAllTheme)) {
		//
		// } else if (cacheUri.equals(Constant.cacheUriAllFavorite)) {
		//
		// } else if (cacheUri.equals(Constant.cacheUriAllAlbum)) {
		//
		// }else if (cacheUri.startsWith(Constant.regCacheUriAlbum)) {
		//
		// } else if (cacheUri.startsWith(Constant.regCacheUriTheme)) {
		//
		// } else if (cacheUri.startsWith(Constant.regCacheUriFavorite)) {
		//
		// } else if (cacheUri.startsWith(Constant.regCacheUriMusic)) {
		//
		// }

		UIHelper.refreshLocalSinglesView();

		System.out.println("dealCacheInfo finished");
	}

	// 设置所有音乐的cacheState
	private void dealCacheInfoAllMusic(String jsonCacheInfo) {
		try {
			JSONObject jsonObj = new JSONObject(jsonCacheInfo);
			if(jsonObj.has("status")){
				JSONArray jsonArray = jsonObj.getJSONArray("status");
				JSONObject jsonObject = null;
				String cacheuriOutput = "";
				String statusCode = "";

				for (int i = 0; i < jsonArray.length(); i++) {
					jsonObject = new JSONObject(String.valueOf(jsonArray.get(i)));
					cacheuriOutput = String.valueOf(jsonObject.get("cacheuri"));
					statusCode = String.valueOf(jsonObject.get("statusCode"));

					// long id =
					// Long.parseLong(cacheuriOutput.substring(cacheuriOutput.lastIndexOf("=")
					// + 1));
					if (!statusCode.equals(Music.CACHE_DOWNLOADED) && !statusCode.equals(Music.CACHE_DOWNLOADING)) {
						statusCode = Music.CACHE_WAIT;
					}

					WatchDog.cacheStateMap.put(Long.parseLong(cacheuriOutput.substring(cacheuriOutput.lastIndexOf("=") + 1)), statusCode);
				}
				System.out.println("cacheStateMap=" + WatchDog.cacheStateMap);
				Log.e("BUG580", TAG+ "dealCacheInfoAllMusic():WatchDog.cacheStateMap.size()="+WatchDog.cacheStateMap.size());
				UIHelper.refreshAllMusicsCasheState();
			}
			

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void dealjsonCacheProgress(String jsonCacheProgress, String cacheUri) {
		Log.e("BUG974", TAG+"dealjsonCacheProgress()");

		if (cacheUri.startsWith(Constant.regCacheUriMusic)) {
			dealjsonCacheProgressMusic(jsonCacheProgress);
		}

		System.out.println("dealjsonCacheProgress Finished");
	}

	private void dealjsonCacheProgressMusic(String jsonCacheProgress) {
		Log.e("BUG974", TAG+"dealjsonCacheProgressMusic()");
		int progress = -1;

		try {
			// JSONObject obj=new JSONObject(jsonCacheProgress);
			progress = Integer.parseInt(new JSONObject(jsonCacheProgress).getString("progress"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Intent intent = new Intent("updateCacheProgress");
		intent.putExtra("progress", progress);
		UpnpApp.context.sendBroadcast(intent);

		System.out.println("dealjsonCacheProgress Music Finished");
	}

	public List<Column> getBotiques(String json) {
		List<Column> botiques = new ArrayList<Column>();

		try {
			JSONArray array = new JSONArray(json);
			Column botique = null;
			JSONObject object = null;

			for (int i = 0; i < array.length(); i++) {
				botique = new Column();
				object = (JSONObject) array.get(i);
				botique.setId((Long) object.get("id"));
				botique.setName((String) object.get("name"));
				botique.setType((Integer) object.get("type"));
				botiques.add(botique);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		// System.out.println("botiques=" + botiques);
		return botiques;
	}

	public List<Column> getTops(String json) {
		List<Column> tops = new ArrayList<Column>();

		try {
			JSONArray array = new JSONArray(json);
			Column top = null;
			JSONObject object = null;

			for (int i = 0; i < array.length(); i++) {
				top = new Column();
				object = (JSONObject) array.get(i);
				top.setId((Long) object.get("id"));
				top.setName((String) object.get("name"));
				top.setType((Integer) object.get("type"));
				tops.add(top);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return tops;
	}

	public List<Column> getGenres(String json) {
		List<Column> genres = new ArrayList<Column>();

		try {
			JSONArray array = new JSONArray(json);
			Column genre = null;
			JSONObject object = null;

			for (int i = 0; i < array.length(); i++) {
				genre = new Column();
				object = (JSONObject) array.get(i);
				genre.setId((Long) object.get("id"));
				genre.setName((String) object.get("name"));
				// genre.setType((Integer) object.get("type"));

				genres.add(genre);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return genres;
	}

	public ColumnDetail getColumnDetail(String json) {
		int total = -1;
		int num = -1;
		List<Album> albums = new ArrayList<Album>();

		try {
			JSONObject object = new JSONObject(json);

			total = (Integer) object.get("total");
			num = (Integer) object.get("num");
			JSONArray albumsArray = (JSONArray) object.get("album");
			JSONObject albumObject = null;
			Artist artist = null;
			List<Artist> artists = null;

			for (int i = 0; i < albumsArray.length(); i++) {
				albumObject = (JSONObject) albumsArray.get(i);

				artist = new Artist();
				String name = (String) albumObject.get("artist");
				artist.setName("".equals(name) || "未知".equals(name) ? "未知演出者" : name);
				artists = new ArrayList<Artist>();
				artists.add(artist);

				Album album = new Album();
				album.setId((Long) albumObject.get("id"));
				album.setName((String) albumObject.get("name"));
				album.setImgUrl((String) albumObject.get("smallimg"));
				album.setArtistli(artists);

				albums.add(album);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return new ColumnDetail(total, num, albums);
	}

//	public List<BotiqueItem> getBotiqueItemFromColumnDetailJson(String json, String columnName, int columnIndex) {
//		
//		List<BotiqueItem> botiqueItemList = new ArrayList<BotiqueItem>();
//		
//		int total = -1;
//		int num = -1;
//		List<Album> albums = new ArrayList<Album>();
//
//		try {
//			JSONObject object = new JSONObject(json);
//
//			total = (Integer) object.get("total");
//			num = (Integer) object.get("num");
//			JSONArray albumsArray = (JSONArray) object.get("album");
//			JSONObject albumObject = null;
//			Artist artist = null;
//			List<Artist> artists = null;
//
//			for (int i = 0; i < albumsArray.length(); i++) {
//				albumObject = (JSONObject) albumsArray.get(i);
//				
//				artist = new Artist();
//				String name = (String) albumObject.get("artist");
//				artist.setName("".equals(name) || "未知".equals(name) ? "未知演出者" : name);
//				artists = new ArrayList<Artist>();
//				artists.add(artist);
//				
//				/*
//				Album album = new Album();
//				album.setId((Long) albumObject.get("id"));
//				album.setName((String) albumObject.get("name"));
//				album.setImgUrl((String) albumObject.get("smallimg"));
//				album.setArtistli(artists);
//
//				albums.add(album);*/
//				
//				BotiqueItem botiqueItem = new BotiqueItem();
//				
//				botiqueItem.setColumnName(columnName);  
//				botiqueItem.setColumnIndex(columnIndex);
//
//				botiqueItem.setAlbumId((Long) albumObject.get("id"));
//				botiqueItem.setAlbumName((String) albumObject.get("name"));
//				botiqueItem.setImgUrl((String) albumObject.get("smallimg"));
//				botiqueItem.setArtistList(artists);
//				
//				botiqueItemList.add(botiqueItem);
//			}
//		} catch (JSONException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
////		return new ColumnDetail(total, num, albums);
//		return botiqueItemList;
//	}
	
	/*
	 * 解析jsonArtists
	 */
	public List<Artist> getArtists(String json) {
		int total = -1;
		int num = -1;
		List<Artist> artists = new ArrayList<Artist>();

		try {
			JSONObject object = new JSONObject(json);

			total = (Integer) object.get("total");
			num = (Integer) object.get("itemnum");
			JSONArray artistsArray = (JSONArray) object.get("artists");
			JSONObject artistObject = null;
			Artist artist = null;
			for (int i = 0; i < artistsArray.length(); i++) {
				artistObject = (JSONObject) artistsArray.get(i);

				// String name = (String) artistObject.get("name");
				// int state= (Integer) artistObject.get("state");
				// String imgUrl = (String) artistObject.get("smallimg");
				// String firstchar = (String) artistObject.get("firstchar");
				// long id = (Long) artistObject.get("id");

				artist = new Artist();
				artist.setName((String) artistObject.get("name"));
				artist.setFirstChar((String) artistObject.get("firstchar"));
				artist.setId((Long) artistObject.get("id"));
				artist.setImgUrl((String) artistObject.get("smallimg"));
				artists.add(artist);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return artists;
	}

	/*
	 * 解析jsonThemes
	 */
	public List<Pack> getThemes(String json) {
		int total = -1;
		int num = -1;
		List<Pack> themes = new ArrayList<Pack>();

		try {
			JSONObject object = new JSONObject(json);

			total = (Integer) object.get("total");
			num = (Integer) object.get("itemnum");
			JSONArray themesArray = (JSONArray) object.get("packs");
			JSONObject themeObject = null;
			Pack pack = null;

			for (int i = 0; i < themesArray.length(); i++) {
				themeObject = (JSONObject) themesArray.get(i);

				// long id = (Long) themeObject.get("id");
				// String imgUrl = (String) themeObject.get("smallimg");
				// String name = (String) themeObject.get("name");
				// int musicCount= (Integer) themeObject.get("trackstotal");

				pack = new Pack();
				pack.setName((String) themeObject.get("name"));
				pack.setId((Long) themeObject.get("id"));
				pack.setImgurl((String) themeObject.get("smallimg"));
				pack.setMcount((Integer) themeObject.get("trackstotal"));

				themes.add(pack);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return themes;
	}

	public AlbumDetail getAlbumDetail(long albumId, String json) {
		AlbumDetail detail = new AlbumDetail();

		try {

			JSONObject object = new JSONObject(json);
			String name = object.getString("name");
			String artists = object.getString("artists");
			String smallimg = object.getString("smallimg");
			String state = object.getString("state");
			String price = object.getString("price");
			
			String introduction = object.getString("introduction");
			String publishTime = object.getString("publishtime");
			String language = object.getString("language");
			String companyName = object.getString("companyname");
			
			int musiccount = object.getInt("musiccount");
			int actualMusicCount = 0;
			JSONArray disks = (JSONArray) object.get("disks");
			List<Disk> disklist = new ArrayList<Disk>();

			detail.setAlbumId(albumId);
			detail.setAlbumname(name);
			detail.setArtistName(artists);
			detail.setDisklist(disklist);
			detail.setPrice(Double.parseDouble(price));
			detail.setState(state);
			detail.setSmallImg(smallimg);
			// detail.setMusicCount(actualMusicCount);
			detail.setIntroduction(introduction);
			detail.setPublishTime(publishTime);
			detail.setLanguage(language);
			detail.setCompanyName(companyName);

			JSONObject diskObj = null;
			JSONArray musics = null;
			List<Music> musicList = null;
			Disk disk = null;

			JSONObject musicObj = null;
			Music music = null;
			for (int i = 0; i < disks.length(); i++) {
				diskObj = (JSONObject) disks.get(i);
				// String diskName = (String) diskObj.get("name");
				musics = (JSONArray) diskObj.get("musics");
				musicList = new ArrayList<Music>();
				actualMusicCount += musics.length();

				disk = new Disk();
				disk.setName((String) diskObj.get("name"));
				disk.setMusicList(musicList);

				for (int j = 0; j < musics.length(); j++) {
					musicObj = (JSONObject) musics.get(j);

					// String musicName = musicObj.getString("name");
					// String musicState = musicObj.getString("state");
					// String musicPrice = musicObj.getString("price");
					// String musicArtist = musicObj.getString("artist");
					// String musicTotalTime = musicObj.getString("totaltime");
					String musicId = musicObj.getString("id");
					// xxbox://listen?id=xxx
					String musicPlayurl = "xxbox://listen?id=" + musicId;

					music = new Music();
					music.setName(musicObj.getString("name"));
					music.setArtistName(musicObj.getString("artist"));
					music.setId(Long.parseLong(musicId));
					music.setPlay_time(musicObj.getString("totaltime"));
					music.setPrice(musicObj.getString("price"));
//					music.setMediaurl(musicPlayurl);
					music.setMediaurl(musicObj.getString("playurl"));
					music.setPurchaseState(musicObj.getString("state"));

					musicList.add(music);
				}
				disklist.add(disk);
			}

			detail.setMusicCount(actualMusicCount);

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return detail;
	}

	public PackDetail getPackDetail(long packId, String json) {
		PackDetail detail = new PackDetail();

		try {
			JSONObject object = new JSONObject(json);

			// String name = object.getString("name");
			// String bigimg = object.getString("bigimg");
			// String state = object.getString("state");
			// String price = object.getString("price");
			// String score = object.getString("score");
			// String introduction = object.getString("introduction");

			detail.setPackId(packId);
			detail.setPackName(object.getString("name"));
			detail.setPrice(Double.parseDouble(object.getString("price")));
			detail.setState(object.getString("state"));
			detail.setImgUrl(object.getString("bigimg"));

			JSONArray musicArray = (JSONArray) object.get("musics");
			List<Music> musics = new ArrayList<Music>();
			detail.setMusics(musics);
			JSONObject musicObj = null;
			Music music = null;

			for (int i = 0; i < musicArray.length(); i++) {
				musicObj = (JSONObject) musicArray.get(i);

				// String musicName = musicObj.getString("name");
				// String musicState = musicObj.getString("state");
				// String musicPrice = musicObj.getString("price");
				// String musicArtist = musicObj.getString("artist");
				// String musicTotalTime = musicObj.getString("totaltime");
				String musicId = musicObj.getString("id");
				String musicPlayurl = "xxbox://listen?id=" + musicId;

				music = new Music();
				music.setName(musicObj.getString("name"));
				music.setArtistName(musicObj.getString("artist"));
				music.setId(Long.parseLong(musicId));
				music.setPlay_time(musicObj.getString("totaltime"));
				music.setPrice(musicObj.getString("price"));
				music.setPurchaseState(musicObj.getString("state"));
				music.setMediaurl(musicPlayurl);
				music.setImgUrl(getImgurlPrefix()+musicObj.getString("albumimg"));

				musics.add(music);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return detail;
	}

	public double getBalance(String json) {
		double balance = -1;
		try {
			// JSONObject obj=new JSONObject(json);
			balance = (Double) new JSONObject(json).get("balance");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return balance;
	}

	public String getOrderFeedback(String json) {
		String resultcode = "-1";
		try {
			// JSONObject obj=new JSONObject(json);
			resultcode = new JSONObject(json).get("resultcode") + "";
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resultcode;
	}
	
	/**
	 * 
	 * @param json 盒子缓存更新信息
	 * @return CurrentCacheChangeInfo
	 */
	public CurrentCacheChangeInfo getCurrentCacheChangeInfo(String xmlStr){
		String xmlJson = xmlStr.substring(xmlStr.lastIndexOf("CurrentCacheChange val=") + "CurrentCacheChange val=".length() + 1, xmlStr.lastIndexOf("&#34;}") + "&#34;}".length());
		xmlJson = xmlJson.replaceAll("&#34;", "\"");
		CurrentCacheChangeInfo currentCacheChangeInfo = new CurrentCacheChangeInfo();
		try {
			JSONObject jsonObject = new JSONObject(xmlJson);
			
			String serialNumber = "";
			String cacheuri = "";
			String statusCode = "";
			String errorCode = "";
			
			serialNumber = jsonObject.optString("serialNumber", "");
			cacheuri = jsonObject.getString("cacheuri");
			statusCode = String.valueOf(jsonObject.getInt("statusCode"));
			errorCode = jsonObject.getString("errorcode");
			
			currentCacheChangeInfo.setSerialNumber(serialNumber);
			currentCacheChangeInfo.setCacheuri(cacheuri);
			currentCacheChangeInfo.setStatusCode(statusCode);
			currentCacheChangeInfo.setErrorcode(errorCode);

		} catch (JSONException e) {
			Log.e(TAG, "error in getCurrentCacheChangeInfo:" + e.getMessage());
			e.printStackTrace();
		}
		return currentCacheChangeInfo;
	}
	
	public String[] getUriAndCode(String json) {
		String serialNumber = "";
		String uri = "";
		String statusCode = "";
		String errorCode = "";

		try {
			JSONObject jsonObject = new JSONObject(json);
			
			serialNumber = jsonObject.optString("serialNumber", "");
//			serialNumber = String.valueOf(jsonObject.getInt("serialNumber"));
			uri = jsonObject.getString("cacheuri");
			statusCode = String.valueOf(jsonObject.getInt("statusCode"));
			errorCode = jsonObject.getString("errorcode");

			// Log.e(TAG, "uri=" + uri);
			// Log.e(TAG, "statusCode=" + statusCode);
			// Log.e(TAG, "errorCode=" + errorCode);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new String[] { serialNumber, uri, statusCode, errorCode };
	}

	public SearchDataObject getSearchDataAll(String json) {

		SearchDataObject sdo = new SearchDataObject();
		List<Album> albums = new ArrayList<Album>();
		List<Music> musics = new ArrayList<Music>();
		List<Artist> artists = new ArrayList<Artist>();

		try {
			JSONObject jsonObject = new JSONObject(json);

			int total = jsonObject.getInt("total");
			int itemnum = jsonObject.getInt("itemnum");
			JSONArray listArray = jsonObject.getJSONArray("list");

			for (int i = 0; i < listArray.length(); i++) {
				JSONObject item = (JSONObject) listArray.get(i);

				long contentid = item.getLong("contentid");
				String artistName = "未知艺术家";
				if (item.has("artistname")) {
					artistName = item.getString("artistname");
				}
				String albumName = "未知专辑";
				if (item.has("albumname")) {
					albumName = item.getString("albumname");
				}
				String name = item.getString("name");
				int type = item.getInt("type");
				String imgurl = item.getString("imgurl");

				switch (type) {
				case 1:// 专辑
					Album album = new Album();
					album.setId(contentid);
					album.setArtistName(artistName);
					album.setName(name);
					album.setImgUrl(imgurl);
					albums.add(album);
					break;

				case 5:// 单曲
					Music music = new Music();
					music.setId(contentid);
					music.setArtistName(artistName);
					music.setAlbumName(albumName);
					music.setName(name);
					music.setImgUrl(imgurl);
					musics.add(music);
					break;

				case 10:// 艺术家
					Artist artist = new Artist();
					artist.setId(contentid);
					artist.setName(name);
					artist.setImgUrl(imgurl);
					artists.add(artist);
					break;
				}
			}

			sdo.setAlbums(albums);
			sdo.setMusics(musics);
			sdo.setArtists(artists);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return sdo;
	}

	public List<Album> getSearchDataAlbums(String json) {

		List<Album> albums = new ArrayList<Album>();

		try {
			JSONObject jsonObject = new JSONObject(json);

			int total = jsonObject.getInt("total");
			int itemnum = jsonObject.getInt("itemnum");
			JSONArray listArray = jsonObject.getJSONArray("list");

			for (int i = 0; i < listArray.length(); i++) {
				JSONObject item = (JSONObject) listArray.get(i);

				long contentid = item.getLong("contentid");
				String artistName = "未知艺术家";
				if (item.has("artistname")) {
					artistName = item.getString("artistname");
				}
				String albumName = "未知专辑";
				if (item.has("albumname")) {
					albumName = item.getString("albumname");
				}
				String name = item.getString("name");
				int type = item.getInt("type");
				String imgurl = item.getString("imgurl");

				Album album = new Album();
				album.setId(contentid);
				album.setArtistName(artistName);
				album.setName(name);
				album.setImgUrl(imgurl);
				albums.add(album);
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return albums;
	}

	public List<Music> getSearchDataMusics(String json) {

		List<Music> musics = new ArrayList<Music>();

		try {
			JSONObject jsonObject = new JSONObject(json);

			int total = jsonObject.getInt("total");
			int itemnum = jsonObject.getInt("itemnum");
			JSONArray listArray = jsonObject.getJSONArray("list");

			for (int i = 0; i < listArray.length(); i++) {
				JSONObject item = (JSONObject) listArray.get(i);

				long contentid = item.getLong("contentid");
				String name = item.getString("name");
				int type = item.getInt("type");
				
				String temp =getImgurlPrefix();			
				String imgurl = "";
				if (item.has("albumimg")) {
					imgurl = temp + item.getString("albumimg");
				}

				String artistName = "未知艺术家";
				if (item.has("artistname")) {
					artistName = item.getString("artistname");
				}

				String albumName = "未知专辑";
				if (item.has("albumname")) {
					albumName = item.getString("albumname");
				}

				int state = -1;
				if (item.has("state")) {
					state = item.getInt("state");
				}

				int price = 0;
				if (item.has("price")) {
					price = item.getInt("price");
				}

				Music music = new Music();
				music.setId(contentid);
				music.setArtistName(artistName);
				music.setAlbumName(albumName);
				music.setName(name);
				music.setImgUrl(imgurl);
				music.setPurchaseState("" + state);
				music.setPrice("" + price);
				musics.add(music);
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return musics;
	}

	private String getImgurlPrefix() {
		String temp="";
		if(WatchDog.currentHost.endsWith(".fm/")){
			temp = WatchDog.currentHost + "fea63n/0oesq3/boss/upload/images/";// 外网图片地址
			Log.e(TAG, "外网图片地址");
		}else{
			temp = WatchDog.currentHost + "boss/upload/images/";// 内网图片地址
			Log.e(TAG, "内网图片地址");
		}
		return temp;
	}

	public List<Artist> getSearchDataArtists(String json) {

		List<Artist> artists = new ArrayList<Artist>();

		try {
			JSONObject jsonObject = new JSONObject(json);

			int total = jsonObject.getInt("total");
			int itemnum = jsonObject.getInt("itemnum");
			JSONArray listArray = jsonObject.getJSONArray("list");

			for (int i = 0; i < listArray.length(); i++) {
				JSONObject item = (JSONObject) listArray.get(i);

				long contentid = item.getLong("contentid");
				String artistName = "未知艺术家";
				if (item.has("artistname")) {
					artistName = item.getString("artistname");
				}
				String albumName = "未知专辑";
				if (item.has("albumname")) {
					albumName = item.getString("albumname");
				}
				String name = item.getString("name");
				int type = item.getInt("type");
				String imgurl = item.getString("imgurl");

				Artist artist = new Artist();
				artist.setId(contentid);
				artist.setName(name);
				artist.setImgUrl(imgurl);
				artists.add(artist);
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return artists;
	}

	public boolean validate(String json) {
		if("".equals(json)){
			return false;
		}
		
		if(UpnpApp.context.getString(R.string.data_load_failed).equals(json)){
			return false;
		}
		
		return true;
	}

	/*解析盒子传过来的json，获取盒子版本号*/
	public void getBoxVersion(String json,LoginActivity loginActivity) {
		try {
			JSONObject obj=new JSONObject(json);
			WatchDog.boxVersionName=obj.getString("versionName");
			WatchDog.boxVersionCode=obj.getInt("versionCode");
			
			Log.e("0221", TAG+"getBoxVersion():WatchDog.boxVersionName="+WatchDog.boxVersionName);
			Log.e("0221", TAG+"getBoxVersion():WatchDog.boxVersionCode="+WatchDog.boxVersionCode);
			
			if (WatchDog.boxVersionCode < Constant.LEAST_SUPPORT_BOX_VERSIONCODE) {// 2
				// 对话框提示用户升级盒子
				loginActivity.mHandler.sendEmptyMessage(LoginActivity.BOX_VERSION_LOW);
			}else{
				loginActivity.mHandler.sendEmptyMessage(LoginActivity.BOX_VERSION_OK);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*解析获取控制端最新版本号*/
	public void getLatestVersion(String json) {
		try {
			JSONObject obj=new JSONObject(json);
			WatchDog.latestControllerVersion = obj.getString("ver");
			WatchDog.forcingUpdateCode = obj.getString("isforce");
			WatchDog.latestVersionDescription = obj.getString("des");
			WatchDog.latestVersionapkDownloadUrl = obj.getString("url");
			
			Log.e("软件升级", TAG+"getLatestVersion():WatchDog.latestVersion="+WatchDog.latestControllerVersion);
			Log.e("软件升级", TAG+"getLatestVersion():WatchDog.forcingUpdateCode="+WatchDog.forcingUpdateCode);
			Log.e("软件升级", TAG+"getLatestVersion():WatchDog.latestVersionDescription="+WatchDog.latestVersionDescription);
			Log.e("软件升级", TAG+"getLatestVersion():WatchDog.latestVersionapkDownloadUrl="+WatchDog.latestVersionapkDownloadUrl);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public MusicDetail getMusicDetail(String json){
		MusicDetail  mDetail = null;
		try {
			JSONObject object=new JSONObject(json);
			mDetail=new MusicDetail();
			mDetail.setName(object.getString("name"));
			mDetail.setArtist(object.getString("artistname"));
			mDetail.setImgUrl(object.getString("albumimg"));
			mDetail.setDuration(object.getString("playtimes"));
			mDetail.setListenUrl(object.getString("listenurl"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return mDetail;	
	}
	
}
