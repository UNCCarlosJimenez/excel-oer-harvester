/**
 * 
 */
package com.unicomer.oer.harvester.writer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xmlbeans.XmlCursor;

import com.flashline.crypto.EncodeDecode;
import com.flashline.registry.openapi.base.OpenAPIException;
import com.flashline.registry.openapi.entity.Asset;
import com.flashline.registry.openapi.entity.AuthToken;
import com.flashline.registry.openapi.entity.Categorization;
import com.flashline.registry.openapi.entity.CategorizationType;
import com.flashline.registry.openapi.entity.NameValue;
import com.flashline.registry.openapi.service.v300.FlashlineRegistryTr;
import com.flashline.util.StringUtils;
import com.oracle.artifact.ArtifactAlgorithm;
import com.oracle.oer.sync.framework.MetadataEntityWriter;
import com.oracle.oer.sync.framework.MetadataIntrospectionConfig;
import com.oracle.oer.sync.framework.MetadataIntrospectionException;
import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.oracle.oer.sync.manifest.ChildSfidType;
import com.oracle.oer.sync.manifest.Entry;
import com.oracle.oer.sync.manifest.RelativePathEntries;
import com.oracle.oer.sync.manifest.RelativePathManifest;
import com.oracle.oer.sync.model.AbstractEntity;
import com.oracle.oer.sync.model.ArtifactEntity;
import com.oracle.oer.sync.model.Entity;
import com.oracle.oer.sync.model.ManifestEntry;
import com.oracle.oer.sync.model.Relationship;
import com.oracle.oer.sync.model.TypeEntity;
import com.oracle.oer.sync.oer.client.component.attributes.Attribute;
import com.oracle.oer.sync.oer.client.component.attributes.AttributeSaver;
import com.oracle.oer.sync.oer.client.component.attributes.AttributeSaverImpl;
import com.oracle.oer.sync.plugin.writer.oer.ALERAssetQueries;
import com.oracle.oer.sync.plugin.writer.oer.ALERConnectionCache;
import com.oracle.oer.sync.plugin.writer.oer.ALERFacade;
import com.oracle.oer.sync.plugin.writer.oer.OERWriter;
import com.oracle.oer.sync.plugin.writer.oer.RelativePathCMF;
import com.unicomer.oer.harvester.model.UnicomerEntity;
import com.unicomer.oer.harvester.writer.artifact.UnicomerArtifactSaver;
import com.unicomer.oer.harvester.writer.artifact.UnicomerEntityWriterImpl;

/**
 * @author carlosj_rodriguez
 *
 */
public class UnicomerOERWriter extends OERWriter {
	private static final String HP_PRODUCT_VERSION = "Product Version";
	private static final String HP_PRODUCT_NAME = "Product Name";
	private static final String API_CAS = "api_view";
	private static final String HP_DEPLOYMENT_STATUS = "Deployment Status";
	public static final String HP_SCOPE = "Scope";
	public static final String SCOPE_LOCAL = "local";
	public static final String SCOPE_GLOBAL = "global";
	private static final String HP_HARVESTER_DESCRIPTION = "Harvester Description";
	private static final String HP_HARVESTER_VERSION = "Harvester Version";
	public static final String HP_NAMESPACE = "Namespace";
	public static final String HP_SHORTNAME = "Short Name";
	private static final String HP_HARVESTED_BY = "Harvested by";
	private static final String HP_DATE_HARVESTED = "Date Harvested";
	private static final String SUMMARY_XML = "Summary XML";
	public static final String HP_INTNAME = "intname";
	public static final List<String> CORE_PROPERTIES = Arrays
			.asList(new String[] { "Product Version", "Product Name", "Harvester Description", "Harvester Version",
					"Namespace", "Short Name", "Harvested by", "Date Harvested", "intname" });
	public static final String RS_REGISTERED = "Registered";
	public static final String RS_SUBMITTED_UNDER_REVIEW = "Submitted - Under Review";
	public static final String RS_SUBMITTED_PENDING_REVIEW = "Submitted - Pending Review";
	public static final String RS_SUBMITTED_UNDER_REVIEW_CLI = "UnderReview";
	public static final String RS_SUBMITTED_PENDING_REVIEW_CLI = "PendingReview";
	public static final String RS_UNSUBMITTED = "Unsubmitted";
	public static Integer UNSUBMITTED = new Integer(10);

