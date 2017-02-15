package com.unicomer.oer.harvester.excel;

import java.util.Iterator;
import java.util.Set;

import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.oracle.oer.sync.model.Entity;
import com.unicomer.oer.harvester.reader.JBossRemoteReader;

public class TestJBoss {
	public static void main(String[] args) throws Exception {
		MetadataLogger logger = MetadataManager.getLogger(JBossRemoteReader.class);
		JBossRemoteReader reader = new JBossRemoteReader();
		
		Iterator<Set<Entity>> it= reader.read().iterator();
		while (it.hasNext()){
			logger.info("Activos obtenidos:");
			Set<Entity> entities = it.next();
			for(Entity entity : entities){
				logger.info("Entity name: " + entity.getDescription());
			}
		}
	}
}
