/* 规律总结: 
1.在信号灯左转处,没有左转弧线,道路会缺失;在非信号灯双向路左转处,有时候会断开,有时候不会
2.在部分掉头处,导航路径(道路)会缺失
3.连续间隔15分钟访问某tmc,获得step的速度,速度会一直变.
4.最靠近交叉口的tmc速度一天到晚都很低,应该是考虑了信号灯等待时间.
5.有一些小路高德路况为"未知",在官方地图上也未显示,这些小路的速度(路程/时间)会给出一个相对固定的值(比如9km/h)
6.一个请求的不同step有时候会断开(tmc不会断开),java中已将断开的手动补线,包括
++    向右前方行驶
++    向左后方行驶
++    向左前方行驶
++    右转
++    左转     还需要在交叉口设置禁止走三角形的两边实现左转
++    左转调头   
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

-------------------------------------------- 执行完以上两部即可运行java runAll方法了-------------------------------------
-- 首次删除: 重合的线条  简约去重,首尾点相同即认为重复
DELETE FROM GD_NAV_TRAFFIC WHERE SEQ IN 
(SELECT SEQ FROM  
(SELECT AA.SEQ, ROW_NUMBER() OVER (PARTITION BY BB.X || BB.Y, CC.X || CC.Y ORDER BY SEQ) RN  
FROM GD_NAV_TRAFFIC AA,table(SDO_UTIL.GETVERTICES(AA.POLYLINE)) BB, table(SDO_UTIL.GETVERTICES(AA.POLYLINE)) CC
WHERE AA.DEPT = 'tmc' AND BB.ID=1 AND CC.ID=SDO_UTIL.GETNUMVERTICES (AA.POLYLINE))
WHERE RN>1 GROUP BY SEQ)
;





-- 第三步, 对获取的原始路段进行连通性和拓扑处理
-- 3.1 创建中间表, 增加字段: 长度/第一个点/第二个点/顶点数
-- DROP TABLE GD_NAV_TRAFFIC_2;
CREATE TABLE GD_NAV_TRAFFIC_2 AS
with
    P as
(
SELECT AA.SEQ,
SDO_GEOMETRY(2001,4326,SDO_POINT_TYPE(BB.X,BB.Y,NULL),NULL,NULL) P1,
SDO_GEOMETRY(2001,4326,SDO_POINT_TYPE(CC.X,CC.Y,NULL),NULL,NULL) P2
FROM GD_NAV_TRAFFIC AA, table(SDO_UTIL.GETVERTICES(AA.POLYLINE)) BB,table(SDO_UTIL.GETVERTICES(AA.POLYLINE)) CC
WHERE BB.ID = 1 AND CC.ID = SDO_UTIL.GETNUMVERTICES (AA.POLYLINE)
)
SELECT AA.*,
ROUND(SDO_GEOM.SDO_LENGTH(aa.POLYLINE,0.05,'UNIT=METER'),3) lng,  --长度
SDO_UTIL.GETNUMVERTICES (AA.POLYLINE) NUMVER,   --顶点数
P.P1,P.P2     -- 第一个点/第二个点
FROM GD_NAV_TRAFFIC AA,P
WHERE AA.SEQ = P.SEQ AND AA.DEPT = 'tmc'
;
-- 创建空间索引
CALL SDOINDEX('GD_NAV_TRAFFIC_2','polyline',4326);


															     
/* 处理1: 信号交叉口(左转和直行,长的(直行)完全覆盖短的(左转),起点相同) */
-- 1.1 COVERS临时表,筛选covers清单数据.使用covers索引,不然速度很慢
DROP TABLE TMP_COVERS;
CREATE TABLE TMP_COVERS AS
WITH
    T1 AS
(SELECT /*+ ORDERED */ A.SEQ LSEQ,B.SEQ SSEQ,
ROW_NUMBER() OVER(PARTITION BY A.SEQ ORDER BY B.P2.SDO_POINT.X,B.P2.SDO_POINT.Y) RN /*取x小或者y小进行裁剪*/
FROM GD_NAV_TRAFFIC_2 B,GD_NAV_TRAFFIC_2 A
WHERE SDO_GEOM.RELATE(A.P1,'EQUAL',B.P1,0.05)='EQUAL' AND SDO_COVERS(A.POLYLINE,B.POLYLINE)='TRUE'
AND SDO_GEOM.SDO_LENGTH(SDO_GEOM.SDO_DIFFERENCE(A.POLYLINE,B.POLYLINE,0.05), 0.05, 'UNIT=Meter')>1
AND SDO_UTIL.GETNUMVERTICES(SDO_GEOM.SDO_DIFFERENCE(A.POLYLINE,B.POLYLINE,0.05)) = 2)
SELECT * FROM T1 WHERE RN = 1
;
-- 1.2: 处理交叉口直行与左转直线的重叠关系:把长的更新为长的比短的多的部分.
    -- 条件: 1满足covers关系;2起点相同,终点不同;3裁剪段line的顶点为2;4裁剪不唯一时,取x小或者y小进行裁剪
    -- 切割更新后,oracle随机产生反转,需要处理反转的线 
-- a 切割更新
UPDATE GD_NAV_TRAFFIC_2 A SET A.POLYLINE = 
    (SELECT SDO_GEOM.SDO_DIFFERENCE(A.polyline,B.polyline,0.05)
     FROM GD_NAV_TRAFFIC_2 B,TMP_COVERS C WHERE A.SEQ = C.LSEQ AND B.SEQ = C.SSEQ )
