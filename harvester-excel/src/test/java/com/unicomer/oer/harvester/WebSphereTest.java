/**
 * 
 */
package com.unicomer.oer.harvester;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.AdminClientFactory;
import com.ibm.websphere.management.AdminServiceFactory;
import com.ibm.websphere.management.Session;
import com.ibm.websphere.management.configservice.ConfigService;
import com.ibm.websphere.management.configservice.ConfigServiceProxy;
import com.ibm.websphere.management.exception.ConnectorException;
import com.ibm.websphere.models.config.adminservice.AdminserviceFactory;
import com.ibm.ws.management.AdminServiceFactoryInitializer;
import com.oracle.oer.sync.framework.MetadataIntrospectionException;

/**
 * @author carlosj_rodriguez
 *
 */
public class WebSphereTest {
	private static String host = "";
	private static String port = "0";
	private static String username = "";
	private static String password = "";

	public static void main(String[] args) throws Exception {
		host = "192.168.130.179";
		port = "9100";
		username = "summeradmin";
		password = "desaaix";
		/// WsnAdminNameService#JMXConnector
		/// jndi/JMXConnector
		// JMXConnector jmxConn = initConnection(host, port, username, password,
		// "/WsnAdminNameService#JMXConnector");
		JMXConnector jmxConn = initConnection(host, Integer.parseInt(port), username, password, "/jndi/JMXConnector");
		MBeanServerConnection mbconn = jmxConn.getMBeanServerConnection();
		ObjectName service = new ObjectName("WebSphere:type=NodeAgent,node=REGALWASDNode01,*");
		
		Set<ObjectName> setNodeAgent = mbconn.queryNames(service, null);
		
		if(setNodeAgent!=null){
			System.out.println("true");
			Iterator<ObjectName> it = setNodeAgent.iterator();
			while(it.hasNext()){
				ObjectName nodeAgent = it.next();
				
				MBeanInfo info = mbconn.getMBeanInfo(nodeAgent);
				System.out.println("ClassName="+info.getClassName());
				
			}
		}else{
			System.out.println("false");
		}
//		
//		ObjectName[] nodeAgentSet = (ObjectName[]) mbconn.getAttribute(service, "ServerRuntimes");
//		System.out.println("Se obtiene " + nodeAgentSet.length
//				+ " servidores manejados del dominio. Para cada servidor manejado se obtienen aplicaciones y datasources");
//		
//		
		
		
		
		
		jmxConn.close();
		
		AdminClient adminClient = getAdminClient(host, "9101", username, password, AdminClient.CONNECTOR_TYPE_RMI);
		ConfigService configService = new ConfigServiceProxy(adminClient);
		Session session = new Session();
//		ObjectName jvmQuery = new ObjectName("*:j2eeType=JVM,*");
		
		ObjectName[] jvmBeans = configService.resolve(session, "*:j2eeType=JVM,*");
		
		for(ObjectName jvmMBean : jvmBeans){
			MBeanInfo mbeanInfo=adminClient.getMBeanInfo(jvmMBean);
			System.out.println("JVM: "+jvmMBean.getCanonicalName());
			for (int i = 0; i < mbeanInfo.getAttributes().length; i++) {
				String attributeName = mbeanInfo.getAttributes()[i].getName();
				Object attributeValue = adminClient.getAttribute(jvmMBean, attributeName);
				System.out.println("Name: "+attributeName+" Value: "+((attributeValue!=null) ? attributeValue.toString():"null"));
			}
		}
		
	}

	private static JMXConnector initConnection(String hostname, int port, String username, String password,
			String jndiPath) {
		JMXConnector connector = null;
		try {
			JMXServiceURL url = new JMXServiceURL("service:jmx:iiop://" + hostname + ":" + port + jndiPath);
			System.out.println(
					"service:jmx:iiop://" + hostname + "/jndi/corbaname:iiop:" + hostname + ":" + port + jndiPath);
			
			// JMXServiceURL url = new
			// JMXServiceURL("service:jmx:iiop://"+hostname+"/jndi/corbaname:iiop:"+hostname+":"+port+jndiPath);
			Hashtable<String, String> h = new Hashtable<String, String>();

			h.put("jmx.remote.credentials", "{" + username + ", " + password + " }");
			h.put("java.naming.factory.initial", "com.ibm.websphere.naming.WsnInitialContextFactory");
			
			connector = JMXConnectorFactory.connect(url, h);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return connector;
	}

	@SuppressWarnings("unused")
	private static AdminClient getAdminClient(String host, String port, String username, String password, String connectorType) 
			throws MetadataIntrospectionException{
		Properties connectProps = new Properties();
		connectProps.setProperty(AdminClient.CONNECTOR_TYPE, connectorType);
		connectProps.setProperty(AdminClient.CONNECTOR_HOST, host);
		connectProps.setProperty(AdminClient.CONNECTOR_PORT, port);
		connectProps.setProperty(AdminClient.USERNAME, username);
		connectProps.setProperty(AdminClient.CONNECTOR_AUTO_ACCEPT_SIGNER, "true");
		connectProps.setProperty(AdminClient.PASSWORD, password);
		connectProps.setProperty(AdminClient.CONNECTOR_SECURITY_ENABLED, "true");
//		System.setProperty("com.ibm.CORBA.ConfigURL",
//				"file:C:/AA/cf010839.26/profiles/AppSrv02/properties/sas.client.props");
//		System.setProperty("com.ibm.SSL.ConfigURL",
//				"file:C:/AA/cf010839.26/profiles/AppSrv02/properties/ssl.client.props");
		AdminClient adminClient = null;
		try {
			adminClient = AdminClientFactory.createAdminClient(connectProps);
			Properties myProps = adminClient.getConnectorProperties();
			
			System.out.println("AdminClient connected!");
		} catch (Exception e) {
			throw new MetadataIntrospectionException("Exception creating admin client: " + e.getMessage(), e);
		}
		return adminClient;
	}
	
	
	
	
	
	
	
	
	
	
	@SuppressWarnings("unused")
	private static void initConnectionSOAP() {
		Properties connectProps = new Properties();
		connectProps.setProperty(AdminClient.CONNECTOR_TYPE, AdminClient.CONNECTOR_TYPE_SOAP);
		
		connectProps.setProperty(AdminClient.CONNECTOR_HOST, "192.168.130.179");
		connectProps.setProperty(AdminClient.CONNECTOR_PORT, "8879");
		connectProps.setProperty(AdminClient.USERNAME, "summeradmin");
		connectProps.setProperty(AdminClient.PASSWORD, "desaaix");
		AdminClient adminClient = null;
		try {
			adminClient = AdminClientFactory.createAdminClient(connectProps);
			System.out.println("initConnectionSOAP()=Connected!");
		} catch (ConnectorException e) {
			System.out.println("Exception creating admin client: " + e);
		}
	}

}