	public static Integer PENDING_REVIEW = new Integer(51);

	public static Integer UNDER_REVIEW = new Integer(52);

	public static Integer REGISTERED = new Integer(100);

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	ALERAssetQueries alerQueryTr = null;

	protected ALERFacade facadeTr = null;
	RelativePathCMF relativePathCMF = null;
	private String uri = null;
	private String user = null;
	private String pass = null;
	private Map<String, Asset> entityAssetMap = null;
	private Set<String> relationshipsProcessed = new HashSet<String>();

	EncodeDecode enc = new EncodeDecode();
	private MetadataIntrospectionConfig configManager = null;
	
	protected boolean committed = false;
	private boolean shutdown = false;

	private UnicomerArtifactSaver artifactSaver = new UnicomerArtifactSaver(this);
	private static boolean bEnableTransaction = true;
	private NameValue[] lSuppressionValues = null;
	private MetadataLogger logger = MetadataManager.getLogger(UnicomerOERWriter.class);
	private boolean mPreview;
	
	public UnicomerOERWriter(MetadataIntrospectionConfig config) {
		super(config);
		this.configManager = config;
		bEnableTransaction = !this.configManager.getSettings().getDisableTransaction();
		this.uri = sanitizeURI(this.configManager.getRepository().getUri());
		this.user = this.configManager.getRepository().getCredentials().getUser();
		this.pass = this.enc.decode(this.configManager.getRepository().getCredentials().getPassword());
		
		this.alerQueryTr = new ALERAssetQueries(this.uri, this.user, this.pass);
		this.facadeTr = new ALERFacade(this.uri, this.user, this.pass, this.alerQueryTr);
		this.relativePathCMF = new RelativePathCMF(this.uri, this.user, this.pass, this.facadeTr);
		
		this.entityAssetMap = new Hashtable<String, Asset>();
		NameValue lFileEventsArg = new NameValue();
		lFileEventsArg.setName("FIRE_EVENTS");
		lFileEventsArg.setValue(new Boolean(this.configManager.getSettings().getTriggerEvent()).toString());
		this.lSuppressionValues = new NameValue[] { lFileEventsArg };

		ALERConnectionCache.getInstance().setTimeoutSeconds(this.configManager.getRepository().getTimeout());

		this.mPreview = ((this.configManager.getSettings().getIntrospection() != null)
				&& (this.configManager.getSettings().getIntrospection() != null)
						? this.configManager.getSettings().getIntrospection().getPreview() : false);
	}
	