WHERE EXISTS (SELECT 1 FROM TMP_COVERS C WHERE A.SEQ = C.LSEQ);
-- b 修复反转
UPDATE GD_NAV_TRAFFIC_2 M SET M.POLYLINE = SDO_UTIL.REVERSE_LINESTRING(M.POLYLINE) 
WHERE EXISTS (SELECT 1 FROM (SELECT AA.SEQ,CC.X,CC.Y FROM GD_NAV_TRAFFIC_2 AA,TABLE(SDO_UTIL.GETVERTICES(AA.POLYLINE)) CC
                             WHERE CC.ID=SDO_UTIL.GETNUMVERTICES (AA.POLYLINE) AND EXISTS(SELECT 1 FROM TMP_COVERS BB WHERE AA.SEQ = BB.LSEQ )) N 
              WHERE M.SEQ = N.SEQ AND (M.P2.SDO_POINT.X<>N.X OR M.P2.SDO_POINT.Y<>N.Y));
-- c 手动调头. 认为a中切割部分(交叉口直行与左转直线的重叠)是可以掉头的,插入切割部分反转线,为其它方向提供调头车道
   -- 1取切割完成后的路段;2与其它路网touch的数目必须>=5才认为是交叉口(经大量观察); 3此步骤会产生大量重复线,需再清理一下重复线
INSERT INTO GD_NAV_TRAFFIC_2 
WITH
    CUT AS
(SELECT A.* FROM GD_NAV_TRAFFIC_2 A, TMP_COVERS C WHERE A.SEQ = C.LSEQ),
    FIT_SEQ AS
(SELECT /*+ ORDERED */ A.SEQ FROM CUT A,GD_NAV_TRAFFIC_2 B
 WHERE SDO_TOUCH(B.POLYLINE,A.POLYLINE) = 'TRUE' GROUP BY A.SEQ HAVING COUNT(DISTINCT B.SEQ) >= 5)
SELECT SEQ_GD_NAV_TRAFFIC.nextval SEQ,'tmc' DEPT,-1 tmcid,'手动调头' ACTION,-1 DISTANCE,-1 DURATION,-1 SPEED,NULL ORIENTATION,
       ROAD,NULL STATUS,INSERT_TIME,SDO_UTIL.REVERSE_LINESTRING(A.POLYLINE) POLYLINE,NULL LNG,NULL NUMVER,NULL P1,NULL P2
FROM GD_NAV_TRAFFIC_2 A WHERE A.SEQ IN (SELECT SEQ FROM FIT_SEQ)
;
-- 1.3: 删除重合的线条. 简约去重,首尾点相同即认为重复
DELETE FROM GD_NAV_TRAFFIC_2 WHERE SEQ IN(
SELECT SEQ FROM  
(SELECT AA.SEQ, ROW_NUMBER() OVER (PARTITION BY BB.X || BB.Y, CC.X || CC.Y ORDER BY SEQ) RN  
FROM GD_NAV_TRAFFIC_2 AA,table(SDO_UTIL.GETVERTICES(AA.POLYLINE)) BB, table(SDO_UTIL.GETVERTICES(AA.POLYLINE)) CC
WHERE BB.ID=1 AND CC.ID=SDO_UTIL.GETNUMVERTICES (AA.POLYLINE))
WHERE RN>1 GROUP BY SEQ)
;
-- 1.4: 更新 LNG,NUMVER,P1,P2
UPDATE GD_NAV_TRAFFIC_2 M SET (M.LNG,M.NUMVER,M.P1,M.P2) = (
    SELECT ROUND(SDO_GEOM.SDO_LENGTH(aa.POLYLINE,0.05,'UNIT=METER'),3) lng,  --长度
    SDO_UTIL.GETNUMVERTICES (AA.POLYLINE) NUMVER,   --顶点数
    SDO_GEOMETRY(2001,4326,SDO_POINT_TYPE(BB.X,BB.Y,NULL),NULL,NULL) P1,SDO_GEOMETRY(2001,4326,SDO_POINT_TYPE(CC.X,CC.Y,NULL),NULL,NULL) P2
    FROM GD_NAV_TRAFFIC_2 AA, table(SDO_UTIL.GETVERTICES(AA.POLYLINE)) BB,table(SDO_UTIL.GETVERTICES(AA.POLYLINE)) CC
    WHERE BB.ID = 1 AND CC.ID = SDO_UTIL.GETNUMVERTICES (AA.POLYLINE) AND M.SEQ = AA.SEQ
);
-- 1.5: 更新双向道路线形至相同. 首尾点交叉相同,且不满足equal关系
DROP TABLE TMP_EQUAL;
CREATE TABLE TMP_EQUAL AS
SELECT /*+ ORDERED */ B.SEQ,SDO_UTIL.REVERSE_LINESTRING(A.POLYLINE) POLYLINE
FROM GD_NAV_TRAFFIC_2 A,GD_NAV_TRAFFIC_2 B
WHERE A.P1.SDO_POINT.X = B.P2.SDO_POINT.X AND A.P1.SDO_POINT.Y = B.P2.SDO_POINT.Y AND A.P2.SDO_POINT.X = B.P1.SDO_POINT.X AND A.P2.SDO_POINT.Y = B.P1.SDO_POINT.Y
AND A.SEQ<B.SEQ AND SDO_EQUAL(B.POLYLINE,A.POLYLINE) <> 'TRUE'
;
UPDATE GD_NAV_TRAFFIC_2 M SET M.POLYLINE = (SELECT B.POLYLINE FROM TMP_EQUAL B WHERE M.SEQ = B.SEQ) WHERE EXISTS (SELECT 1 FROM TMP_EQUAL B WHERE M.SEQ = B.SEQ);										
										
										
										
										
										
