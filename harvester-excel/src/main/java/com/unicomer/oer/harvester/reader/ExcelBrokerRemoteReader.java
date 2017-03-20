/**
 * 
 */
package com.unicomer.oer.harvester.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.oracle.artifact.ArtifactAlgorithm;
import com.oracle.oer.integration.harvester.RemoteQuery;
import com.oracle.oer.sync.framework.MetadataIntrospectionConfig;
import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.oracle.oer.sync.framework.MetadataReader;
import com.oracle.oer.sync.model.Entity;
import com.unicomer.oer.harvester.model.UnicomerEntity;
import com.unicomer.oer.harvester.util.PropertiesLoader;
import com.unicomer.oer.harvester.writer.YamlWriter;

/**
 * @author carlosj_rodriguez
 *
 */
public class ExcelBrokerRemoteReader implements MetadataReader {
	private static MetadataLogger logger = MetadataManager.getLogger(ExcelBrokerRemoteReader.class);
	private MetadataIntrospectionConfig config = null;
	private PropertiesLoader prop = PropertiesLoader.getInstance();
	private String templateFile = "";
	
	public ExcelBrokerRemoteReader(){
		try{
			MetadataManager metadataManager = MetadataManager.getInstance();
		    this.config = metadataManager.getConfigManager();
		    RemoteQuery remote = this.config.getRemoteQuery();
		    
		    templateFile = remote.getUri();
		}catch(Exception e){
			logger.error("Failure staring SoaSuiteRemoteReader... " + e.getMessage());
			
			templateFile = prop.getProperty("broker.template.file");
		}
	}
	
	
	public List<Set<Entity>> read() throws Exception {
		List<Set<Entity>> list = new ArrayList<Set<Entity>>();
		HashMap<String, Entity> servicesMap = new HashMap<String, Entity>();
				
		String defEnvironment = prop.getProperty("default.environment");
		String defaultAssetType = prop.getProperty("broker.service.asset-type");
		String resourceEnvironmentXPath = prop.getProperty("broker.custom-data.environment");
		String resourceNameXPath = prop.getProperty("broker.custom-data.endpoint");
//		String defServiceToAppRelation = prop.getProperty("broker.service-to-app-relation");
		String harvestType = prop.getProperty("broker.harvest-type");
		String transportProtocolXpath = prop.getProperty("broker.custom-data.transport-protocol");
		String authenticationMethod = prop.getProperty("broker.custom-data.authentication");
		String authorizationMethod = prop.getProperty("broker.custom-data.authentication");
		boolean hasHeader = Boolean.valueOf(prop.getProperty("broker.has-header"));
		
		try {
			FileInputStream file = new FileInputStream(new File(templateFile));
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			XSSFSheet sheet = workbook.getSheetAt(0);
			
			Iterator<Row> rowIterator = sheet.iterator();
			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				if(hasHeader){
					hasHeader = false;
				}else{
					Iterator<Cell> cellIterator = row.cellIterator();
					String name = cellIterator.next().getStringCellValue();
					String description = cellIterator.next().getStringCellValue();
					String originProtocol = cellIterator.next().getStringCellValue();
					String resourceName = cellIterator.next().getStringCellValue();
//					String location = cellIterator.next().getStringCellValue();
//					String originSystem = cellIterator.next().getStringCellValue();
//					String targetProtocol = cellIterator.next().getStringCellValue();
//					String targetSystem = cellIterator.next().getStringCellValue();
					String version = getVersion(name);
					logger.info("Asset: " + name + " - " + version);					
					
					UnicomerEntity entity = new UnicomerEntity(defaultAssetType, name, name, description, version, ArtifactAlgorithm.DEFAULT);
					entity.addCategorization("LineOfBusiness", prop.getProperty("broker.line-of-business"));
					entity.addCategorization("AssetLifecycleStage", prop.getProperty("default.asset-lifecycle-stage"));
					entity.addCategorization("Technology", prop.getProperty("broker.technology"));
					entity.addCategorization("Region", prop.getProperty("default.region"));
					entity.addHarvesterProperty("Modulo", harvestType);
					entity.addHarvesterProperty("Harvester Description", prop.getProperty("default.harvester-description"));
					entity.addCustomData("acquisition-method", prop.getProperty("default.acquisition-method"));
					entity.addCustomData(resourceEnvironmentXPath, defEnvironment);
					entity.addCustomData(resourceNameXPath, resourceName);
					entity.addCustomData(transportProtocolXpath, originProtocol);
					entity.addCustomData("authentication-method", authenticationMethod);
					entity.addCustomData("authorization-method", authorizationMethod);
					entity.addCustomData("has-test-scripts", "true");
					entity.addCustomData("needs-performance-testing", "false");
					entity.addCustomData("has-automated-testing", "false");
					entity.addCustomData("consistent-with-business-mission", "true");
					entity.addCustomData("passes-legal-review", "true");
					entity.addCustomData("approved-for-internal-use", "true");
					entity.addCustomData("approved-for-external-use", "false");
					entity.addCustomData("passes-technical-review", "true");
					entity.addCustomData("downtime-impact", prop.getProperty("broker.downtime-impact"));
					entity.addCustomData("license-terms", prop.getProperty("broker.license-terms"));
					
					servicesMap.put(name,  entity);
				}
			}
			workbook.close();
			file.close();
			
			list.add(new HashSet<Entity>(servicesMap.values()));
			
			for(Set<Entity> entitySet : list){
				if(entitySet != null && entitySet.size() > 0){
					YamlWriter.writeToYaml(entitySet, harvestType);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return list;
	}
	
	private String getVersion(String name){
		String version = prop.getProperty("default.version");
		try{
			Integer versionNumber = Integer.valueOf(name.substring(name.length() -2, name.length()));
			version = version.replace("1", versionNumber.toString());
		}catch(Exception ex){
			logger.error("Error while trying to get version from service name in " + name + ". " + ex.getMessage(), ex);
		}
		
		return version;
	}
	
	public void close() throws IOException {
		
	}
	
	public static void main(String[] args) {
		
	}
}