	public void write(List<Set<Entity>> mapList) throws Exception {
		Date publishedDate = new Date();		
		createIntrospectorMetadataEntry();

		boolean bTxStarted = false;
		this.committed = false;
		shutdown = false;
		Exception errEx = null;
		if (bEnableTransaction) {
			if (!this.uri.endsWith("Tr"))
				throw new Exception(
						"Incorrect URL is detected for connecting to OER. Please use a URL that ends with 'Tr' to transactionally submit artifacts. Example:- http://localhost:7101/aler/services/FlashlineRegistryTr");
			int counter = 0;
			while (counter < 10 && !bTxStarted){
				try {
					counter++;
					this.logger.debug("Starting OER Transaction. " + (counter) + " attempt");
					this.facadeTr.beginTx(this.lSuppressionValues);
					bTxStarted = true;
				} catch (OpenAPIException e) {
					errEx = e;
					if (e.getServerErrorCode() == 16007) {
						this.logger.warn("OER is currently processing a Transaction. Only one concurrent transaction is supported. Will retry after 10 seconds...");
						Thread.sleep(10000L);
					} else {
						this.logger.error(e);
						throw e;
					}
				}
			}
		}
		if ((bEnableTransaction) && (!bTxStarted))
			throw new Exception("Unable to start a OER Transaction...", errEx);

		this.logger.debug("Got " + mapList.size() + " maps for writing.");
		Iterator<Set<Entity>> listIter = mapList.iterator();

		while (listIter.hasNext()) {
			Set<Entity> table = listIter.next();
			write(table, publishedDate);
		}
		
		if (this.mPreview) {
			Iterator<String> lIt = this.entityAssetMap.keySet().iterator();
			int lCount = 0;

			while (lIt.hasNext()) {
				String lAssetID = lIt.next();
				Asset lAsset = this.entityAssetMap.get(lAssetID);

				this.logger
						.info("Asset of type " + lAsset.getTypeName() + " to be created: " + lAsset.getDisplayName());
				lCount++;
			}

			this.logger.info("A total of " + lCount + " new assets will be created or updated\n");
			this.logger.info("Rolling back OER transaction because harvester is in Preview Mode");
			this.facadeTr.rollback();
		} else if (bEnableTransaction) {
			this.logger.debug("Commiting OER Transaction...");
			try {
				this.facadeTr.commitTx();
			} catch (OpenAPIException e) {
				if (e.getServerErrorCode() == 16003) {
					throw new Exception("The server has timed out during harvesting.");
				}
				throw e;
			}
		}

		this.committed = true;
	}

	public void write(Set<Entity> table, Date publishDate) throws Exception {
		for (Entity entity : table) {
			logger.info("CreateEntity: " + entity.getName());
			createEntities(entity, publishDate);
		}
		
		this.relationshipsProcessed.clear();
		for (Entity entity : table) {
			createRelations(entity);
		}

		for (Entity entity : table) {
			Asset asset = getMappedAsset(entity);
			postProcess(entity, asset);
		}
	}
	
	private void createEntities(Entity entity, Date publishDate) throws Exception {
		MetadataEntityWriter artifactWriter = new UnicomerEntityWriterImpl();		
		artifactWriter.write(entity, this, publishDate);
	}
	
	
	public List<Asset> createUnicomerEntity (UnicomerEntity entity, Date publishedDate) throws MetadataIntrospectionException {
		ArtifactAlgorithm algo = entity.getArtifactAlgorithm();
		List<Asset> assetList = new ArrayList<Asset>();
		try {
			long assetType = this.facadeTr.getAssetTypeNoCache(entity.getAssetType());
			assetList = this.artifactSaver.findOrCreateArtifact(assetType, entity, algo, publishedDate, this);
		} catch (Exception e) {
			e.printStackTrace();
			throw new MetadataIntrospectionException(e);
		}
		
		return assetList;
	}
	
	public Asset findAssetUUID(TypeEntity entity) throws Exception {
		String uuid = entity.getHarvesterPropertyValue("uuid");

		if (uuid != null) {
			Asset[] assets = this.alerQueryTr.assetQueryByUUID(uuid);
			if (assets.length > 0) {
				return assets[0];
			}
		}
		return findAsset(entity);
	}

