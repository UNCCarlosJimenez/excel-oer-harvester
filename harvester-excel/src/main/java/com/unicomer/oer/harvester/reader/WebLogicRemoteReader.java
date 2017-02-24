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
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.oracle.artifact.ArtifactAlgorithm;
import com.oracle.oer.integration.harvester.RemoteQuery;
import com.oracle.oer.sync.framework.MetadataIntrospectionConfig;
import com.oracle.oer.sync.framework.MetadataIntrospectionException;
import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.oracle.oer.sync.framework.MetadataReader;
import com.oracle.oer.sync.model.Entity;
import com.unicomer.oer.harvester.model.UnicomerEntity;
import com.unicomer.oer.harvester.util.PropertiesLoader;
import com.unicomer.oer.harvester.writer.YamlWriter;

/**
 * @author carlosj_rodriguez
 *
 */
public class WebLogicRemoteReader implements MetadataReader {
	private static MetadataLogger logger = MetadataManager.getLogger(WebLogicRemoteReader.class);
	private MetadataIntrospectionConfig config = null;
	private String urlString = "";//"http://uinhsap1wldev.datacenter.milady.local:7091/";
	private String username = "";//"weblogic";
	private String password = "";//"Un1c0m3r";
	
	PropertiesLoader prop = PropertiesLoader.getInstance();
	private String jndiPath = prop.getProperty("weblogic.jndi-path");
	private String rootMBean =  prop.getProperty("weblogic.root-mbean");
//	private String datasourceAssetType = prop.getProperty("weblogic.datasource.asset-type");	
	private String deploymentAssetType = prop.getProperty("weblogic.deployment.asset-type");
	private String librarytAssetType = prop.getProperty("weblogic.library.asset-type");
	private String serverAssetType = prop.getProperty("weblogic.server.asset-type"); 
	private String defVersion = prop.getProperty("default.version");
	private String defAppToServerRelation = prop.getProperty("default.app-to-server-relation");
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		WebLogicRemoteReader reader = new WebLogicRemoteReader();
		
