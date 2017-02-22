/**
 * 
 */
package com.unicomer.oer.harvester.util;

import java.io.FileReader;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * @author carlosj_rodriguez
 *
 */
public class Configuration {
		
	public static Configuration load(String fileName) throws Exception{
		Yaml yaml = new Yaml(new SafeConstructor());		
		return yaml.loadAs( new FileReader("UnicomerHarvesterSettings.yml"), Configuration.class );
	}
	
	public Configuration(){
		
	}
	
	
	
}
