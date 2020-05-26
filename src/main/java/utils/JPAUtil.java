package utils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.Session;

public class JPAUtil {
    private static final EntityManagerFactory emFactory;

    static {
        try {
            emFactory = Persistence.createEntityManagerFactory("SPATIAL-JPA");
        }catch(Throwable ex){
            System.err.println("Cannot create EntityManagerFactory.");
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static EntityManager createEntityManager() {
        return emFactory.createEntityManager();
    }

    public static void close(){
        emFactory.close();
    }
    //为了在JPA环境中使用hibernate API,此步骤获取hiber原生session
    public static Session getSession() {
    	//EntityManager em = JPAUtil.createEntityManager();
		//Session session = em.unwrap(Session.class);
    	return emFactory.createEntityManager().unwrap(Session.class);
    }
}