		Iterator<Set<Entity>> it= reader.read().iterator();
		while (it.hasNext()){
			logger.info("Activos obtenidos:");
			Set<Entity> entities = it.next();
			for(Entity entity : entities){
				logger.info("Entity name: " + entity.getDescription());
			}
		}
	}
	
	public WebLogicRemoteReader(){
		try{
			MetadataManager metadataManager = MetadataManager.getInstance();
		    this.config = metadataManager.getConfigManager();
		    RemoteQuery remote = this.config.getRemoteQuery();
		    
		    urlString = remote.getUri();
			username = remote.getCredentials().getUser();
			password = remote.getCredentials().getPassword();
		}catch(Exception e){
			logger.error("Failure staring WebLogicDeploymentRemoteReader... " + e.getMessage());
			
			urlString = "http://uinhsap1wldev.datacenter.milady.local:7091/";
			username = "weblogic";
			password = "Un1c0m3r";
		}
	}
	
	public List<Set<Entity>> read() throws Exception {
		List<Set<Entity>> list = new ArrayList<Set<Entity>>();
		
		HashMap<String, Entity> serverMap = new HashMap<String, Entity>();
		HashMap<String, Entity> applicationMap = new HashMap<String, Entity>();
		HashMap<String, Entity> libraryMap = new HashMap<String, Entity>();
		
		try{
			final ObjectName service;
			UnicomerEntity serverEntity = null;
			UnicomerEntity applicationEntity = null;
			UnicomerEntity libraryEntity = null;
						
			JMXConnector jmxConn = initConnection(urlString, username, password, jndiPath);
			MBeanServerConnection mbconn = jmxConn.getMBeanServerConnection();
			
			service = new ObjectName(rootMBean);
			
			ObjectName[] serverRuntimes = (ObjectName[]) mbconn.getAttribute(service, "ServerRuntimes");
			logger.info("Se obtiene " + serverRuntimes.length
					+ " servidores manejados del dominio. Para cada servidor manejado se obtienen aplicaciones y datasources");
						
			for (ObjectName serverRuntime:serverRuntimes) {
				String serverName = (String) mbconn.getAttribute(serverRuntime, "Name");
				logger.info("> " + serverName);
				serverEntity = new UnicomerEntity(serverAssetType, serverName, serverName, serverName, defVersion, ArtifactAlgorithm.DEFAULT);
				serverMap.put(serverName, serverEntity);
				
//				logger.info("  Aplicaciones desplegadas:");
				ObjectName[] applicationRuntimes = (ObjectName[]) mbconn.getAttribute(serverRuntime, "ApplicationRuntimes");
				for (ObjectName applicationRuntime:applicationRuntimes) {
					String applicationName = (String) mbconn.getAttribute(applicationRuntime, "Name");
					if (applicationMap.containsKey(applicationName)){
						//Se actualiza la referencia
						applicationEntity = (UnicomerEntity) applicationMap.get(applicationName);
						applicationMap.remove(applicationName);
					}else{
						// Se crea la referencia
						applicationEntity = new UnicomerEntity(deploymentAssetType, applicationName, applicationName, applicationName, defVersion, ArtifactAlgorithm.DEFAULT);
						applicationEntity.addCategorization("AssetFunction", "Creacion manual");
					}
					applicationEntity.addRelationship(serverEntity, defAppToServerRelation, false);
					applicationMap.put(applicationName, applicationEntity);
					logger.info("    " + applicationName);
					
//					ObjectName[] componentRuntimes = (ObjectName[]) mbconn.getAttribute(applicationRuntime, "ComponentRuntimes");
//					
//					for (ObjectName componentRuntime:componentRuntimes) {
//						String componentType = (String) mbconn.getAttribute(componentRuntime, "Type");
////						logger.info("        " + (String) mbconn.getAttribute(componentRuntime, "Name")
////								+ " de tipo " + componentType);
//						
//						if (componentType.toString().equals("WebAppComponentRuntime")) {
//							ObjectName[] servletRTs = (ObjectName[]) mbconn.getAttribute(componentRuntime,"Servlets");
//							int servletLength = (int) servletRTs.length;
//							for (int z = 0; z < servletLength; z++) {
//								path = (String) mbconn.getAttribute(servletRTs[z], "ContextPath");
////								logger.info(
////										" Servlet name: " + (String) mbconn.getAttribute(servletRTs[z], "Name"));
////								logger.info(" Servlet context path: "
////										+ (String) mbconn.getAttribute(servletRTs[z], "ContextPath"));
////								logger.info(" Invocation Total Count : "
////										+ (Object) mbconn.getAttribute(servletRTs[z], "InvocationTotalCount"));
//							}
//						}
//					}					
				}
				
				logger.info("Librerias desplegadas:");
				ObjectName[] libraryRuntimes = (ObjectName[]) mbconn.getAttribute(serverRuntime, "LibraryRuntimes");
				
				for (ObjectName library : libraryRuntimes) {
					String libraryName = (String) mbconn.getAttribute(library, "LibraryName");
					String libraryVersion = (String) mbconn.getAttribute(library, "SpecificationVersion");
					
					if (libraryMap.containsKey(libraryName)){
						//Se actualiza la referencia
						libraryEntity = (UnicomerEntity) libraryMap.get(libraryName);
						libraryMap.remove(libraryName);
					}else{
						// Se crea la referencia
						libraryEntity = new UnicomerEntity(librarytAssetType, libraryName, libraryName, libraryName, libraryVersion, ArtifactAlgorithm.DEFAULT);
						libraryEntity.addCategorization("AssetFunction", "Creacion manual");
					}
					libraryEntity.addRelationship(serverEntity, defAppToServerRelation, false);
					libraryMap.put(libraryName, libraryEntity);
					logger.info("    " + libraryName);
					
					ObjectName[] referencingRuntimes = (ObjectName[]) mbconn.getAttribute(library, "ReferencingRuntimes");
					for (ObjectName reference : referencingRuntimes) {
						String referenceName = (String) mbconn.getAttribute(reference, "Name");
						logger.info("        " + referenceName);
						
//						entity = new ArtifactEntity("Artifact: Jar", "", (String) mbconn.getAttribute(reference, "Name"), "", (String) mbconn.getAttribute(library, "Name"), "1.0", ArtifactAlgorithm.DEFAULT);
						
					}
				}
			}
			
			// Set<ObjectInstance> serverRT = mbconn.queryMBeans(null, null);
			// logger.info("got server runtimes: "+serverRT.size());
			// int length = (int) serverRT.size();
			//
			// Iterator<ObjectInstance> it = serverRT.iterator();
			// while(it.hasNext()){
			// ObjectInstance objectInstance = it.next();
			// logger.info(objectInstance.toString());
			// logger.info(objectInstance.getObjectName().getKeyPropertyListString());
			// logger.info(objectInstance.getObjectName().getDomain());
			// logger.info("");
			// }
			jmxConn.close();
		}catch(Exception e){
			System.err.println("Ha ocurrido un problema: " + e.getMessage());
			e.printStackTrace();
		}
		list.add(new HashSet<Entity>(serverMap.values()));
		list.add(new HashSet<Entity>(applicationMap.values()));
		list.add(new HashSet<Entity>(libraryMap.values()));
		
		for(Set<Entity> entitySet : list){
			if(entitySet != null && entitySet.size() > 0){
				YamlWriter.writeToYaml(entitySet, prop.getProperty("weblogic.harvest-type"));
			}
		}
		return list;
	}
	
	private static JMXConnector initConnection(String urlString, String username, String password, String jndiPath)
			throws MetadataIntrospectionException {
		String lProtocol = "t3";
		if ((urlString.toLowerCase().startsWith("https")) || (urlString.toLowerCase().startsWith("t3s"))) {
			lProtocol = "t3s";
		}
		try {
			URI uri = new URI(urlString);
			if (uri.getHost() == null) {
				uri = new URI(new StringBuilder().append(lProtocol).append("://").append(urlString).toString());
			}
			if ((uri.getHost() == null) || (uri.getPort() < 0)) {
				throw new MetadataIntrospectionException(
						"Invalid argument: -remote_url.  Expected a URL in the form of <host>:<port>");
			}
			
			JMXServiceURL serviceURL = new JMXServiceURL(lProtocol, uri.getHost(), uri.getPort(), jndiPath);
			
			logger.info(new StringBuilder().append("Connecting to: ").append(serviceURL).toString());
			Hashtable<String, String> h = new Hashtable<String, String>();
			h.put("java.naming.security.principal", username);
			h.put("java.naming.security.credentials", password);
			h.put("jmx.remote.protocol.provider.pkgs", "weblogic.management.remote");

			JMXConnector connector = JMXConnectorFactory.newJMXConnector(serviceURL, h);
			connector.connect();
			return connector;
		} catch (URISyntaxException e) {
			throw new MetadataIntrospectionException(
					new StringBuilder().append("Unable to parse remote server URL: ").append(urlString).toString(), e);
		} catch (MalformedURLException e) {
			throw new MetadataIntrospectionException(
					new StringBuilder().append("Unable to parse remote server URL: ").append(urlString).toString(), e);
		} catch (IOException e) {
			throw new MetadataIntrospectionException(new StringBuilder().append("Unable to connect to remote server: ")
					.append(urlString).append(".  Please make sure the remote URL, username, and password are correct.")
					.toString(), e);
		}
	}

	public void close() throws IOException {
				
	}

}
