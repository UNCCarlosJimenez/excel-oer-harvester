/**
 * 
 */
package com.unicomer.oer.harvester;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.oracle.artifact.ArtifactAlgorithm;
import com.oracle.oer.sync.framework.MetadataIntrospectionException;
import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.oracle.oer.sync.model.Entity;
import com.unicomer.oer.harvester.model.UnicomerEntity;
import com.unicomer.oer.harvester.util.PropertiesLoader;
import com.unicomer.oer.harvester.writer.YamlWriter;

import weblogic.utils.StringUtils;

/**
 * @author carlosj_rodriguez
 *
 */
public class WebLogicTest {
	private static MetadataLogger logger = MetadataManager.getLogger(WebLogicTest.class);
	private String urlString = "";
	private String username = "";
	private String password = "";
	
	PropertiesLoader prop = PropertiesLoader.getInstance("C:\\Users\\carlosj_rodriguez\\work\\exec\\harvester\\unicomer-harvester.properties");
	private String jndiPath = prop.getProperty("weblogic.jndi-path");
	private String rootMBean =  prop.getProperty("weblogic.root-mbean");
//	private String datasourceAssetType = prop.getProperty("weblogic.datasource.asset-type");	
	private String deploymentAssetType = prop.getProperty("weblogic.deployment.asset-type");
	private String librarytAssetType = prop.getProperty("weblogic.library.asset-type");
	private String serverAssetType = prop.getProperty("weblogic.server.asset-type"); 
	private String defVersion = prop.getProperty("default.version");
	private String defAppToServerRelation = prop.getProperty("default.app-to-server-relation");
	private String defLibToAppRelation = prop.getProperty("default.lib-to-app-relation");
	
	public static void main(String[] args) throws Exception {
		String valor = "ASBCHGPRCLSOLUNI01";
		System.out.println((valor.substring(valor.length() -2, valor.length())));
		System.out.println(Integer.valueOf(valor.substring(valor.length() -2, valor.length())));
		
//		WebLogicTest reader = new WebLogicTest();
//		reader.read();
//		
//		Iterator<Set<Entity>> it= reader.read().iterator();
//		while (it.hasNext()){
//			logger.info("Activos obtenidos:");
//			Set<Entity> entities = it.next();
//			for(Entity entity : entities){
//				logger.info("Entity name: " + entity.getDescription());
//			}
//		}
	}

