/**
 * 
 */
package com.unicomer.oer.harvester.writer.artifact;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flashline.registry.openapi.entity.Asset;
import com.oracle.oer.sync.framework.MetadataEntityWriter;
import com.oracle.oer.sync.framework.MetadataIntrospectionException;
import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.oracle.oer.sync.framework.MetadataWriter;
import com.oracle.oer.sync.model.AbstractEntity;
import com.oracle.oer.sync.model.Entity;
import com.oracle.oer.sync.model.Relationship;
import com.oracle.oer.sync.model.TypeEntity;
import com.oracle.oer.sync.plugin.writer.oer.OERWriter;
import com.unicomer.oer.harvester.model.UnicomerEntity;
import com.unicomer.oer.harvester.writer.UnicomerOERWriter;

/**
 * @author carlosj_rodriguez
 *
 */
public class UnicomerEntityWriterImpl implements MetadataEntityWriter {
	private MetadataLogger logger = MetadataManager.getLogger(UnicomerEntityWriterImpl.class);
	private Map<String, Asset> typeAssetsByInternalName = new HashMap<String, Asset>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.oracle.oer.sync.framework.MetadataEntityWriter#write(com.oracle.oer.
	 * sync.model.Entity, com.oracle.oer.sync.framework.MetadataWriter,
	 * java.util.Date)
	 */
	public void write(Entity entity, MetadataWriter metadataWriter, Date publishedDate) throws Exception {
		this.logger.debug("Creating assets.");
		UnicomerOERWriter oerWriter = (UnicomerOERWriter) metadataWriter;
		
		if (oerWriter.getMappedAsset(entity) != null) {
			return;
		}
		
		if ((entity instanceof UnicomerEntity)) {
			createUnicomerEntity((UnicomerEntity) entity, oerWriter, publishedDate);
		} else
			throw new MetadataIntrospectionException("unexpected asset class: " + entity.getClass());
	}

	protected Asset createUnicomerEntity(UnicomerEntity entity, UnicomerOERWriter oerWriter, Date publishedDate)
			throws Exception {
		List<Asset> assets = oerWriter.createUnicomerEntity(entity, publishedDate);
		
		if ((assets.size() == 0) || (assets.size() > 1)) {
			String errMsg = "Expected to create/find only one artifact asset. Instead created/found "
					+ assets.size() + " assets. ";
			throw new MetadataIntrospectionException(errMsg);
		}
		
		Asset srcAsset = assets.get(0);
		
		oerWriter.mapAssetToEntity(srcAsset, entity);
		
		List<Relationship> relationships = entity.getRelationships();
		for (Relationship rel : relationships) {
			write(rel.getRelatedTo(), oerWriter, publishedDate);
		}

		return srcAsset;
	}

	

	protected Asset createAbstractEntity(AbstractEntity entity, OERWriter oerWriter, Date publishedDate)
			throws Exception {
		Asset srcAsset = oerWriter.findOrCreateAsset(entity, publishedDate);

		oerWriter.mapAssetToEntity(srcAsset, entity);

		List<Relationship> relationships = entity.getRelationships();
		for (Relationship rel : relationships) {
			write(rel.getRelatedTo(), oerWriter, publishedDate);
		}

		return srcAsset;
	}

	protected Asset findTypeEntity(TypeEntity entity, OERWriter oerWriter, Date publishDate) throws Exception {
		Asset assetFound = (Asset) this.typeAssetsByInternalName.get(entity.getInternalName());
		if (assetFound != null) {
			return assetFound;
		}

		Asset srcAsset = oerWriter.findAssetUUID(entity);
		if (srcAsset == null) {
			boolean supportPartial = MetadataManager.getInstance().getConfigManager().getIntrospection()
					.getSupportPartialIntrospection();
			if (!supportPartial) {
				throw new MetadataIntrospectionException("Unable to find Type asset: " + entity.getName()
						+ ".  You must import this asset via an OER Solution pack or create it manually.  "
						+ "See the Harvester User Guide, in the section 'Advanced Configuration | Adapters and Applications'.");
			}

			return null;
		}

		oerWriter.mapAssetToEntity(srcAsset, entity);

		List<Relationship> relationships = entity.getRelationships();
		for (Relationship rel : relationships) {
			write(rel.getRelatedTo(), oerWriter, publishDate);
		}

		this.typeAssetsByInternalName.put(entity.getInternalName(), srcAsset);

		return srcAsset;
	}

}
