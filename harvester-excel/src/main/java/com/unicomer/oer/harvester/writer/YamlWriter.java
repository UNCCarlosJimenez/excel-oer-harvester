package com.unicomer.oer.harvester.writer;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.oracle.oer.sync.model.Entity;

public class YamlWriter {
	private static MetadataLogger logger = MetadataManager.getLogger(YamlWriter.class);
	
	public YamlWriter(){
		
	}
	
	public static void writeToYaml(List<Set<Entity>> harvestedEntities){
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		int count = 0;
		try {
        	String fileLocation = "HarvestedAssets-"
        			+ new SimpleDateFormat("yyyyMMddhhmmss").format(new Date())
        			+ ".yml";
        	
        	for(Set<Entity> setEntities : harvestedEntities){
        		for(Entity entity : setEntities){
        			mapper.writeValue(new FileWriter(fileLocation, true), entity);
        			count++;
            	}
        	}
        	logger.info(count + " entities written in " + fileLocation + " file");
        } catch (Exception e) {
            logger.warn("Error when writing Harvested Assets in YAML file... " + e.getMessage());
        }
	}
	
}
