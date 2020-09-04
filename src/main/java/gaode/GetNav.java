/* 高德导航,获取路况 
 * hibernate版本
 * 请求示例: 
https://restapi.amap.com/v3/direction/driving?
origin=119.363306,26.048199
&destination=119.364579,26.041252
&waypoints=119.2299,26.088477;119.236967,26.091547;119.240965,26.093188
&strategy=10
&extensions=all
&key=你的key
 */

package gaode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import entity.GdNavLink_hibernate;
import utils.HttpClientResult;
import utils.HttpClientUtils;
import utils.JPAUtil;

public class GetNav implements Runnable{
private java.sql.Timestamp insert_time;
	private static List<Map> links; // 需要导航的OD对..
	private int linkIndex = 0;

	public GetNav() {
		java.util.Date sysDate = new java.util.Date();
		insert_time = new java.sql.Timestamp(sysDate.getTime());
	};

	public static void main(String[] args) throws Exception {
		GetNav gdrun = new GetNav();

		int choose = 2;

		if (choose == 1) // 单线程
			gdrun.runSingle();
		if (choose == 2) // 多线程
			gdrun.runMul(gdrun);

		JPAUtil.close();
		System.out.println("main end..");
	}

	// 多线程接口方法run实现
	public void run() {
		Session session = JPAUtil.getSession();
		Transaction tx = session.beginTransaction();
		Map link = new HashMap();
		int linkCnt = 0;

		while (true) {
			synchronized (this) {
				if (linkIndex < links.size()) {
					link = links.get(linkIndex);
					linkIndex++;
					System.out.println("get request: " + linkIndex + "/" + links.size());
				} else {
					break;
				}
			}
			// 发送http请求
			String content = getHttpReq(link);
			if (content == null)
				continue;
			link.put("content", content);
			// 解析返回的内容
			List<GdNavLink_hibernate> parseList = null;
			try {
				parseList = parseJson(link);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			if (parseList == null)
				continue;
			// 保存step和tmc
			for (GdNavLink_hibernate gdNavLink : parseList) {
				session.save(gdNavLink);
			}

			if (++linkCnt % 100 == 0) { // 每N次访问刷新并写入数据库
				session.flush();
				session.clear();
				tx.commit();
				tx = session.beginTransaction();
			}
		}
		session.flush();
		tx.commit();
		session.close();
	}

	// 运行--多线程
	public void runMul(GetNav mulThread) throws InterruptedException {
		// 获取所有request
		links = getLinks();
		Thread t1 = new Thread(mulThread);
		Thread t2 = new Thread(mulThread);
		Thread t3 = new Thread(mulThread);
		Thread t4 = new Thread(mulThread);
		Thread t5 = new Thread(mulThread);
		t1.start();
		t2.start();
		t3.start();
		t4.start();
		t5.start();
		t1.join();
		t2.join();
		t3.join();
		t4.join();
		t5.join();
	}

	// 运行--单线程
	public void runSingle() throws Exception {
		Session session = JPAUtil.getSession();
		Transaction tx = session.beginTransaction();

		// 获取所有request
		links = getLinks();
		// 逐条处理: 发送请求/解析/保存
		for (int i = 0; i < links.size(); i++) {
			Map<String, Object> link = links.get(i);
			System.out.println("get response:" + (i + 1) + "/" + links.size());
			// 发送http请求
			String content = getHttpReq(link);
			if (content == null)
				continue;
			link.put("content", content);
			// 解析返回的内容
			List<GdNavLink_hibernate> parseList = parseJson(link);
			if (parseList == null)
				continue;
			// 保存step和tmc
			for (GdNavLink_hibernate gdNavLink : parseList) {
				session.save(gdNavLink);
			}
			if (i % 50 == 49) {
				session.flush();
				session.clear();
			}
		}
		tx.commit();
		session.close();
	}

	// 1获取导航OD对
	public static List<Map> getLinks() {
		Session session = JPAUtil.getSession();
		List<Object[]> objects = session.createSQLQuery("SELECT * FROM V_GD_NAV_POINT").addScalar("s").addScalar("e").list();
		session.clear();
		session.close();

		Map<String, Object> map = new HashMap<String, Object>();
		List<Map> linkList = new ArrayList<Map>();
		for (Object[] obj : objects) {
			map = new HashMap<String, Object>();
			map.put("S", obj[0]);
			map.put("E", obj[1]);
			linkList.add(map);
		}
		System.out.println("待解析的OD数: " + linkList.size());
		return linkList;
	}

	// 2发起请求
	public String getHttpReq(Map<String, Object> map) {
		String url = "https://restapi.amap.com/v3/direction/driving";
		Map<String, String> params = new HashMap<String, String>();
		params.put("key", "你的key"); 
		params.put("extensions", "all");
		params.put("strategy", "10"); //10默认多路径;2单路径,距离最短
		params.put("origin", (String) map.get("S"));
		params.put("destination", (String) map.get("E"));
		
		HttpClientResult result;
		try {
			result = HttpClientUtils.doGet(url, params);
		} catch (Exception e) {
			System.out.println("访问接口出错了,忽略本次请求...");
			return null;
		}

		// 访问API失败
		if (result.getCode() != 200) {
			System.out.println("访问API失败: " + result.getCode());
			return null;
		}
		// 访问成功
		return result.getContent();
	}

	// 3json解析返回值
	public List<GdNavLink_hibernate> parseJson(Map<String, Object> linkContent) throws ParseException {
		JsonParser parser = new JsonParser();
		JsonObject obj = (JsonObject) parser.parse((String) linkContent.get("content"));

		// 导航是否成功,处理失败的情景
		int status = 0, count = 0;
		if (obj.get("status") != null) {
			status = obj.get("status").getAsInt();
		}
		if (obj.get("count") != null) {
			count = obj.get("count").getAsInt();
		}
		String info = obj.get("info").getAsString();
		// 无效导航
		if (status == 0 || count == 0) {
			System.out.println("response none..; info:" + info);
			return null;
		}
		// 有效导航,开始处理
		String orientation, road, action, polyline, linkStatus;
		Long tmcid, distance, duration;
		GdNavLink_hibernate singleLink;
		List<GdNavLink_hibernate> list = new ArrayList<GdNavLink_hibernate>();

		JsonArray paths = obj.get("route").getAsJsonObject().get("paths").getAsJsonArray();
		// 逐一处理paths (paths即为多个方案, 每个方案中有多个steps)
		for (int pid = 0; pid < paths.size(); pid++) {
			List<GdNavLink_hibernate> stepList = new ArrayList<GdNavLink_hibernate>();
			JsonArray steps = paths.get(pid).getAsJsonObject().get("steps").getAsJsonArray();
			// 逐一处理steps
			for (int i = 0; i < steps.size(); i++) {
				orientation = "";
				road = "";
				action = "";
				linkStatus = "";
				polyline = "";
				Geometry geom = null;
				singleLink = null;
				tmcid = 0L;
				distance = -1L;
				duration = -1L;

				JsonObject step = steps.get(i).getAsJsonObject();
				if (step.get("orientation") != null) {
					orientation = step.get("orientation").getAsString();
				}
				if (step.get("road") != null) {
					road = step.get("road").getAsString();
				}
				if (step.get("action") != null) {
					if (!step.get("action").isJsonArray())
						action = step.get("action").getAsString();
				}
				if (step.get("distance") != null) {
					distance = step.get("distance").getAsLong();
				}
				if (step.get("duration") != null) {
					duration = step.get("duration").getAsLong();
				}
				if (step.get("polyline") != null) {
					polyline = step.get("polyline").getAsString();
					polyline = polyline.replace(",", " ");
					polyline = polyline.replace(";", ",");
					WKTReader fromText = new WKTReader();
					geom = fromText.read("LINESTRING(" + polyline + ")");
					geom.setSRID(4326);
				}
				singleLink = new GdNavLink_hibernate("step", (Long) tmcid, action, (Long) distance, (Long) duration,
						orientation, road, linkStatus, insert_time, geom);
				list.add(singleLink);
				stepList.add(singleLink);

				// 循环处理tmcs
				JsonArray tmcs = step.get("tmcs").getAsJsonArray();
				// System.out.print(tmcs.size() + ",");
				for (int j = 0; j < tmcs.size(); j++) {
					if (i == 0 && j == 0) {
						continue;
					}
					if (i == (steps.size() - 1) && j == (tmcs.size() - 1)) {
						continue;
					}
					tmcid++;
					distance = -1L;
					linkStatus = "";
					polyline = "";
					singleLink = null;

					JsonObject tmc = tmcs.get(j).getAsJsonObject();
					if (tmc.get("distance") != null) {
						distance = tmc.get("distance").getAsLong();
					}
					if (tmc.get("status") != null) {
						linkStatus = tmc.get("status").getAsString();
					}
					if (tmc.get("polyline") != null) {
						polyline = tmc.get("polyline").getAsString();
						polyline = polyline.replace(",", " ");
						polyline = polyline.replace(";", ",");
						WKTReader fromText = new WKTReader();
						geom = fromText.read("LINESTRING(" + polyline + ")");
						geom.setSRID(4326);
					}
					singleLink = new GdNavLink_hibernate("tmc", tmcid, action, distance, -1L, orientation, road,
							linkStatus, insert_time, geom);
					list.add(singleLink);
				}
			}

			// 额外补充: 如果两个step之间断开,则额外补充一条线将两者相连
			for (int i = 0; i < stepList.size() - 1; i++) {
				Coordinate pointLast = stepList.get(i).getPolyline()
						.getCoordinates()[stepList.get(i).getPolyline().getNumPoints() - 1];
				Coordinate pointFirst = stepList.get(i + 1).getPolyline().getCoordinates()[0];
				if (pointLast.equals2D(pointFirst)) {
					continue;
				}
				String addLine = pointLast.x + " " + pointLast.y + "," + pointFirst.x + " " + pointFirst.y;
				WKTReader fromText = new WKTReader();
				Geometry geom = fromText.read("LINESTRING(" + addLine + ")");
				geom.setSRID(4326);
				list.add(new GdNavLink_hibernate("tmc", -1L, "手动补线", -1L, -1L, stepList.get(i).getOrientation(),
						stepList.get(i).getRoad(), "", insert_time, geom));
			}

		}
		return list;
	}
	
}
