package com.unicomer.oer.harvester.reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.comparator.NameFileComparator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.oracle.artifact.ArtifactAlgorithm;
import com.oracle.oer.integration.harvester.RemoteQuery;
import com.oracle.oer.sync.framework.MetadataIntrospectionConfig;
import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.oracle.oer.sync.framework.MetadataReader;
import com.oracle.oer.sync.model.Entity;
import com.oracle.oer.sync.model.ManifestEntry;
import com.unicomer.oer.harvester.model.UAttribute;
import com.unicomer.oer.harvester.model.URelationship;
import com.unicomer.oer.harvester.model.UnicomerEntity;
import com.unicomer.oer.harvester.model.UnicomerEntityYaml;

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
			fileLocation = System.getProperty("user.dir");
		}
	}
	
	public List<Set<Entity>> read() throws Exception {
		List<Set<Entity>> list = new ArrayList<Set<Entity>>();
		Set<Entity> entitySet = new HashSet<Entity>();
		int count = 0;
		try{
			File folder = new File(fileLocation);
			File[] fileArray = folder.listFiles();
			
			Arrays.sort(fileArray, NameFileComparator.NAME_COMPARATOR);
			
			for(File file : fileArray){
				if (file.isFile() && getExtension(file.getName()).equals(".yml") ) {
					ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
					Set<UnicomerEntityYaml> harvestedEntities = mapper.readValue(file, new TypeReference <Set<UnicomerEntityYaml>>(){});
					entitySet = new HashSet<Entity>();
					for(UnicomerEntityYaml harvestedEntity : harvestedEntities){
	        			Entity convertedEntity = fileToInterfaceConverter(harvestedEntity);
	        			logger.info("Converted entity: " + convertedEntity.getName() + " (" + convertedEntity.getVersion()+ ")");
	        			entitySet.add(convertedEntity);
	            	}
					
					logger.info(entitySet.size() + " assets read from " + file.getName());
					count = count + entitySet.size();
					list.add(entitySet);
				}
			}
		}catch (Exception e) {
			logger.error("Exception occured while Harvesting: " + e.getMessage());
			throw e;
		}
		
		logger.info(count + " assets read from files in " + fileLocation);
		return list;
	}
	
	private String getExtension(String fileName){
		String extension = "";
		try{
			int i = fileName.lastIndexOf('.');
			if (i >= 0) {
				extension = fileName.substring(i);
			}
		}catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception occured while parsing extension from file " + fileName + ". " + e.getMessage());
			extension = "";
		}		
		return extension;
	}
	
	private Entity fileToInterfaceConverter(UnicomerEntityYaml fileEntity) throws Exception{
		Entity result = null;
		try{
			
			result = new UnicomerEntity(fileEntity.getAssetType(), fileEntity.getId(), fileEntity.getName(), fileEntity.getDescription(), fileEntity.getVersion(), ArtifactAlgorithm.DEFAULT);
			result.setNamespace(fileEntity.getNamespace());
			result.setKeywords(fileEntity.getKeywords());
			result.setRelationshipsProcessed(fileEntity.getRelationshipsProcessed());
			result.setSummaryXML(fileEntity.getSummaryXML());
			result.setWsdlSummary(fileEntity.getWsdlSummary());
			
			for(UAttribute attibute : fileEntity.getCategorizations()){
				result.addCategorization(attibute.getName(), attibute.getValue());
			}
			
			fileEntity.getCustomData().forEach(result :: addCategorization);
			
			for(UAttribute attibute : fileEntity.getHarvesterProperties()){
				result.addHarvesterProperty(attibute.getName(), attibute.getValue());
			}
			
			for(ManifestEntry manifestEntry : fileEntity.getManifestEntries()){
				result.addManifestEntry(manifestEntry);
			}
			
			for(URelationship relationship : fileEntity.getRelationships()){
				result.addRelationship(fileToInterfaceConverter(relationship.getRelatedTo()), relationship.getName(), relationship.isUseAsSourceEntity());
			}
		}catch(Exception e){
			logger.error("fileToInterfaceConverter: " + e.getMessage());
			throw e;
		}
		
		return result;
	}
	
	public void close() throws IOException {
		
	}
}
