--------------------- 2020年12月更新-------------------------  
功能一:  
南京市东南大学至新街口区域,道路车速获取的代码.  
  核心代码:  
  src/main/entity/GdNavLinkNJ.java  
  src/main/gaode/GetNavNJ.java  
--------------------- end -------------------------    
  
  
--------------------- 2020年1月更新-------------------------    
功能二:  
利用高德路径规划接口获取路网  
    核心代码:  
    src/main/entity/GdNavLink_hibernate.java  
    src/main/gaode/GetNavTrafficHibernate.java  
    "SQL部分.sql"  
------------------------ end -------------------------  
  
    
概述----------  
语言: JAVA  
项目管理方式: maven  
数据库: ORACLE  
持久化框架: hibernate. 使用该持久化框架的目的是方便大家切换数据库为自己的偏好(如postSQL/MySQL等)    

  
  
如何使用----------  
1. 构建maven工程  
2. maven无法自动添加oracle数据库的jdbc驱动，需要手动安装该驱动(ojdbc6-11.2.0.1.0.jar),oracle版权原因, 我就不上传这个jar包了, 需要请自行百度,联系我也行.  
3. 在main/resources/META-INF/persistence.xml中修改数据库连接参数  
4. 在getHttpReq方法中填入你自己申请的KEY  
5. SQL部分。仅功能二用到,功能一不用。在数据库中新建表GD_NAV_POINT和GD_NAV_TRAFFIC,详见“SQL部分.sql”，可以用你自己的方法构造OD点对，也可以参照公众号教程中的操作实例  
6. 运行: 功能一: 运行src/main/java/gaode/GetNavNJ.java 中的main方法; 功能二: 运行src/main/java/gaode/GetNavTrafficHibernate.java中的main方法.  
7. SQL部分(仅功能二需要运行)。回到“SQL部分.sql”， 进行连通性和拓扑性的批量处理。  
8. 最后在数据库中导出表GD_NAV_TRAFFIC_2， 即为最终路网。  
  
  
建议:   
1. 建议先学习功能二, 再学习功能一. 不然你很难理解功能一每个步骤的意图(尽管有注释).    
2. 看代码时一定要结合着公众号文章教程, 原因同上.  
  
  
作者公众号: 数牍小点子  
微信: xuxinkun0591  
