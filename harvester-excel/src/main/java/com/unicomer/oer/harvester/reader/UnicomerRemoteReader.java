/**
 * 
 */
package com.unicomer.oer.harvester.reader;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.oracle.oer.integration.harvester.RemoteQuery;
import com.oracle.oer.sync.framework.MetadataIntrospectionConfig;
import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.oracle.oer.sync.framework.MetadataReader;
import com.oracle.oer.sync.model.Entity;

/**
 * @author carlosj_rodriguez
 *
 */
public class UnicomerRemoteReader implements MetadataReader {
	private static MetadataLogger logger = MetadataManager.getLogger(UnicomerRemoteReader.class);
	private MetadataIntrospectionConfig config = null;
	private String harvestType = "";
	
	public UnicomerRemoteReader(){
		try{
			MetadataManager metadataManager = MetadataManager.getInstance();
		    this.config = metadataManager.getConfigManager();
		    RemoteQuery remote = this.config.getRemoteQuery();
		    
		    harvestType = remote.getServerType();
		}catch(Exception e){
			logger.error("Failure staring UnicomerRemoteReader... " + e.getMessage());
			harvestType = "File";
		}
	}
	
	public List<Set<Entity>> read() throws Exception {
		MetadataReader actualReader = null;
		if(harvestType.equals("File")){
			actualReader = new YamlRemoteReader();
		}else if(harvestType.equals("JBoss")){
			actualReader = new JBossRemoteReader();
		}else if(harvestType.equals("SOASuite")){
			actualReader = new SoaSuiteRemoteReader();
		}else if(harvestType.equals("WLS")){
			actualReader = new WebLogicRemoteReader();
		}else if(harvestType.equals("WebSphere")){
			actualReader = new WebSphereRemoteReader();
		}else {
			logger.error("An implementation class for " + harvestType + " server type is not available...");
		}
		
		return actualReader.read();
	}

	public void close() throws IOException {
		
	}

}
