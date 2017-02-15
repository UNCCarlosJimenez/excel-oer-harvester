/**
 * 
 */
package com.unicomer.oer.harvester.reader;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.oracle.oer.sync.framework.MetadataReader;
import com.oracle.oer.sync.model.Entity;
import com.unicomer.oer.harvester.model.UnicomerEntity;

/**
 * @author carlosj_rodriguez
 *
 */
public class JBossRemoteReader implements MetadataReader {
	private static MetadataLogger logger = MetadataManager.getLogger(JBossRemoteReader.class);
	
	public List<Set<Entity>> read() throws Exception {
		String hostname = "oraposdms.unicomer.com";
		String username = "administrator";
		String password = "password";
		int port = 9999;
		
		List<Set<Entity>> result = new ArrayList<Set<Entity>>();
		HashMap<String, Entity> serverMap = new HashMap<String, Entity>();
		HashMap<String, Entity> applicationMap = new HashMap<String, Entity>();
		HashMap<String, Entity> componentMap = new HashMap<String, Entity>();
		
		try{
			ModelControllerClient client = createClient(InetAddress.getByName(hostname), port, username, password, "ManagementRealm");
			logger.info("Conexion establecida!");
			
            //Se obtienen generalidades del ambiente
			ModelNode op = new ModelNode();
			op = Operations.createOperation("read-resource");
			op.get("profile").set("default");
            op.get("include-runtime").set(true);
            op.get("include-defaults").set(true);
            ModelNode productInfo = client.execute(op);
            
            if(Operations.isSuccessfulOutcome(productInfo)){
            	
            	// Se obtienen todos las aplicaciones desplegadas en cada servidor
            	op = new ModelNode();
            	op = Operations.createOperation("read-children-resources");
                op.get(ClientConstants.CHILD_TYPE).set(ClientConstants.SERVER_GROUP);
                op.get("recursive").set(true);
                op.get("include-runtime").set(true);
                op.get("include-defaults").set(true);
                ModelNode serverGroupResult = client.execute(op);
                
                if(Operations.isSuccessfulOutcome(serverGroupResult)){
                	// Operations.readResult(result).require("summary")
        			ModelNode serverGroups = serverGroupResult.get("result");
        			Iterator<ModelNode> it =  serverGroups.asList().iterator();
        			while(it.hasNext()){
        				ModelNode serverNode = it.next();
        				List<Property> properties = serverNode.asPropertyList();
        				Property property = properties.get(0);
        				String serverGroupName = property.getName();
        				StringBuilder jvmDetails = new StringBuilder("");
        				logger.info("Server Group: " + serverGroupName);
        				//Obtener detalles de JVM
        				Iterator<Property> jvm = property.getValue().get("jvm").asPropertyList().iterator();
        				while(jvm.hasNext()){
        					jvmDetails.append(jvm.next().getValue().asString()).append(System.getProperty("line.separator"));
        				}
        				Entity serverEntity = createServerEntity(serverGroupName, jvmDetails, productInfo);
        				serverMap.put(serverGroupName, serverEntity);
        				
        				//Obtener detalle de despliegues
        				Iterator<Property> deployments = property.getValue().get("deployment").asPropertyList().iterator();
        				while(deployments.hasNext()){
        					ModelNode deployment = deployments.next().getValue();
        					if(deployment.get("enabled").asBoolean()){
        						String deploymentName = deployment.get("runtime-name").asString();
        						logger.info("    " + deploymentName);
        						Entity applicationEntity = createApplicationEntity(deploymentName, serverEntity);
                				serverMap.put(deploymentName, applicationEntity);
        					}
        				}
        				logger.info("");
        			}
        		}else{
        			logger.error("La consulta a JBoss ha fallado: " + serverGroupResult.get("result").asString());
        		}                
            }
			
            client.close();
		}catch(Exception e){
			System.err.println("Ha ocurrido un problema: " + e.getMessage());
			e.printStackTrace();
		}
		result.add(new HashSet<Entity>(serverMap.values()));
		result.add(new HashSet<Entity>(applicationMap.values()));
		result.add(new HashSet<Entity>(componentMap.values()));
		
		return result;
	}
	
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	private Entity createServerEntity (String serverName, StringBuilder jvmDetails, ModelNode productInfo){
		Entity entity = new UnicomerEntity();
		StringBuilder description = new StringBuilder("");
		String productVersion = Operations.readResult(productInfo).require("product-version").asString();
    	String releaseCodeName = Operations.readResult(productInfo).require("release-codename").asString();
    	String releaseVersion = Operations.readResult(productInfo).require("release-version").asString();
    	String productName = Operations.readResult(productInfo).require("product-name").asString();
		
    	description.append("Product Name: " + productName).append(System.getProperty("line.separator"));
    	description.append("Product Version: " + productVersion).append(System.getProperty("line.separator"));
    	description.append("Release Code Name: " + releaseCodeName).append(System.getProperty("line.separator"));
    	description.append("Release Version: " + releaseVersion).append(System.getProperty("line.separator"));
    	description.append(jvmDetails);
    	
		entity.setAssetType("Environment");
		entity.setName(serverName);
		entity.setVersion(releaseVersion);
		entity.setDescription(description.toString());
			
		return entity;
	}
	
