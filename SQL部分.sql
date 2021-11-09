/* 规律总结: 
1.在信号灯左转处,没有左转弧线,道路会缺失;在非信号灯双向路左转处,有时候会断开,有时候不会
2.在部分掉头处,导航路径(道路)会缺失
3.连续间隔15分钟访问某tmc,获得step的速度,速度会一直变.
4.最靠近交叉口的tmc速度一天到晚都很低,应该是考虑了信号灯等待时间.
5.有一些小路高德路况为"未知",在官方地图上也未显示,这些小路的速度(路程/时间)会给出一个相对固定的值(比如9km/h)
*/

/* 第一步 */
--1.1 创建OD点对表, 用于设置导航起终点, 该表可直接在ArcMap中编辑
-- DROP TABLE "XXK"."GD_NAV_POINT" ;
CREATE TABLE "XXK"."GD_NAV_POINT" 
   (	"OBJECTID" NUMBER(*,0) NOT NULL ENABLE, 
	"PROJECT" NVARCHAR2(50), 
	"SHAPE" "MDSYS"."SDO_GEOMETRY" , 
	"SE_ANNO_CAD_DATA" BLOB
);
--1.2 生成表GD_NAV_POINT的视图V_GD_NAV_POINT, 按高德接口要求构造起终点坐标, java将直接访问该视图
CREATE OR REPLACE VIEW v_gd_nav_point as
select TO_CHAR(ROUND(a.shape.sdo_point.x,6))||','||TO_CHAR(ROUND(a.shape.sdo_point.y,6)) S, --起点
TO_CHAR(ROUND(b.shape.sdo_point.x,6))||','||TO_CHAR(ROUND(b.shape.sdo_point.y,6)) E, --终点
from GD_NAV_POINT a,GD_NAV_POINT b
where a.objectid <> b.objectid
;
SELECT * FROM v_gd_nav_point;




/* 第二步 */
--2 创建路段表, 用于保存API路段数据
-- drop table GD_NAV_TRAFFIC;
create table GD_NAV_TRAFFIC(
seq number(8) ,
dept varchar2(32),    -- step/tmc
tmcid number(8),     -- 第几个tmc
action varchar2(32),
distance number(8),
duration number(8),
speed number(4),
orientation varchar2(64),
road varchar2(128),
status varchar2(32),
insert_time date,
polyline sdo_geometry
);
-- 创建空间索引
CALL SDOINDEX('GD_NAV_TRAFFIC','polyline',4326);
