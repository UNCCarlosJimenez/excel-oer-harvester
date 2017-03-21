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
import com.unicomer.oer.harvester.util.PropertiesLoader;

/**
 * @author carlosj_rodriguez
 *
 */
public class UnicomerRemoteReader implements MetadataReader {
	private static MetadataLogger logger = MetadataManager.getLogger(UnicomerRemoteReader.class);
	private MetadataIntrospectionConfig config = null;
	
	PropertiesLoader prop = PropertiesLoader.getInstance();
	private String jBossHarvestType = prop.getProperty("jboss.harvest-type");
	private String soaSuiteHarvestType = prop.getProperty("soasuite.harvest-type");
	private String weblogicHarvestType = prop.getProperty("weblogic.harvest-type");
	private String fileHarvestType = prop.getProperty("file.harvest-type");
	private String websphereHarvestType = prop.getProperty("websphere.harvest-type");
	private String brokerHarvestType = prop.getProperty("broker.harvest-type");
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
		if(harvestType.equals(fileHarvestType)){
			actualReader = new YamlRemoteReader();
		}else if(harvestType.equals(jBossHarvestType)){
			actualReader = new JBossRemoteReader();
		}else if(harvestType.equals(soaSuiteHarvestType)){
			actualReader = new SoaSuiteRemoteReader();
		}else if(harvestType.equals(weblogicHarvestType)){
			actualReader = new WebLogicRemoteReader();
		}else if(harvestType.equals(websphereHarvestType)){
			actualReader = new WebSphereRemoteReader();
		}else if(harvestType.equals(brokerHarvestType)){
			actualReader = new ExcelBrokerRemoteReader();
		}else {
			logger.error("An implementation class for " + harvestType + " server type is not available...");
		}
		
		return actualReader.read();
	}

	public void close() throws IOException {
		
	}

}
