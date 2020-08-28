package entity;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.vividsolutions.jts.geom.Geometry;

/*
create table gd_nav_traffic(
seq number(8) ,
dept varchar2(32),    -- step/tmc
tmcid number(8),     -- 第几个tmc
action varchar2(32),
distance number(8),
duration number(8),
orientation varchar2(64),
road varchar2(128),
status varchar2(32),
insert_time date,
polyline sdo_geometry
);
*/
@Entity
@Table(name = "GD_NAV_TRAFFIC")
public class GdNavLink_hibernate {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long seq;
	private String dept;
	private Long tmcid;
	private String action;
	private Long distance;
	private Long duration;
	private String orientation;
	private String road;
	private String status;
	//@Temporal(TemporalType.TIMESTAMP) //指定时间格式
	@Column(name="insert_time")    //指定特定的列名
	private java.sql.Timestamp insertTime;
	private Geometry polyline;
	
	public GdNavLink_hibernate() {
		super();
	}
	
	public GdNavLink_hibernate(String dept, Long tmcid, String action, Long distance,
			Long duration, String orientation, String road, String status, java.sql.Timestamp insertTime, Geometry polyline) {
		super();
		this.dept = dept;
		this.tmcid = tmcid;
		this.action = action;
		this.distance = distance;
		this.duration = duration;
		this.orientation = orientation;
		this.road = road;
		this.status = status;
		this.insertTime = insertTime;
		this.polyline = polyline;
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

}
