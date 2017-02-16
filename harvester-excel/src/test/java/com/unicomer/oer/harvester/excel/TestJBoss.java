package com.unicomer.oer.harvester.excel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

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

public class TestJBoss {
	private static MetadataLogger logger = MetadataManager.getLogger(TestJBoss.class);
	
	public static void main(String[] args) throws Exception {		
//		JBossRemoteReader reader = new JBossRemoteReader();
		init();
		
//		System.out.println(getVersion("ejb-securityclient-1.4.0-snapshot.jar"));
//		System.out.println(getVersion("ejb-adminhothclient-1.4.0-snapshot.jar"));
		
		// Iterator<Set<Entity>> it= reader.read().iterator();
		// while (it.hasNext()){
		// logger.info("Activos obtenidos:");
		// Set<Entity> entities = it.next();
		// for(Entity entity : entities){
		// logger.info("Entity name: " + entity.getDescription());
		// }
		// }
	}
	
	private static String getVersion(String deploymentName){
		String module = "";
		String name = "";
		String version = "";
		String extension = "";
		
		try{
			int i = deploymentName.lastIndexOf('.');
			if (i >= 0) {
			    extension = deploymentName.substring(i);
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
						module = part1;
					}else{
						module = part2;
					}
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception occured while parsing name from deployment " + deploymentName + ". " + e.getMessage());
			module = deploymentName;
			name = deploymentName;
		}
		
		return "[Name=" + name + ", Module=" + module + ", Version=" + version + ", Extension=" + extension + "]";
	}
	
	private static void init2() throws Exception {
		String host = "oraposdms.unicomer.com";
		int port = 9999;
		String username = "administrator";
		String password = "admin";
		ModelControllerClient client = createClient(InetAddress.getByName(host), port, username, password.toCharArray(), "ManagementRealm");
		logger.info("Conexion establecida!");
		try {
			/*
			 curl --digest -D - http://admin:admin123@localhost:9990/management/ -d '
			 
			 {"operation":"read-resource", "include-rutime":"true", "address":
			 	[{"core-service":"platform-mbean"},{"type":"runtime"}], 
			 
			 "json.pretty":1}' -HContent-Type:application/json

			 */
			
			ModelNode op = new ModelNode();
			op = Operations.createOperation("read-resource");
			op.get("profile").set("default");
            op.get("include-runtime").set(true);
            op.get("include-defaults").set(true);
            logger.info(op.asString());
            ModelNode serverGroupResult = client.execute(op);
            logger.info(serverGroupResult.toJSONString(false));
            
            
			client.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void init() throws Exception {
		URI uri = new URI("http://oraposdms.unicomer.com:9999");
		int port = uri.getPort();
		String username = "administrator";
		String password = "admin";
		ModelControllerClient client = createClient(InetAddress.getByName(uri.getHost()), port, username, password.toCharArray(), "ManagementRealm");
		logger.info("Conexion establecida!");
		try {
			ModelNode op = new ModelNode();
			op = Operations.createOperation("read-children-resources");
            op.get(ClientConstants.CHILD_TYPE).set(ClientConstants.SERVER_GROUP);
            op.get("recursive").set(true);
//            op.get("include-runtime").set(true);
//            op.get("include-defaults").set(true);
            ModelNode serverGroupResult = client.execute(op);
            printModelNode(serverGroupResult);
            logger.info(serverGroupResult.toJSONString(false));
             
            op = Operations.createOperation("read-resource-description");
            op.get(ClientConstants.CHILD_TYPE).set(ClientConstants.DEPLOYMENT);
            op.get("recursive").set(true);
//            ModelNode subsystemList = client.execute(op);
//            logger.info("");
//            logger.info("**************subsystemList**************");
//            logger.info(subsystemList.toJSONString(false));
            
			
            op = Operations.createOperation("read-resource");
            op.get(ClientConstants.OP_ADDR).add("subsystem", "datasources");
            op.get("recursive").set(true);
//            op.get("include-runtime").set(true);
//            op.get("include-defaults").set(true);
//            ModelNode datasourcesResult = client.execute(op);
//            logger.info("");
//            logger.info("**************Datasources**************");
//            logger.info(datasourcesResult.toJSONString(false));
            
			client.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	private static void printModelNode(ModelNode modelNode){
		//Obtener server groups
		
		if(modelNode.get("outcome").asString().equals(ClientConstants.SUCCESS)){
			ModelNode result = modelNode.get("result");
			Iterator<ModelNode> it =  result.asList().iterator();
			while(it.hasNext()){
				ModelNode serverNode = it.next();
				List<Property> properties = serverNode.asPropertyList();
				Property property = properties.get(0);
				logger.info(property.getName());
				
				Iterator<Property> deployments = property.getValue().get("deployment").asPropertyList().iterator();
				while(deployments.hasNext()){
					ModelNode deployment = deployments.next().getValue();
					if(deployment.get("enabled").asBoolean()){
						String deploymentName = deployment.get("runtime-name").asString();
//						logger.info("    " + deploymentName);
						logger.info("    " + getVersion(deploymentName));
					}
				}
				logger.info("");
								
//				//Obtener despliegues
//				ModelNode desplieguesList = serverNode.get("dms-server-group");
//				Iterator<ModelNode> despliegues = desplieguesList.asList().iterator();
//				while(despliegues.hasNext()){
//					ModelNode despliegue = despliegues.next();
//					logger.info("    "+despliegue.get("deployment").asString());
//				}
			}
		}
	}

	static ModelControllerClient createClient(final InetAddress host, final int port, final String username,
			final char[] password, final String securityRealmName) {
		final CallbackHandler callbackHandler = new CallbackHandler() {
			public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
				for (Callback current : callbacks) {
					if (current instanceof NameCallback) {
						NameCallback ncb = (NameCallback) current;
						logger.debug("JBoss username = " + new String(username));
						ncb.setName(new String(username));
					} else if (current instanceof PasswordCallback) {
						PasswordCallback pcb = (PasswordCallback) current;
						logger.debug("JBoss Password = " + new String(password));
						pcb.setPassword(password);
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