	private Entity createApplicationEntity (String deploymentName, Entity server){
		Entity entity = new UnicomerEntity();
		String module = "";
		String name = "";
		String version = "";
		String extension = "";
		
		try{
			int i = deploymentName.lastIndexOf('.');
			if (i >= 0) {
			    extension = deploymentName.substring(i+1);
			}
		}catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception occured while parsing extension from deployment " + deploymentName + ". " + e.getMessage());
			extension = "";
		}
		
		try{
			version = "1.0.0";
			String fileName = deploymentName;
			// Remover la extension, si existe
			int extensionIndex = fileName.lastIndexOf('.');
			if (extensionIndex >= 0) {
				fileName = fileName.substring(0,extensionIndex);
			}
			
			// Remover el modulo y componente, si existe
			if (fileName.contains(".")){
			    String majorVersion = fileName.substring(0, fileName.indexOf("."));
			    String minorVersion = fileName.substring(fileName.indexOf("."));
			    int delimiter = majorVersion.lastIndexOf("-");
			    if (majorVersion.indexOf("_")>delimiter) 
			    	delimiter = majorVersion.indexOf("_");
			    majorVersion = majorVersion.substring(delimiter+1, fileName.indexOf("."));
			    version = majorVersion + minorVersion;
			}			
		}catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception occured while parsing version from deployment " + deploymentName + ". " + e.getMessage());
			version = "1.0.0";
		}
		
		try{
			name = deploymentName;
			name = name.replace(extension, "");
			name = name.replace(version, "");
			if(name.endsWith("-")){
				name = name.substring(0,name.length() - 1);
			}
			
			if (name.contains("-")){
				int hyphenIndex = name.indexOf('-');				
				if (hyphenIndex >= 0) {
					String part1 = name.substring(hyphenIndex+1);
					String part2 = name.substring(0,hyphenIndex);
					
					if(part1.length() > part2.length()){
						name = part1;
						module = part2;
					}else{
						module = part1;
						name = part2;
					}
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception occured while parsing name from deployment " + deploymentName + ". " + e.getMessage());
			module = deploymentName;
			name = deploymentName;
		}
		
		entity.setAssetType("Application");
		entity.setName(name);
		entity.setVersion(version);
		entity.setDescription("Aplicacion de " + module + " cargada con Harvester, de tipo " +  extension);
		
		
		entity.addRelationship(server, "deployment", false);
		
		return entity;
	}
	
	
	private ModelControllerClient createClient(final InetAddress host, final int port, final String username, final String password, final String securityRealmName) {
		final CallbackHandler callbackHandler = new CallbackHandler() {
			public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
				for (Callback current : callbacks) {
					if (current instanceof NameCallback) {
						NameCallback ncb = (NameCallback) current;
						logger.debug("JBoss username = " + new String(username));
						ncb.setName(new String(username));
					} else if (current instanceof PasswordCallback) {
						PasswordCallback pcb = (PasswordCallback) current;
						logger.debug("JBoss Password = " + password);
						pcb.setPassword(password.toCharArray());
					} else if (current instanceof RealmCallback) {
						RealmCallback rcb = (RealmCallback) current;
						logger.debug("JBoss Realm = " + rcb.getDefaultText());
						rcb.setText(securityRealmName);
					} else {
						throw new UnsupportedCallbackException(current);
					}
				}
			}
		};
		return ModelControllerClient.Factory.create(host, port, callbackHandler);
	}
	
}
