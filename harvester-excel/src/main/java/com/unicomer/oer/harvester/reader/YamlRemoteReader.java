package com.unicomer.oer.harvester.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.constructor.SafeConstructor;
import com.oracle.oer.integration.harvester.RemoteQuery;
import com.oracle.oer.sync.framework.MetadataIntrospectionConfig;
import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.oracle.oer.sync.framework.MetadataReader;
import com.oracle.oer.sync.model.Entity;
import com.unicomer.oer.harvester.model.UnicomerEntity;

public class YamlRemoteReader implements MetadataReader {
	private static MetadataLogger logger = MetadataManager.getLogger(YamlRemoteReader.class);
	private MetadataIntrospectionConfig config = null;
	private String fileLocation = "";
	
	public YamlRemoteReader(){
		try {
			MetadataManager metadataManager = MetadataManager.getInstance();
			this.config = metadataManager.getConfigManager();
			RemoteQuery remote = this.config.getRemoteQuery();
			
			fileLocation = remote.getUri();
		} catch (Exception e) {
			logger.error("Failure staring YamlRemoteReader... " + e.getMessage());
			fileLocation = "Asset.yml";
		}
	}
	
	public List<Set<Entity>> read() throws Exception {
		List<Set<Entity>> list = new ArrayList<Set<Entity>>();
		Set<Entity> entitySet = new HashSet<Entity>();
		try (InputStream input = new FileInputStream(fileLocation)) {
		    Yaml yaml = new Yaml(new SafeConstructor());
		    Iterator<Object> it = yaml.loadAll(input).iterator();
		    while(it.hasNext()){
		    	entitySet.add((UnicomerEntity) it.next());
		    }
		} catch (Throwable e) {
		    System.out.println("ERROR: " + e.getMessage());
		    throw e;
		}
		
		logger.info(entitySet.size() + " assets read from " + fileLocation);
		list.add(entitySet);
		return list;
	}
	
	public void close() throws IOException {
		
	}
}