	public WebLogicTest() {
		urlString = "http://uinhsap1wldev.datacenter.milady.local:7091/";
		username = "weblogic";
		password = "Un1c0m3r";

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
				String productVersion = (String) mbconn.getAttribute(serverRuntime, "WeblogicVersion");
				
				logger.info("> " + serverName);
				if (!serverName.equals(prop.getProperty("weblogic.admin-managed-server"))) {
					serverEntity = new UnicomerEntity(serverAssetType, serverName, serverName, serverName, defVersion, ArtifactAlgorithm.DEFAULT);
					serverEntity.addCategorization("LineOfBusiness", prop.getProperty("default.line-of-business"));
					serverEntity.addCategorization("AssetLifecycleStage", prop.getProperty("default.asset-lifecycle-stage"));
					serverEntity.addCategorization("ApplicationServer", prop.getProperty("weblogic.application-server"));
					serverEntity.addCategorization("Technology", prop.getProperty("default.technology"));
					serverEntity.addCategorization("Region", prop.getProperty("default.region"));
					serverEntity.addCustomData("product-version", productVersion);
					
					serverEntity.addCustomData("host-information/host/hostname", new URI(urlString).getHost());
			    	serverEntity.addCustomData("host-information/host/operative-system", "AIX");
			    	serverEntity.addCustomData("host-information/host/operative-system-version", "7.1");
			    	serverEntity.addCustomData("host-information/host/cpu-info", "12 cores");
			    	serverEntity.addCustomData("host-information/host/memory-info", "10 GB");
			    	serverEntity.addCustomData("host-information/host/storage-capacity", "235 GB");
					
					serverMap.put(serverName, serverEntity);
					
//					logger.info("  Aplicaciones desplegadas:");
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
						}
						applicationEntity.addRelationship(serverEntity, defAppToServerRelation, false);
						applicationEntity.addCategorization("LineOfBusiness", prop.getProperty("default.line-of-business"));
						applicationEntity.addCategorization("AssetLifecycleStage", prop.getProperty("default.asset-lifecycle-stage"));
						applicationEntity.addCategorization("Technology", prop.getProperty("default.technology"));
						applicationEntity.addCategorization("Region", prop.getProperty("default.region"));
						applicationEntity.addHarvesterProperty("Harvester Description", prop.getProperty("default.harvester-description"));
						applicationEntity.addCustomData("acquisition-method", prop.getProperty("default.acquisition-method"));
						
						applicationMap.put(applicationName, applicationEntity);
						logger.info("    " + applicationName);
						
						ObjectName[] componentRuntimes = (ObjectName[]) mbconn.getAttribute(applicationRuntime, "ComponentRuntimes");
						
						for (ObjectName componentRuntime:componentRuntimes) {
							String componentType = (String) mbconn.getAttribute(componentRuntime, "Type");							
							if (componentType.toString().equals("WebAppComponentRuntime")) {
								ObjectName[] servletRTs = (ObjectName[]) mbconn.getAttribute(componentRuntime,"Servlets");
								
								Set<String> urlSet = new HashSet<String>();
								for (ObjectName servletObject : servletRTs) {
									String url = (String) mbconn.getAttribute(servletObject, "URL");
									if(!StringUtils.isEmptyString(url)){
										urlSet.add(url);
									}
								}
								for(String url : urlSet){
									applicationEntity.addCustomData(prop.getProperty("weblogic.custom-data.environment"), prop.getProperty("default.environment"));
									applicationEntity.addCustomData(prop.getProperty("weblogic.custom-data.endpoint"), url);
									logger.debug("        " + url);
								}
							}
						}
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
						libraryEntity.addCategorization("LineOfBusiness", prop.getProperty("default.line-of-business"));
						libraryEntity.addCategorization("AssetLifecycleStage", prop.getProperty("default.asset-lifecycle-stage"));
						libraryEntity.addCategorization("Technology", prop.getProperty("default.technology"));
						libraryEntity.addCategorization("Region", prop.getProperty("default.region"));
						libraryEntity.addHarvesterProperty("Harvester Description", prop.getProperty("default.harvester-description"));
						libraryEntity.addCustomData("acquisition-method", prop.getProperty("default.acquisition-method"));
											
						libraryMap.put(libraryName, libraryEntity);
						logger.info("    " + libraryName);
						
						ObjectName[] referencingRuntimes = (ObjectName[]) mbconn.getAttribute(library, "ReferencingRuntimes");
						for (ObjectName reference : referencingRuntimes) {
							String referenceName = (String) mbconn.getAttribute(reference, "ApplicationIdentifier");
							logger.info("        " + referenceName);
							libraryEntity.addRelationship(applicationMap.get(referenceName), defLibToAppRelation, false);
						}
					}
				}
			}
			
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
		
	private String customDataArray(String valueOne, String valueTwo){
		String result = "";
		try {
            DocumentBuilderFactory dFact = DocumentBuilderFactory.newInstance();
            DocumentBuilder build = dFact.newDocumentBuilder();
            Document doc = build.newDocument();
                        
            Element root = doc.createElement("url-per-environment");
            doc.appendChild(root);
            
//        	Element urlPerEnvironment = doc.createElement("url-per-environment");
        	
        	Element environmentName = doc.createElement("environment-name");
            environmentName.setTextContent(valueOne);
            
            Element environmentUri = doc.createElement("uri");
            environmentUri.setTextContent(valueTwo);
            root.appendChild(environmentName);
            root.appendChild(environmentUri);
            
//            root.appendChild(urlPerEnvironment);
            
            TransformerFactory tFact = TransformerFactory.newInstance();
            Transformer trans = tFact.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            
            StringWriter writer = new StringWriter();
            StreamResult stream = new StreamResult(writer);
            DOMSource source = new DOMSource(doc);
            
            trans.transform(source, stream);
            result = writer.toString();
        } catch (TransformerException ex) {
            System.out.println("Error outputting document");
        } catch (ParserConfigurationException ex) {
            System.out.println("Error building document");
        }
		
		return result;
	}

}
