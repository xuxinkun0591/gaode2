package utils;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtils {
	private static SessionFactory sf;

	static {
		// 1 创建,调用空参构造,默认加载hibernate.cfg.xml,如需个性化加载,可改变configure("配置文件位置")
		Configuration conf = new Configuration().configure("oracle_hibernate.cfg.xml");
		// 2 根据配置信息,创建 SessionFactory对象
		sf = conf.buildSessionFactory();
	}

	// 获得session => 获得全新session
	public static Session openSession() {
		// 3 获得session
		Session session = sf.openSession();
		return session;

	}

	// 获得session => 获得与线程绑定的session
	public static Session getCurrentSession() {
		// 3 获得session
		Session session = sf.getCurrentSession();
		return session;
	}

}
