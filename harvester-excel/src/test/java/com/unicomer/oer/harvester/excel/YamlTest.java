package com.unicomer.oer.harvester.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.TypeDescription;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.constructor.Constructor;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.constructor.SafeConstructor;
import com.oracle.artifact.ArtifactAlgorithm;
import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.oracle.oer.sync.model.Entity;
import com.oracle.oer.sync.model.ManifestEntry;
import com.unicomer.oer.harvester.model.UAttribute;
import com.unicomer.oer.harvester.model.URelationship;
import com.unicomer.oer.harvester.model.UnicomerEntity;
import com.unicomer.oer.harvester.model.UnicomerEntityYaml;

public class YamlTest {
	static MetadataLogger logger = MetadataManager.getLogger(YamlTest.class);
	
	public static void main(String[] args) {
		try{
			UnicomerEntity entityOne = new UnicomerEntity("Application", "entity-one", "entity-one", "First entity", "1.0.0", ArtifactAlgorithm.DEFAULT);
			UnicomerEntity entityTwo = new UnicomerEntity("Application", "entity-two", "entity-two", "second entity", "1.0.0", ArtifactAlgorithm.DEFAULT);
			entityTwo.addRelationship(entityOne, "deployment", false);
			
			Set<Entity> setEntity = new HashSet<Entity>();
			setEntity.add(entityOne);
			setEntity.add(entityTwo);
			setEntity.add(entityOne);
			
			List<Set<Entity>> listEntity = new ArrayList<Set<Entity>>();
			listEntity.add(setEntity);
			
			
			UnicomerEntity entityThree = new UnicomerEntity("Application", "entity-three", "entity-three", "Third entity", "1.0.0", ArtifactAlgorithm.DEFAULT);
			UnicomerEntity entityFour = new UnicomerEntity("Application", "entity-four", "entity-four", "Fourth entity", "1.0.0", ArtifactAlgorithm.DEFAULT);
			entityThree.addRelationship(entityOne, "deployment", false);
			entityThree.addRelationship(entityTwo, "deployment", false);
			entityThree.addRelationship(entityFour, "deployment", false);
			
			setEntity = new HashSet<Entity>();
			setEntity.add(entityThree);
			setEntity.add(entityFour);
			listEntity.add(setEntity);
			
			for(Set<Entity> setEntities : listEntity){
				writeToYaml(setEntities);
			}
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		List<Set<Entity>> entitiesToHarvest = new ArrayList<Set<Entity>>();
		try{
//			String fileLocation = "C:\\Users\\carlosj_rodriguez\\work\\temp\\harvester\\";
        	String fileLocation = "C:\\Users\\carlosj_rodriguez\\Drive\\unicomer-docs\\architecture\\04 - Service repository OER\\sample\\";
			File folder = new File(fileLocation);
			File[] fileArray = folder.listFiles();
			
			for(File file : fileArray){
				if (file.isFile()) {
					String fileName = file.getName();
					
//					Yaml yaml = new Yaml(new SafeConstructor());
//					UnicomerEntityYaml harvestedEntities = yaml.loadAs(new FileReader(file), UnicomerEntityYaml.class);
//					
					ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
					Set<UnicomerEntityYaml> harvestedEntities2 = mapper.readValue(file, new TypeReference <Set<UnicomerEntityYaml>>(){});
		            
					logger.info("Done?");
					Set<Entity> setEntities = new HashSet<Entity>();
//					for(Set<UnicomerEntityYaml> setEntities : harvestedEntities2){
		        		for(UnicomerEntityYaml harvestedEntity : harvestedEntities2){
		        			Entity oerEntity = fileToInterfaceConverter(harvestedEntity);
		        			logger.info("name: " + oerEntity.getName());
		        			logger.info("children: " + oerEntity.getChildEntities("Application"));
		        			
		        			setEntities.add(oerEntity);
		            	}
//		        	}
					
					entitiesToHarvest.add(setEntities);
				}			
			}
		}catch (Exception e) {
			logger.error("ERROR: " + e.getMessage());
		}
		
		
		
		if(logger==null){

			try (InputStream input = new FileInputStream("fileLocation")) {
				logger.info("Start!");
				Set<Entity> entitySet = new HashSet<Entity>();
				
				Yaml yaml = new Yaml(new SafeConstructor());
				
				
				
				
//				
				ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
				yaml.loadAll(input).forEach( YamlTest :: populateList );
//				
//				
			} catch (Throwable e) {
				logger.error("ERROR: " + e.getMessage());
			}
		}
	}
	
	public static void writeAssetsYml(){
		
	}
	
	
	
	public static void writeToYaml(Set<Entity> harvestedEntities){
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		int count = 0;
		try {
        	String fileLocation = "C:\\Users\\carlosj_rodriguez\\Drive\\unicomer-docs\\architecture\\04 - Service repository OER\\sample\\" + 
        			"HarvestedAssets-"
        			+ new SimpleDateFormat("yyyyMMddhhmmss").format(new Date())
        			+ ".yml";
        	
        	mapper.writeValue(new FileWriter(fileLocation, true), harvestedEntities);
//        	for(Set<Entity> setEntities : harvestedEntities){
        		for(Entity entity : harvestedEntities){
        			count++;
            	}
//        	}
        	logger.info(count + " entities written in " + fileLocation + " file");
        } catch (Exception e) {
            logger.warn("Error when writing Harvested Assets in YAML file... " + e.getMessage());
        }
	}
	
	
	public static Entity fileToInterfaceConverter(UnicomerEntityYaml fileEntity) throws Exception{
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
	
	private static void populateList (Object entity){
		try {
//			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
//			UnicomerEntityYaml user = mapper.readValue(entity.toString(), UnicomerEntityYaml.class);
//			System.out.println("name="+user.getName());
			
			something(entity.toString());
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private static void something(String objectToString){
		Constructor constructor = new Constructor(UnicomerEntityYaml.class);
		TypeDescription entityDescription = new TypeDescription(UnicomerEntityYaml.class);
		entityDescription.putListPropertyType("harvesterproperties", UAttribute.class);
		entityDescription.putListPropertyType("categorizations", UAttribute.class);
		constructor.addTypeDescription(entityDescription);
		
		Yaml yaml = new Yaml(constructor);
		UnicomerEntityYaml entity2 = yaml.loadAs(objectToString, UnicomerEntityYaml.class);
		System.out.println("name="+entity2.getName());
//		
//		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
//		List<UnicomerEntityYaml> user = mapper.readValue(new File(fileLocation), new TypeReference<List<UnicomerEntityYaml>>() { });
//		System.out.println("name="+user.get(0).getName());
//		System.out.println(ReflectionToStringBuilder.toString(user,ToStringStyle.MULTI_LINE_STYLE));
        
		
		
		
		
		
	}
}
