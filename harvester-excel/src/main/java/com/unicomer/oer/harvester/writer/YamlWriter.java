package com.unicomer.oer.harvester.writer;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
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
	
	public static void writeToYaml(Set<Entity> harvestedEntities) throws Exception{
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		int count = 0;
		try {
        	String fileLocation = "HarvestedAssets-"
        			+ new SimpleDateFormat("yyyyMMddhhmmss").format(new Date())
        			+ ".yml";
        	
			for (Entity entity : harvestedEntities) {
				mapper.writeValue(new FileWriter(fileLocation, true), entity);
				count++;
			}
        	
        	logger.info(count + " entities written in " + fileLocation + " file");
        } catch (Exception e) {
            logger.warn("Error when writing Harvested Assets in YAML file... " + e.getMessage());
        }finally{
        	// Sleeps for 1 second to be sure that the filename will not be repeated at all
        	Thread.sleep(1000);
        }
	}
	
}