	public Asset findOrCreateAsset(AbstractEntity entity, Date publishedDate) throws MetadataIntrospectionException {
		Asset asset = findAsset(entity);

		if (asset == null) {
			this.logger.debug("Creating new asset for Entity - " + entity.getName());
			try {
				long assetTypeId = this.facadeTr.getAssetTypeNoCache(entity.getAssetType());
				String version = entity.getVersion();
				if (StringUtils.isEmpty(version)) {
					version = "1.0";
				}

				if (!StringUtils.isEmpty(this.configManager.getSettings().getAssetVersion())) {
					version = this.configManager.getSettings().getAssetVersion();
				}

				asset = assetCreate(entity.getName(), version, assetTypeId, entity, publishedDate);

				if ((!this.configManager.getRegistrationStatus().equals("Registered"))
						&& (this.alerQueryTr.getSimpleModel())) {
					asset.setCustomAccessSettings(new String[] { "api_view" });
				}

				asset.setDescription(entity.getDescription());

				if (entity.getKeywords() != null) {
					asset.setKeywords(entity.getKeywords());
				}
				setCategorizations(entity, asset);
				asset = assetUpdate(asset);

				asset = saveCustomAttributes(entity, asset);
			} catch (Exception e) {
				throw new MetadataIntrospectionException(e);
			}
		} else {
			this.logger.debug("Updating new asset for Entity - " + entity.getName() + " Asset ID - " + asset.getID());

			matchUDDIServiceKeys(asset, entity);
			try {
				String assetDescription = asset.getDescription() == null ? "" : asset.getDescription();
				if ((entity.getDescription() != null) && (!assetDescription.equals(entity.getDescription()))
						&& (assetDescription.indexOf(entity.getDescription()) < 0)) {
					asset.setDescription(assetDescription + "\n\n" + entity.getDescription());
				}

				applyCategorizations(asset, false);

				setCategorizations(entity, asset);
				if (entity.getKeywords() != null) {
					asset.setKeywords(entity.getKeywords());
				}

				asset.setActiveStatus(0);
				asset = assetUpdate(asset);

				asset = saveCustomAttributes(entity, asset);
				saveAttributes(asset, entity, publishedDate);

				registerAsset(asset, true);
			} catch (Exception e) {
				throw new MetadataIntrospectionException(e);
			}
		}

		setProducingProjects(asset);

		return asset;
	}
	
	public Asset findAsset(AbstractEntity entity) throws MetadataIntrospectionException {
		if ((entity.getInternalName() == null) || (entity.getInternalName().trim().length() == 0)) {
			String errMsg = "Internal name in the specified entity is null or empty. Cannot proceed...";

			this.logger.error(errMsg);
			throw new MetadataIntrospectionException(errMsg);
		}

		Asset asset = getMappedAsset(entity);
		if (asset == null) {
			try {
				long assTypeId = this.facadeTr.getAssetTypeNoCache(entity.getAssetType());
				Asset[] assetArray = queryAssetByInternalName(entity.getInternalName(), assTypeId);
				if (assetArray.length > 1) {
					String assetIdList = "[";
					for (int i = 0; i < assetArray.length; i++) {
						assetIdList = assetIdList + assetArray[i].getID() + ", ";
					}
					assetIdList = assetIdList.substring(0, assetIdList.length() - 2) + "]";

					String errMsg = "Received more than one assets for internal name query. InternalName: ["
							+ entity.getInternalName() + "] AssetTypeId: [" + assTypeId + "] Returned assets: "
							+ assetIdList;

					this.logger.warn(errMsg);
				}

				if (assetArray.length > 0) {
					asset = assetArray[0];
				}

			} catch (Exception e) {
				throw new MetadataIntrospectionException(e);
			}

		}

		return asset;
	}
	
	public Asset[] assetQueryByName(String assetName, long assetTypeId) throws MetadataIntrospectionException {
		try {
			return this.alerQueryTr.assetQuery(assetName, assetTypeId);
		} catch (Exception e) {
			throw new MetadataIntrospectionException(e);
		}
	}
	
	public Map<AbstractEntity, Asset> assetQueryByHarvesterProp(String propValue, String deploymentURL,
			String assetType, String propName) throws Exception {
		Asset[] assets = this.alerQueryTr.assetQueryByHarvesterProp(propValue, deploymentURL, assetType, propName);

		return convert(assets);
	}

	private Map<AbstractEntity, Asset> convert(Asset[] assets) throws MetadataIntrospectionException, Exception {
		Map<AbstractEntity, Asset> results = new HashMap<AbstractEntity, Asset>();
		for (Asset asset : assets) {
			AttributeSaver saver = new AttributeSaverImpl(this);
			List<Attribute> attributes = saver.readAttributes(asset.getID());

			for (Attribute attribute : attributes) {
				if (attribute.getName().equalsIgnoreCase("intname")) {
					AbstractEntity result = new AbstractEntity(asset.getTypeName(), null, asset.getName(), null,
							asset.getVersion(), null);

					result.setInternalName(attribute.getValue());
					result.setName(asset.getName());
					results.put(result, asset);
				}
			}
		}

		return results;
	}

