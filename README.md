--------------------- 2020年12月更新-------------------------  
增加了南京市东南大学至新街口区域,道路车速获取的代码.  
  核心代码:  
  entity/GdNavLinkNJ.java  
  gaode/GetNavNJ.java  
--------------------- end -------------------------    

--------------------- 2020年8月更新-------------------------  
1.增加了多线程访问接口,速度大大提升,开启5个线程时,速度可达每秒十几次.  
2.去掉了junit相关东西,改成了常规的main方法作为入口.  
3.优化代码结构,可读性增强.  
------------------------ end -------------------------  
  
    
利用高德导航接口创建交通路网模型源代码.  
语言: JAVA  
项目管理方式: maven  
持久化框架: hibernate  
数据库: ORACLE  
  
如何使用  
1. 构建maven工程  
2. maven无法自动添加oracle数据库的jdbc驱动，需要手动安装该驱动(ojdbc6-11.2.0.1.0.jar),oracle版权原因, 我就不上传这个jar包了, 需求请自行百度,联系我也行.  
3. 在main/resources/META-INF/persistence.xml中修改数据库连接参数  
4. 在getHttpReq方法中填入你自己申请的KEY  
5. SQL部分。在数据库中新建表GD_NAV_POINT和GD_NAV_TRAFFIC,详见“SQL部分.sql”，可以用你自己的方法构造OD点对，也可以参照公众号教程中的操作实例  
6. 运行gaode2/src/main/java/gaode/GetNav.java 中的main方法
7. SQL部分。回到“SQL部分.sql”， 进行连通性和拓扑性的批量处理。  
8. 最后在数据库中到出表GD_NAV_TRAFFIC_2， 即为最终路网。  
  
  
有问题欢迎交流   
本人邮箱: xuxinkun@189.cn  
微信: xuxinkun0591  
