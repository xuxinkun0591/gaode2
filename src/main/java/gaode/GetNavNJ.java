package gaode;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.type.LongType;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import entity.GdNavLinkNJ;
import utils.HttpClientResult;
import utils.HttpClientUtils;
import utils.JPAUtil;

public class GetNavNJ {
	private java.sql.Timestamp insert_time;
	private List<Map> links; // 需要导航的OD对
	private Long batch;
	private List<GdNavLinkNJ> tlist = new ArrayList<GdNavLinkNJ>();

	public GetNavNJ() {
		java.util.Date sysDate = new java.util.Date();
		insert_time = new java.sql.Timestamp(sysDate.getTime());
	};

	public static void main(String[] args) throws Exception {
		GetNavNJ gdrun = new GetNavNJ();
		gdrun.runSingle();
		System.out.println("main end..");
	}

	// 运行--单线程
	public void runSingle() throws Exception {
		Session session = JPAUtil.getSession();
		Transaction tx = session.beginTransaction();

		// 获取所有request
		links = getLinks();
		// 获取该次导航的batch
		batch = getBatch();

		// 逐条处理: 发送请求;解析
		for (int i = 0; i < links.size(); i++) {
			Map<String, Object> link = links.get(i);
			System.out.println("get response:" + (i + 1) + "/" + links.size());
			// 发送http请求
			String content = getHttpReq(link);
			if (content == null)
				continue;
			link.put("content", content);
			// 解析返回的内容
			parseJson(link);
		}
		
		// 删除重复
		delDump();
		
		// 为新数据赋值linkid
		setLinkid();
		
		//保存数据
		for (GdNavLinkNJ gdNavLink : tlist) {
			session.save(gdNavLink);
		}

		session.flush();
		session.clear();
		tx.commit();
		session.close();
		JPAUtil.close();
		System.out.println("batch: " + batch + "  " + new Date());
	}


	// 1获取导航OD对
	public List<Map> getLinks() {
		Session session = JPAUtil.getSession();
		List<Object[]> objects = session.createSQLQuery("SELECT * FROM V_GD_NAV_ROAD").addScalar("s").addScalar("e")
				.addScalar("m").list();
		session.clear();
		session.close();

		Map<String, Object> map = new HashMap<String, Object>();
		List<Map> linkList = new ArrayList<Map>();
		for (Object[] obj : objects) {
			map = new HashMap<String, Object>();
			map.put("S", obj[0]);
			map.put("E", obj[1]);
			map.put("M", obj[2]);
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
		params.put("strategy", "2"); // 10默认多路径;2单路径,距离最短
		params.put("origin", (String) map.get("S"));
		params.put("destination", (String) map.get("E"));
		// 中途点
		params.put("waypoints", (String) map.get("M"));

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
	public void parseJson(Map<String, Object> linkContent) throws ParseException {
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
			return;
		}
		// 有效导航,开始处理
		String orientation, road, action, polyline, linkStatus;
		Long tmcid, distance, duration, speed;
		GdNavLinkNJ singleLink;
		List<GdNavLinkNJ> list = new ArrayList<GdNavLinkNJ>();

		JsonArray paths = obj.get("route").getAsJsonObject().get("paths").getAsJsonArray();
		// System.out.print("paths:" + paths.size() + "; ");
		// 逐一处理paths (paths即为多个方案, 每个方案中有多个steps)
		for (int pid = 0; pid < paths.size(); pid++) {
			JsonArray steps = paths.get(pid).getAsJsonObject().get("steps").getAsJsonArray();
			// System.out.print("steps:" + steps.size() + "; tmcs:");
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
				speed = -1L;

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
				if (step.get("distance") != null && step.get("duration") != null) {
					distance = step.get("distance").getAsLong();
					duration = step.get("duration").getAsLong();
					speed = new Double(distance * 3.6 / duration).longValue();
				}
				if (step.get("polyline") != null) {
					polyline = step.get("polyline").getAsString();
					polyline = polyline.replace(",", " ");
					polyline = polyline.replace(";", ",");
					WKTReader fromText = new WKTReader();
					geom = fromText.read("LINESTRING(" + polyline + ")");
					geom.setSRID(4326);
				}
				// singleLink = new GdNavLinkNJ("step", (Long) tmcid, action, (Long)
				// distance, (Long) duration,speed,orientation, road, linkStatus, insert_time,
				// geom);
				// list.add(singleLink);

				// 循环处理tmcs
				JsonArray tmcs = step.get("tmcs").getAsJsonArray();
				// System.out.print(tmcs.size() + ",");
				for (int j = 0; j < tmcs.size(); j++) {
					/*
					 * // 第一个step的第一个tmc忽略 if (i == 0 && j == 0) { continue; } // 最后一个step的最后一个tmc忽略
					 * if (i == (steps.size() - 1) && j == (tmcs.size() - 1)) { continue; }
					 */
					tmcid++;
					distance = -1L;
					linkStatus = "";
					polyline = "";
					singleLink = null;

					JsonObject tmc = tmcs.get(j).getAsJsonObject();
					if (tmc.get("distance") != null) {
						distance = tmc.get("distance").getAsLong();
						if (distance < 3L)
							continue;
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
					// 速度直接取step的速度
					singleLink = new GdNavLinkNJ(batch,null,"tmc", tmcid, action, distance, -1L, speed, orientation, road,
							linkStatus, insert_time, geom);
					list.add(singleLink);
				}
			}
		}
		tlist.addAll(list);
	}

	// 取循环标识 batch, 从oracle中的序列SEQ_GD_NAV_NJ取
	public Long getBatch() {
		Session session = JPAUtil.getSession();
		Long batch = (Long)session.createSQLQuery("select SEQ_GD_NAV_NJ.nextval from dual").addScalar("nextval", LongType.INSTANCE).uniqueResult();
		session.close();
		return batch;
	}
	
	// 删除重复. 已在GdNavLinkNJ中定义何为重复
	public void delDump() {
		HashSet<GdNavLinkNJ> tset = new HashSet<GdNavLinkNJ>();
		tset.addAll(tlist);
		tlist.removeAll(tlist);
		tlist.addAll(tset);
	}
	
	// 为新数据赋值linkid
	public void setLinkid() {
		//取基准linkList
		Session session = JPAUtil.getSession();
		List<GdNavLinkNJ> baseLinks = session.createQuery("from GdNavLinkNJ t where t.batch = 0").list();
		session.close();
		//更新新数据
		for(int i = 0;i<tlist.size();i++) {
			int j = baseLinks.indexOf(tlist.get(i));
			if (j==-1) {
				System.out.println("找不到匹配的..");
				continue;
			}
			tlist.get(i).setLinkid(baseLinks.get(j).getLinkid());
		}
	}
}