	private void createRelations(Entity entity) throws Exception {
		if (entity.getRelationshipsProcessed()) {
			this.logger.debug("Entity: [" + entity.getNamespace() + "::" + entity.getName() + "] ID: [" + entity.getId()
					+ "] is already processed processed for relationship.");
			return;
		}
		entity.setRelationshipsProcessed(true);
		this.logger.debug("Creating relations for entity: [" + entity.getNamespace() + "::" + entity.getName() + "] ID: [" + entity.getId() + "]");
		Asset entityAsset = getMappedAsset(entity);
		if (entityAsset == null) {
			throw new MetadataIntrospectionException("No asset found for entity: [" + entity.getNamespace() + "::" + entity.getName() + "] ID: [" + entity.getId() + "].");
		}
		
		List<Relationship> relationships = entity.getRelationships();
		for (Relationship relationship : relationships) {
			Entity relatedEntity = relationship.getRelatedTo();
			Asset relatedEntityAsset = getMappedAsset(relatedEntity);
			if (relatedEntityAsset == null) {
				this.logger.debug("No asset found for related entity: [" + relatedEntity.getNamespace() + "::"
						+ relatedEntity.getName() + "] ID: [" + relatedEntity.getId() + "].");
			} else {
				this.logger.debug("Processing relation: [" + relationship.getName() + "] between Entity: ["
						+ entity.getNamespace() + "::" + entity.getName() + "] ID: [" + entity.getId() + "] AssetId: ["
						+ entityAsset.getID() + "] and Related Entity: [" + relatedEntity.getNamespace() + "::"
						+ relatedEntity.getName() + "] ID: [" + relatedEntity.getId() + "] AssetId: ["
						+ relatedEntityAsset.getID() + "]. UseRelatedEntityAsSource: "
						+ relationship.isUseAsSourceEntity());

				long srcAssetId = -1L;
				long targetAssetId = -1L;
				if (relationship.isUseAsSourceEntity()) {
					srcAssetId = relatedEntityAsset.getID();
					targetAssetId = entityAsset.getID();
				} else {
					srcAssetId = entityAsset.getID();
					targetAssetId = relatedEntityAsset.getID();
				}

				String relString = relationship.getName() + ":" + srcAssetId + ":" + targetAssetId;
				if (!this.relationshipsProcessed.contains(relString)) {
					this.relationshipsProcessed.add(relString);
					createRelation(relationship.getName(), srcAssetId, targetAssetId);

					this.logger.debug("Created relation: [" + relationship.getName() + "] between Entity: ["
							+ entity.getNamespace() + "::" + entity.getName() + "] ID: [" + entity.getId()
							+ "] AssetId: [" + entityAsset.getID() + "] and Related Entity: ["
							+ relatedEntity.getNamespace() + "::" + relatedEntity.getName() + "] ID: [" + entity.getId()
							+ "] AssetId: [" + relatedEntityAsset.getID() + "] using assetId [" + srcAssetId
							+ "] as source and assetId [" + targetAssetId + "] as target.");
				}

				createRelations(relatedEntity);
			}
		}
	}
	
	private void postProcess(Entity assetBase, Asset asset) throws Exception {
		if ((assetBase instanceof ArtifactEntity)) {
			ArtifactEntity entity = (ArtifactEntity) assetBase;

			RelativePathManifest manifest = RelativePathManifest.Factory.newInstance();
			RelativePathEntries entries = manifest.addNewRelativePathEntries();
			for (ManifestEntry path : entity.getManifestEntries()) {
				Entry entry = entries.addNewEntry();
				ChildSfidType sfid = entry.addNewChildSfid();
				entity.getArtifactAlgorithm().toString();

				if (entity.getArtifactAlgorithm() != null) {
					String fingerprinter = entity.getArtifactAlgorithm().getFingerprinterClass().getSimpleName();
					sfid.setType(fingerprinter);
				}
				XmlCursor cursor = sfid.newCursor();
				cursor.setTextValue(path.getChildSFID());
				entry.setChildName(path.getChildName());
				entry.setChildPath(path.getChildPath());
				entry.setAbsolutePath(path.getAbsolutePath());
			}
			this.relativePathCMF.setRelativePath(manifest.xmlText(), asset.getID());
		}
	}
	
