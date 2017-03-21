/**
 * 
 */
package com.unicomer.oer.harvester.reader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.AdminClientFactory;
import com.ibm.websphere.management.exception.ConnectorException;
import com.oracle.artifact.ArtifactAlgorithm;
import com.oracle.oer.integration.harvester.RemoteQuery;
import com.oracle.oer.sync.framework.MetadataIntrospectionConfig;
import com.oracle.oer.sync.framework.MetadataIntrospectionException;
import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.oracle.oer.sync.framework.MetadataReader;
import com.oracle.oer.sync.model.Entity;
import com.unicomer.oer.harvester.model.UnicomerEntity;

/**
 * @author carlosj_rodriguez
 *
 */
public class WebSphereRemoteReader implements MetadataReader {
	private static MetadataLogger logger = MetadataManager.getLogger(WebSphereRemoteReader.class);
	private MetadataIntrospectionConfig config = null;
	private String urlString = "";
	private String username = "";
	private String password = "";
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		WebSphereRemoteReader reader = new WebSphereRemoteReader();
		
//		Iterator<Set<Entity>> it= reader.read().iterator();
//		while (it.hasNext()){
//			logger.info("Activos obtenidos:");
//			Set<Entity> entities = it.next();
//			for(Entity entity : entities){
//				logger.info("Entity name: " + entity.getDescription());
//			}
//		}
	}
	
	public WebSphereRemoteReader(){
		try{
			MetadataManager metadataManager = MetadataManager.getInstance();
		    this.config = metadataManager.getConfigManager();
		    RemoteQuery remote = this.config.getRemoteQuery();
		    
		    URI uri = new URI(remote.getUri());
		    urlString = uri.getHost() + ":" + uri.getPort();
			username = remote.getCredentials().getUser();
			password = remote.getCredentials().getPassword();
		}catch(Exception e){
//			logger.error("Failure staring WebLogicDeploymentRemoteReader... " + e.getMessage());
			
			urlString = "regalwasd.siman.com:9043";
			username = "summeradmin";
			password = "desaaix";
		}
	}
	
	public List<Set<Entity>> read() throws Exception {
		List<Set<Entity>> list = new ArrayList<Set<Entity>>();
		
		HashMap<String, Entity> serverMap = new HashMap<String, Entity>();
		HashMap<String, Entity> applicationMap = new HashMap<String, Entity>();
		HashMap<String, Entity> libraryMap = new HashMap<String, Entity>();
		
		try{
			final ObjectName service;
			String jndiPath = "/jndi/weblogic.management.mbeanservers.domainruntime";// "/jndi/weblogic.management.mbeanservers.runtime";
			UnicomerEntity serverEntity = null;
			UnicomerEntity applicationEntity = null;
			UnicomerEntity libraryEntity = null;
			
//			service = new ObjectName(
//					// "com.bea:Name=DomainRuntimeService,Type=*");
//					"com.bea:Name=DomainRuntimeService,Type=weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean");
//			
//			ObjectName[] serverRuntimes = (ObjectName[]) mbconn.getAttribute(service, "ServerRuntimes");
//			logger.info("Se obtiene " + serverRuntimes.length
//					+ " servidores manejados del dominio. Para cada servidor manejado se obtienen aplicaciones y datasources");
//						
//			for (ObjectName serverRuntime:serverRuntimes) {
//				String serverName = (String) mbconn.getAttribute(serverRuntime, "Name");
//				logger.info("> " + serverName);
//				serverEntity = new UnicomerEntity("Environment : Application Server", serverName, serverName, serverName, "1.0.0", ArtifactAlgorithm.DEFAULT);
//				serverMap.put(serverName, serverEntity);
//				
////				logger.info("  Aplicaciones desplegadas:");
//				ObjectName[] applicationRuntimes = (ObjectName[]) mbconn.getAttribute(serverRuntime, "ApplicationRuntimes");
//				for (ObjectName applicationRuntime:applicationRuntimes) {
//					String applicationName = (String) mbconn.getAttribute(applicationRuntime, "Name");
//					if (applicationMap.containsKey(applicationName)){
//						//Se actualiza la referencia
//						applicationEntity = (UnicomerEntity) applicationMap.get(applicationName);
//						applicationMap.remove(applicationName);
//					}else{
//						// Se crea la referencia
//						applicationEntity = new UnicomerEntity("Application", applicationName, applicationName, applicationName, "1.0.0", ArtifactAlgorithm.DEFAULT);
//						applicationEntity.addCategorization("AssetFunction", "Creacion manual");
//					}
//					applicationEntity.addRelationship(serverEntity, "Deployment", false);
//					applicationMap.put(applicationName, applicationEntity);
//					logger.info("    " + applicationName);
//					
////					ObjectName[] componentRuntimes = (ObjectName[]) mbconn.getAttribute(applicationRuntime, "ComponentRuntimes");
////					
////					for (ObjectName componentRuntime:componentRuntimes) {
////						String componentType = (String) mbconn.getAttribute(componentRuntime, "Type");
//////						logger.info("        " + (String) mbconn.getAttribute(componentRuntime, "Name")
//////								+ " de tipo " + componentType);
////						
////						if (componentType.toString().equals("WebAppComponentRuntime")) {
////							ObjectName[] servletRTs = (ObjectName[]) mbconn.getAttribute(componentRuntime,"Servlets");
////							int servletLength = (int) servletRTs.length;
////							for (int z = 0; z < servletLength; z++) {
////								path = (String) mbconn.getAttribute(servletRTs[z], "ContextPath");
//////								logger.info(
//////										" Servlet name: " + (String) mbconn.getAttribute(servletRTs[z], "Name"));
//////								logger.info(" Servlet context path: "
//////										+ (String) mbconn.getAttribute(servletRTs[z], "ContextPath"));
//////								logger.info(" Invocation Total Count : "
//////										+ (Object) mbconn.getAttribute(servletRTs[z], "InvocationTotalCount"));
////							}
////						}
////					}					
//				}
//				
//				logger.info("Librerias desplegadas:");
//				ObjectName[] libraryRuntimes = (ObjectName[]) mbconn.getAttribute(serverRuntime, "LibraryRuntimes");
//				
//				for (ObjectName library : libraryRuntimes) {
//					String libraryName = (String) mbconn.getAttribute(library, "LibraryName");
//					String libraryVersion = (String) mbconn.getAttribute(library, "SpecificationVersion");
//					
//					if (libraryMap.containsKey(libraryName)){
//						//Se actualiza la referencia
//						libraryEntity = (UnicomerEntity) libraryMap.get(libraryName);
//						libraryMap.remove(libraryName);
//					}else{
//						// Se crea la referencia
//						libraryEntity = new UnicomerEntity("Application", libraryName, libraryName, libraryName, libraryVersion, ArtifactAlgorithm.DEFAULT);
//						libraryEntity.addCategorization("AssetFunction", "Creacion manual");
//					}
//					libraryEntity.addRelationship(serverEntity, "Deployment", false);
//					libraryMap.put(libraryName, libraryEntity);
//					logger.info("    " + libraryName);
//					
//					ObjectName[] referencingRuntimes = (ObjectName[]) mbconn.getAttribute(library, "ReferencingRuntimes");
//					for (ObjectName reference : referencingRuntimes) {
//						String referenceName = (String) mbconn.getAttribute(reference, "Name");
//						logger.info("        " + referenceName);
//						
////						entity = new ArtifactEntity("Artifact: Jar", "", (String) mbconn.getAttribute(reference, "Name"), "", (String) mbconn.getAttribute(library, "Name"), "1.0", ArtifactAlgorithm.DEFAULT);
//						
//					}
//				}
//			}
//			
//			// Set<ObjectInstance> serverRT = mbconn.queryMBeans(null, null);
//			// logger.info("got server runtimes: "+serverRT.size());
//			// int length = (int) serverRT.size();
//			//
//			// Iterator<ObjectInstance> it = serverRT.iterator();
//			// while(it.hasNext()){
//			// ObjectInstance objectInstance = it.next();
//			// logger.info(objectInstance.toString());
//			// logger.info(objectInstance.getObjectName().getKeyPropertyListString());
//			// logger.info(objectInstance.getObjectName().getDomain());
//			// logger.info("");
//			// }
		}catch(Exception e){
			System.err.println("Ha ocurrido un problema: " + e.getMessage());
			e.printStackTrace();
		}
		list.add(new HashSet<Entity>(serverMap.values()));
		list.add(new HashSet<Entity>(applicationMap.values()));
		list.add(new HashSet<Entity>(libraryMap.values()));
		
		return list;
	}
	
	@SuppressWarnings("unused")
	private AdminClient getAdminClient(String host, String port, String username, String password, String connectorType) 
			throws MetadataIntrospectionException{
		Properties connectProps = new Properties();
		connectProps.setProperty(AdminClient.CONNECTOR_TYPE, connectorType);
		connectProps.setProperty(AdminClient.CONNECTOR_HOST, host);
		connectProps.setProperty(AdminClient.CONNECTOR_PORT, port);
		connectProps.setProperty(AdminClient.USERNAME, username);
		connectProps.setProperty(AdminClient.PASSWORD, password);
//		System.setProperty("com.ibm.CORBA.ConfigURL",
//				"file:C:/AA/cf010839.26/profiles/AppSrv02/properties/sas.client.props");
//		System.setProperty("com.ibm.SSL.ConfigURL",
//				"file:C:/AA/cf010839.26/profiles/AppSrv02/properties/ssl.client.props");
		AdminClient adminClient = null;
		try {
			adminClient = AdminClientFactory.createAdminClient(connectProps);
			System.out.println("AdminClient connected!");
		} catch (Exception e) {
			throw new MetadataIntrospectionException("Exception creating admin client: " + e.getMessage(), e);
		}
		return adminClient;
	}
	
	@SuppressWarnings("unused")
	private static JMXConnector initConnection(String hostname, String port, String username, String password)
			throws MetadataIntrospectionException {
		JMXConnector connector = null;
		try {
			String jndiPath = "";
			JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://" + hostname + ":" + port + jndiPath);
			
			// JMXServiceURL url = new
			// JMXServiceURL("service:jmx:iiop://"+hostname+"/jndi/corbaname:iiop:"+hostname+":"+port+jndiPath);
			Hashtable<String, String> h = new Hashtable<String, String>();

			h.put("jmx.remote.credentials", "{" + username + ", " + password + " }");
			h.put("java.naming.factory.initial", "com.ibm.websphere.naming.WsnInitialContextFactory");
			
			connector = JMXConnectorFactory.connect(url, h);

		} catch (Exception e) {
			throw new MetadataIntrospectionException("Error while trying to connect to " + hostname + ":" + port, e);
		}
		return connector;
	}
	
	public void close() throws IOException {
		
	}

}
