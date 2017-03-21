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
import com.unicomer.oer.harvester.util.PropertiesLoader;

public class YamlWriter {
	private static MetadataLogger logger = MetadataManager.getLogger(YamlWriter.class);
	private static PropertiesLoader prop = PropertiesLoader.getInstance();
	private static String fileLocation = "";
	
	public YamlWriter(){
		
	}
	
	public static void writeToYaml(Set<Entity> harvestedEntities, String serverType) throws Exception{
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {
			fileLocation = prop.getProperty("file.harvest-summary.location");
        	fileLocation = fileLocation + "Harvested-" + serverType + "-"
        			+ new SimpleDateFormat("yyyyMMddhhmmss").format(new Date())
        			+ ".yml";
        	
        	mapper.writeValue(new FileWriter(fileLocation, true), harvestedEntities);        	
        	logger.info(harvestedEntities.size() + " entities written in " + fileLocation + " file");
        } catch (Exception e) {
            logger.warn("Error when writing Harvested Assets in YAML file... " + e.getMessage());
        }finally{
        	// Sleeps for 1 second to be sure that the filename will not be repeated at all
        	Thread.sleep(1000);
        }
	}	
}
