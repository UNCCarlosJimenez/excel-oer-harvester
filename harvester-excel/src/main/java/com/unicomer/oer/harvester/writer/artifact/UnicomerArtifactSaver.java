/**
 * 
 */
package com.unicomer.oer.harvester.writer.artifact;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.flashline.registry.openapi.entity.Asset;
import com.flashline.registry.openapi.entity.MetadataEntrySummary;
import com.flashline.util.StringUtils;
import com.oracle.artifact.ArtifactAlgorithm;
import com.oracle.oer.sync.framework.MetadataIntrospectionException;
import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.unicomer.oer.harvester.model.UnicomerEntity;
import com.unicomer.oer.harvester.writer.UnicomerOERWriter;

/**
 * @author carlosj_rodriguez
 *
 */
public class UnicomerArtifactSaver {
	private MetadataLogger logger = MetadataManager.getLogger(UnicomerArtifactSaver.class);
	UnicomerOERWriter oerWriter;

	public UnicomerArtifactSaver(UnicomerOERWriter writer) {
		this.oerWriter = writer;
	}
	
	public List<Asset> findOrCreateArtifact(long assetTypeID, UnicomerEntity entity, ArtifactAlgorithm algorithm, Date publishedDate, UnicomerOERWriter metadataWriter) 
			throws MetadataIntrospectionException {
		try {
			String name = entity.getName();
			String description = entity.getDescription();
			String version = entity.getVersion();
			
			List<Asset> results = new ArrayList<Asset>();

			Asset[] matches = null;
			
			matches = oerWriter.assetQueryByName(name);
			
			if (matches!=null && matches.length > 1) {
				logger.warn("Found more than one artifact matching " + name);
			}
			if (matches!=null && matches.length > 0) {
				//Asset encontrado, se actualiza
				logger.debug(name + " fue encontrado (" + matches.length + "), se actualiza");
				for (Asset match : matches) {
					deleteArtifactMetadataEntries(match);
					
//					String artifactName = getArtifactName(tr.getDiscriminator(), name);
//					setArtifactInformation(artifactName, relativeURI, downloadURI, transformedContent, sfid, match);
					
					String matchDescription = match.getDescription() == null ? "" : match.getDescription();
					if ((description != null) && (!matchDescription.equals(description))
							&& (matchDescription.indexOf(description) < 0)) {
						match.setDescription(matchDescription + "\n\n" + description);
					}
					
					oerWriter.setCategorizations(entity, match);
					
					if (entity.getKeywords() != null) {
						match.setKeywords(entity.getKeywords());
					}
					match = oerWriter.assetUpdate(match);
					
					logger.info("********************************oerWriter.assetUpdate********************************");
					logger.info("name: "+match.getName());
					logger.info("description: "+match.getDescription());
					logger.info("version: "+match.getVersion());
					logger.info("************************************************************************************");
					
					match = oerWriter.saveCustomAttributes(entity, match);
					logger.info("********************************oerWriter.saveCustomAttributes********************************");
					logger.info("name: "+match.getName());
					logger.info("description: "+match.getDescription());
					logger.info("version: "+match.getVersion());
					logger.info("************************************************************************************");
					
					oerWriter.saveAttributes(match, entity, publishedDate);
					oerWriter.applyCategorizations(match, false);
					oerWriter.registerAsset(match, true);
					oerWriter.setProducingProjects(match);
					
					results.add(match);
				}

			} else {
				logger.debug(name + " no fue encontrado, se crea");
				//Asset no encontrado, se crea
				if (StringUtils.isEmpty(version)) {
					version = "1.0";
				}
				
				Asset artifactAsset = oerWriter.assetCreate(name, version, assetTypeID, entity, publishedDate);
				logger.info("********************************oerWriter.assetCreate********************************");
				logger.info("name: "+artifactAsset.getName());
				logger.info("description: "+artifactAsset.getDescription());
				logger.info("version: "+artifactAsset.getVersion());
				
				artifactAsset.setDescription(description);
				oerWriter.setCategorizations(entity, artifactAsset);
				logger.info("Categorizations Done!");
				
				if (entity.getKeywords() != null) {
					artifactAsset.setKeywords(entity.getKeywords());
				}				
				artifactAsset = oerWriter.assetUpdate(artifactAsset);
				logger.info("Keywords Done!");
				
				artifactAsset = oerWriter.saveCustomAttributes(entity, artifactAsset);
				logger.info("saveCustomAttributes Done!");
				
				oerWriter.setProducingProjects(artifactAsset);
				logger.info("setProducingProjects Done!");
				
				logger.info("************************************************************************************");
				results.add(artifactAsset);
			}
			
			return results;
		} catch (Exception e) {
			throw new MetadataIntrospectionException(e);
		}
	}
	
	
	private void deleteArtifactMetadataEntries(Asset asset) throws MetadataIntrospectionException {
		MetadataEntrySummary[] entries = oerWriter.assetMetadataReadSummary(asset.getID());
		for (int i = 0; i < entries.length; i++) {
			MetadataEntrySummary mes = entries[i];
			if (mes.getEntryType().equals("internal.artifact.store"))
				oerWriter.assetMetadataDelete(mes.getID());
		}
	}
	
	
}