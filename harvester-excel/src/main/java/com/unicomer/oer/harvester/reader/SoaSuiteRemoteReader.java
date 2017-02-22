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
import java.util.LinkedHashSet;
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
import com.unicomer.oer.harvester.writer.YamlWriter;

/**
 * @author carlosj_rodriguez
 *
 */
public class SoaSuiteRemoteReader implements MetadataReader {
	private static MetadataLogger logger = MetadataManager.getLogger(SoaSuiteRemoteReader.class);
	private MetadataIntrospectionConfig config = null;
	private String urlString = "";
	private String username = "";
	private String password = "";
	private static JMXConnector jmxConn = null;
	
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		SoaSuiteRemoteReader reader = new SoaSuiteRemoteReader();
		
		Iterator<Set<Entity>> it= reader.read().iterator();
		while (it.hasNext()){
			logger.info("Activos obtenidos:");
			Set<Entity> entities = it.next();
			for(Entity entity : entities){
				logger.info("Entity name: " + entity.getDescription());
			}
		}
	}
	
	public SoaSuiteRemoteReader(){
		try{
			MetadataManager metadataManager = MetadataManager.getInstance();
		    this.config = metadataManager.getConfigManager();
		    RemoteQuery remote = this.config.getRemoteQuery();
		    
		    urlString = remote.getUri();
			username = remote.getCredentials().getUser();
			password = remote.getCredentials().getPassword();
		}catch(Exception e){
			logger.error("Failure staring SoaSuiteRemoteReader... " + e.getMessage());
			
			urlString = "http://ebsr12apdbdsoa.unicomer.com:7003/";
			username = "weblogic";
			password = "soadev123";
		}
	}
	
	public List<Set<Entity>> read() throws Exception {
		List<Set<Entity>> list = new ArrayList<Set<Entity>>();
		
		HashMap<String, Entity> serverMap = new HashMap<String, Entity>();
		HashMap<String, Entity> compositeMap = new HashMap<String, Entity>();
		try {
			UnicomerEntity compositeEntity = null;
			jmxConn = initConnection(urlString, username, password, "/jndi/weblogic.management.mbeanservers.runtime");
			MBeanServerConnection mbconn2 = jmxConn.getMBeanServerConnection();

			ObjectName serverQuery = new ObjectName(
					"com.bea:Name=RuntimeService,Type=weblogic.management.mbeanservers.runtime.RuntimeServiceMBean");
			String serverName = (String) mbconn2.getAttribute(serverQuery, "ServerName");
			Entity serverEntity = new UnicomerEntity("Environment : Application Server", serverName, serverName,
					serverName, "1.0.0", ArtifactAlgorithm.DEFAULT);
			serverMap.put(serverName, serverEntity);

			Set<ObjectName> compositeNames = new LinkedHashSet<ObjectName>();
			compositeNames
					.addAll(mbconn2.queryNames(new ObjectName("oracle.soa.config:j2eeType=SCAComposite,*"), null));

			if (compositeNames.isEmpty()) {
				throw new MetadataIntrospectionException("Found no Composites on the remote server");
			}

			for (ObjectName composite : compositeNames) {
				String name = (String) mbconn2.getAttribute(composite, "Name");
				String revision = (String) mbconn2.getAttribute(composite, "Revision");
				String partition = ((String) mbconn2.getAttribute(composite, "ApplicationName"));

				if (((String) mbconn2.getAttribute(composite, "Mode")).equals("retired")) {
					logger.warn("Skipped instrospection of " + name + "_rev" + revision
							+ "because it is retired in SOA Suite.");
				} else {
					System.out.println(name + "-" + revision + "-" + partition);
					if (compositeMap.containsKey(name)) {
						// Se actualiza la referencia
						compositeEntity = (UnicomerEntity) compositeMap.get(name);
						compositeMap.remove(name);
					} else {
						// Se crea la referencia
						compositeEntity = new UnicomerEntity("Application", name, name, name, revision,
								ArtifactAlgorithm.DEFAULT);
						compositeEntity.addCategorization("AssetFunction", "Creacion manual");
					}
					compositeEntity.addRelationship(serverEntity, "Deployment", false);
					compositeEntity.addCustomData("partition", partition);

					try {
						ObjectName[] services = (ObjectName[]) mbconn2.getAttribute(composite, "Services");
						for (ObjectName service : services) {
							// Bindings
							ObjectName[] bindings = (ObjectName[]) mbconn2.getAttribute(service, "Bindings");
							String endpoint = "";
							for (ObjectName binding : bindings) {
								String bindingType = (String) mbconn2.getAttribute(binding, "BindingType");
								if (bindingType.equals("binding.rest")) {
									endpoint = (String) mbconn2.getAttribute(binding, "Location");
								} else {
									endpoint = (String) mbconn2.getAttribute(binding, "EndpointAddressURI");

								}
							}
							if (endpoint != null && !endpoint.isEmpty())
								compositeEntity.addCustomData("endpoint", endpoint);
						}
					} catch (Exception e) {
						logger.fatal(e.getMessage());
					}
					compositeMap.put(name, compositeEntity);
				}
			}
			jmxConn.close();
			jmxConn = null;
		} catch (Exception e) {
			System.err.println("Ha ocurrido un problema: " + e.getMessage());
			e.printStackTrace();
		}
		list.add(new HashSet<Entity>(serverMap.values()));
		list.add(new HashSet<Entity>(compositeMap.values()));

		for(Set<Entity> entitySet : list){
			if(entitySet != null && entitySet.size() > 0){
				YamlWriter.writeToYaml(entitySet);
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