	private void matchUDDIServiceKeys(Asset asset, AbstractEntity entity) throws MetadataIntrospectionException {
		this.logger.debug(
				"Matching UDDIServiceKeys from Entity - " + entity.getName() + " and Asset ID - " + asset.getID());

		String entitySvcKey = getUDDIServiceKey(entity);
		this.logger.debug("UDDIServiceKey of Entity - " + entity.getName() + " is " + entitySvcKey);
		if ((entitySvcKey == null) || (entitySvcKey.trim().length() == 0)) {
			return;
		}

		String assetSvcKey = null;
		try {
			assetSvcKey = getUDDIServiceKey(asset);
		} catch (Exception ex) {
			String errMsg = "Failed to get UDDI service key from asset with ID " + asset.getID() + " due to: "
					+ ex.getMessage();
			this.logger.error(errMsg, ex);
			throw new MetadataIntrospectionException(errMsg, ex);
		}

		if ((assetSvcKey == null) || (assetSvcKey.trim().length() == 0)) {
			this.logger.debug("UDDIServiceKey is NULL for Asset ID - " + asset.getID());
			return;
		}

		this.logger.debug("UDDIServiceKey of Asset ID - " + asset.getID() + " is " + assetSvcKey);

		if (assetSvcKey.equals(entitySvcKey)) {
			return;
		}

		String errMsg = "UDDI Service Key mismatch! UDDI service key from entity and asset are not same... EntityID:["
				+ entity.getId() + "] EntityName:[" + entity.getName() + "] EntityUDDISvcKey:[" + entitySvcKey
				+ "]  AssetID:[" + asset.getID() + "] AssetUDDISvcKey:[" + assetSvcKey + "]";

		this.logger.error(errMsg);
		throw new MetadataIntrospectionException(errMsg);
	}
	
	
	public void mapAssetToEntity(Asset asset, Entity entity) {
		this.entityAssetMap.put(entity.getId(), asset);
		this.logger.debug("Mapped Asset Id: [" + asset.getID() + "] with Entity : [" + entity.getName() + "] ID: [" + entity.getId() + "].");
	}
	
	public AuthToken getAuthToken() throws Exception {
		FlashlineRegistryTr oer = ALERConnectionCache.getInstance().getFlashlineRegistry(this.uri);
		return ALERConnectionCache.getInstance().getAuthToken(this.uri, this.user, this.pass, oer);
	}
	
	public Asset getMappedAsset(Entity entity) {
		this.logger.debug("Finding asset for Entity : [" + entity.getNamespace() + "::" + entity.getName() + "] ID: [" + entity.getId() + "] in the map.");

		Asset asset = this.entityAssetMap.get(entity.getId());
		if (asset != null) {
			this.logger.debug("Returning asset from entity-asset map. AssetId: [" + asset.getID() + "] for Entity : ["
					+ entity.getNamespace() + "::" + entity.getName() + "] ID: [" + entity.getId() + "].");
		} else {
			this.logger.debug("No asset exists in entity-asset map for Entity : [" + entity.getNamespace() + "::"
					+ entity.getName() + "] ID: [" + entity.getId() + "].");
		}
		
		return asset;
	}
	
	public void shutdown() {
		this.logger.info("Starting OER Shutdown and Clean up...");
		try {
			if ((!this.committed) && (!this.shutdown)) {
				System.out.println("Rolling Back");
				if (bEnableTransaction)
					try {
						this.facadeTr.rollback();
					} catch (OpenAPIException e) {
						if (e.getServerErrorCode() == 16003)
							this.logger.warn("Cannot rollback as the transaction has timed out.");
					}
			}
		} finally {
			this.shutdown = true;
			ALERConnectionCache.getInstance().clear();
		}
	}
	
}
