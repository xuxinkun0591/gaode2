package entity;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.vividsolutions.jts.geom.Geometry;

@Entity
@Table(name = "GD_NAV_TRAFFIC_NJ")
public class GdNavLinkNJ {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long seq;	
	private Long batch;
	private Long linkid;
	private String dept;
	private Long tmcid;
	private String action;
	private Long distance;
	private Long duration;
	private Long speed;
	private String orientation;
	private String road;
	private String status;
	//@Temporal(TemporalType.TIMESTAMP) //指定时间格式
	@Column(name="insert_time")    //指定特定的列名
	private java.sql.Timestamp insertTime;
	private Geometry polyline;

	public GdNavLinkNJ() {
		super();
	}
	
	public GdNavLinkNJ(Long batch,Long linkid,String dept, Long tmcid, String action, Long distance,
			Long duration, Long speed,String orientation, String road, String status, java.sql.Timestamp insertTime, Geometry polyline) {
		super();
		this.batch=batch;
		this.linkid=linkid;
		this.dept = dept;
		this.tmcid = tmcid;
		this.action = action;
		this.distance = distance;
		this.duration = duration;
		this.speed = speed;
		this.orientation = orientation;
		this.road = road;
		this.status = status;
		this.insertTime = insertTime;
		this.polyline = polyline;
	}

	public Long getBatch() {
		return batch;
	}

	public void setBatch(Long batch) {
		this.batch = batch;
	}

	public Long getLinkid() {
		return linkid;
	}

	public void setLinkid(Long linkid) {
		this.linkid = linkid;
	}

	public Long getSeq() {
		return seq;
	}

	public void setSeq(Long seq) {
		this.seq = seq;
	}

	public String getDept() {
		return dept;
	}

	public void setDept(String dept) {
		this.dept = dept;
	}

	public Long getTmcid() {
		return tmcid;
	}

	public void setTmcid(Long tmcid) {
		this.tmcid = tmcid;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public Long getDistance() {
		return distance;
	}

	public void setDistance(Long distance) {
		this.distance = distance;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}
	
	public Long getSpeed() {
		return speed;
	}

	public void setSpeed(Long speed) {
		this.speed = speed;
	}
	
	public String getOrientation() {
		return orientation;
	}

	public void setOrientation(String orientation) {
		this.orientation = orientation;
	}

	public String getRoad() {
		return road;
	}

	public void setRoad(String road) {
		this.road = road;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public java.sql.Timestamp getInsertTime() {
		return insertTime;
	}

	public void setInsertTime(java.sql.Timestamp insertTime) {
		this.insertTime = insertTime;
	}

	public Geometry getPolyline() {
		return polyline;
	}

	public void setPolyline(Geometry polyline) {
		this.polyline = polyline;
	}
	
	@Override
	public int hashCode() {
		return polyline.toText().toString().hashCode();
	}
	@Override
	public boolean equals(Object o) {
		GdNavLinkNJ p2 = (GdNavLinkNJ)o;
		// 判定条件: 1.geom满足equals关系;2.方向相同
		boolean if_equal = polyline.toText().equals(p2.getPolyline().toText()) &&
				orientation.equals(p2.getOrientation());
		return if_equal;
	}

}
