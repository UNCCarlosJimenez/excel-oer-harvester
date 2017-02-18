package com.unicomer.oer.harvester.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.oracle.oer.sync.model.Entity;
import com.unicomer.oer.harvester.model.UnicomerEntityYaml;

public class YamlTest {
	public static void main(String[] args) {
		MetadataLogger logger = MetadataManager.getLogger(YamlTest.class);
		String fileLocation = "C:\\Users\\carlosj_rodriguez\\work\\temp\\harvester\\HarvestedAssets-20170217053333.yml";
		
		try (InputStream input = new FileInputStream(fileLocation)) {
			logger.info("Start!");
			Set<Entity> entitySet = new HashSet<Entity>();
			
//			Yaml yaml = new Yaml(new SafeConstructor());
//			yaml.loadAll(input).forEach( System.out::println );
//			
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			UnicomerEntityYaml user = mapper.readValue(new File(fileLocation), UnicomerEntityYaml.class);
            System.out.println(ReflectionToStringBuilder.toString(user,ToStringStyle.MULTI_LINE_STYLE));
			
		} catch (Throwable e) {
			logger.error("ERROR: " + e.getMessage());
		}
	}
}
