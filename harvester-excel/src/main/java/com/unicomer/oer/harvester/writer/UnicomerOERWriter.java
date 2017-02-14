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
import com.flashline.registry.openapi.entity.ArtifactStoreBean;
import com.flashline.registry.openapi.entity.Asset;
import com.flashline.registry.openapi.entity.AssetSummary;
import com.flashline.registry.openapi.entity.AuthToken;
import com.flashline.registry.openapi.entity.CategorizationType;
import com.flashline.registry.openapi.entity.MetadataEntrySummary;
import com.flashline.registry.openapi.entity.MetadataEntryTypeSummary;
import com.flashline.registry.openapi.entity.NameValue;
import com.flashline.registry.openapi.entity.ProjectSummary;
import com.flashline.registry.openapi.service.v300.FlashlineRegistryTr;
import com.flashline.util.StringUtils;
import com.oracle.artifact.ArtifactAlgorithm;
import com.oracle.oer.integration.harvester.DeploymentStatus;
import com.oracle.oer.integration.harvester.HarvesterSettings;
import com.oracle.oer.integration.harvester.ProducingProject;
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
import com.oracle.oer.sync.oer.client.component.artifact.ArtifactResult;
import com.oracle.oer.sync.oer.client.component.attributes.Attribute;
import com.oracle.oer.sync.oer.client.component.attributes.AttributeSaver;
import com.oracle.oer.sync.oer.client.component.attributes.AttributeSaverImpl;
import com.oracle.oer.sync.oer.client.component.name.AssetNamerImpl;
import com.oracle.oer.sync.plugin.writer.oer.ALERAssetQueries;
import com.oracle.oer.sync.plugin.writer.oer.ALERConnectionCache;
import com.oracle.oer.sync.plugin.writer.oer.ALERFacade;
import com.oracle.oer.sync.plugin.writer.oer.OERWriter;
import com.oracle.oer.sync.plugin.writer.oer.OERWriterStats;
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
	public static final List CORE_PROPERTIES = Arrays
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
	private Map<String, List<ArtifactResult>> entityArtifactResultMap = null;
	private Hashtable internalNameAssetMap = null;

	private OERWriterStats stats = new OERWriterStats();

	private Set relationshipsProcessed = new HashSet();

	EncodeDecode enc = new EncodeDecode();
	private MetadataIntrospectionConfig configManager = null;
	
	protected boolean committed = false;
	private boolean shutdown = false;

	private UnicomerArtifactSaver artifactSaver = new UnicomerArtifactSaver(this);//	private ArtifactSaver artifactSaver = new ArtifactSaverImpl(this);
	private static boolean bEnableTransaction = true;
	private NameValue[] lSuppressionValues = null;
	private MetadataLogger logger = MetadataManager.getLogger(UnicomerOERWriter.class);
	private boolean mPreview;
	
	public UnicomerOERWriter(MetadataIntrospectionConfig config) {
		super(config);
		this.configManager = config;
		logger.debug("this.configManager.getSettings().getDisableTransaction()="+this.configManager.getSettings().getDisableTransaction());
		bEnableTransaction = this.configManager.getSettings().getDisableTransaction();
		this.uri = sanitizeURI(this.configManager.getRepository().getUri());
		this.user = this.configManager.getRepository().getCredentials().getUser();
		this.pass = this.enc.decode(this.configManager.getRepository().getCredentials().getPassword());

		this.alerQueryTr = new ALERAssetQueries(this.uri, this.user, this.pass);
		this.facadeTr = new ALERFacade(this.uri, this.user, this.pass, this.alerQueryTr);
		this.relativePathCMF = new RelativePathCMF(this.uri, this.user, this.pass, this.facadeTr);

		this.entityAssetMap = new Hashtable();
		this.entityArtifactResultMap = new Hashtable();
		this.internalNameAssetMap = new Hashtable();

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
		logger.info("bEnableTransaction = " + bEnableTransaction);
		Date publishedDate = new Date();
		
		createIntrospectorMetadataEntry();

		boolean bTxStarted = false;
		this.committed = false;
		this.shutdown = false;
		Exception errEx = null;
		if (bEnableTransaction) {
			if (!this.uri.endsWith("Tr"))
				throw new Exception(
						"Incorrect URL is detected for connecting to OER. Please use a URL that ends with 'Tr' to transactionally submit artifacts. Example:- http://localhost:7101/aler/services/FlashlineRegistryTr");
			for (int i = 0; i < 10; i++) {
				try {
					this.logger.debug("Starting OER Transaction. " + (i + 1) + " attempt");
					this.facadeTr.beginTx(this.lSuppressionValues);
					bTxStarted = true;
				} catch (OpenAPIException e) {
					errEx = e;
					if (e.getServerErrorCode() == 16007) {
						this.logger.warn(
								"OER is currently processing a Transaction. Only one concurrent transaction is supported. Will retry after 10 seconds...");
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
		Iterator listIter = mapList.iterator();

		while (listIter.hasNext()) {
			Set table = (Set) listIter.next();
			write(table, publishedDate);
		}
		
		if (this.mPreview) {
			Iterator lIt = this.entityAssetMap.keySet().iterator();
			int lCount = 0;

			while (lIt.hasNext()) {
				String lAssetID = (String) lIt.next();
				Asset lAsset = (Asset) this.entityAssetMap.get(lAssetID);

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

	public static String sanitizeURI(String lURI) {
		if (lURI.indexOf("services/FlashlineRegistry") == -1) {
			if (lURI.endsWith("/"))
				lURI = lURI + "services/FlashlineRegistryTr";
			else
				lURI = lURI + "/services/FlashlineRegistryTr";
		}
		return lURI;
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

	protected void createIntrospectorMetadataEntry() throws Exception {
		this.facadeTr.findOrCreateMetadataEntryType("internal.introspector.store");
		this.facadeTr.findOrCreateMetadataEntryType("internal.introspector.manifest.store");
	}

	private void createEntities(Entity entity, Date publishDate) throws Exception {
		MetadataEntityWriter artifactWriter = new UnicomerEntityWriterImpl();
//		
//		String assetType = entity.getAssetType();
//		this.logger.debug("Asset type of entity [" + entity.getNamespace() + "::" + entity.getName() + "] is ["
//				+ assetType + "].");
//		if (assetType == null) {
//			String errMsg = "Failed to get asset type for entity [" + entity.getNamespace() + "::" + entity.getName()
//					+ "]";
//			this.logger.error(errMsg);
//			throw new MetadataIntrospectionException(errMsg);
//		}
//		try {
//			artifactWriter = MetadataManager.getInstance().getMetadataEntityWriter(assetType);
//			this.logger.debug("Artifact Writer for entity [" + entity.getName() + "] is ["
//					+ artifactWriter.getClass().getName() + "].");
//		} catch (Exception e) {
//			String errMsg = "Failed to load MetadataEntityWriter class for entity [" + entity.getNamespace() + "::"
//					+ entity.getName() + "] with type [" + assetType + "]";
//			this.logger.error(errMsg);
//			throw new MetadataIntrospectionException(errMsg, e);
//		}
		
		artifactWriter.write(entity, this, publishDate);
	}

	public void createRelation(String relationName, long sourceAssetId, long targetAssetId) throws Exception {
		Asset sourceAsset = this.facadeTr.assetRead(sourceAssetId);
		Asset targetAsset = this.facadeTr.assetRead(targetAssetId);

		sourceAsset = this.facadeTr.createRelationship(relationName, sourceAsset, targetAsset);
	}

	public Asset[] assetQueryBySFID(String sfid) throws MetadataIntrospectionException {
		try {
			return this.alerQueryTr.assetQueryBySFID(sfid);
		} catch (Exception e) {
			throw new MetadataIntrospectionException(e);
		}
	}
	
	public Asset[] assetQueryByName(String assetName) throws MetadataIntrospectionException {
		try {
			return this.alerQueryTr.assetQueryByName(assetName);
		} catch (Exception e) {
			throw new MetadataIntrospectionException(e);
		}
	}

	public AssetSummary[] assetQueryByNameStartsWith(String namePrefix, String version)
			throws MetadataIntrospectionException {
		try {
			return this.alerQueryTr.assetQueryByNameStartsWith(namePrefix, version);
		} catch (Exception e) {
			throw new MetadataIntrospectionException(e);
		}
	}

	public Asset assetCreate(String name, String version, long assetTypeID, Entity entity, Date publishedDate)
			throws MetadataIntrospectionException {
		Asset asset = null;
		try {
			asset = this.facadeTr.assetCreate(name, version, assetTypeID);
		} catch (Exception e) {
			String newName = new AssetNamerImpl(this).makeUnique(name, version);
			try {
				asset = this.facadeTr.assetCreate(newName, version, assetTypeID);
				if (entity.getKeywords() != null) {
					asset.setKeywords(entity.getKeywords());
					asset = this.facadeTr.assetUpdate(asset);
				}
			} catch (Exception newEx) {
				throw new MetadataIntrospectionException(newEx);
			}
		}
		try {
			String assetTypeName = this.facadeTr.getAssetTypeName(assetTypeID);
			this.stats.addAssetCreated(assetTypeName);

			registerAsset(asset);

			applyCategorizations(asset, true);

			saveAttributes(asset, entity, publishedDate);
		} catch (Exception e) {
			throw new MetadataIntrospectionException(e);
		}

		return asset;
	}

	public void applyCategorizations(Asset asset, boolean applyCategorizationsToAll) throws Exception {
		if ((this.configManager.getApplyCategorizations() != null)
				&& (this.configManager.getApplyCategorizations().length > 0)) {
			List categorizationsList = new ArrayList();
			com.flashline.registry.openapi.entity.Categorization[] existingCategorizations = asset.getCategorizations();

			if (existingCategorizations != null) {
				for (com.flashline.registry.openapi.entity.Categorization categorization : existingCategorizations) {
					categorizationsList.add(categorization);
				}
			}

			boolean saveAsset = false;
			for (int i = 0; i < this.configManager.getApplyCategorizations().length; i++) {
				com.oracle.oer.integration.harvester.Categorization harvesterCategorization = this.configManager
						.getApplyCategorizations()[i];

				if ((applyCategorizationsToAll) || (harvesterCategorization.getApplyCategorizationToExistingAssets())) {
					com.flashline.registry.openapi.entity.Categorization oerCategorization = null;
					CategorizationType catType = null;
					try {
						oerCategorization = this.alerQueryTr.getCategorization(harvesterCategorization.getType(),
								harvesterCategorization.getValue());
						catType = this.facadeTr.getCategorizationType(harvesterCategorization.getType());

						if (oerCategorization == null)
							throw new MetadataIntrospectionException(harvesterCategorization.getType()
									+ " is not a valid type or " + harvesterCategorization.getValue()
									+ " is not valid.  Please validate the categorizations in the Harvester Settings file.");
					} catch (Exception e) {
						throw new MetadataIntrospectionException(harvesterCategorization.getType()
								+ " is not a valid type or " + harvesterCategorization.getValue()
								+ " is not valid.  Please validate the categorizations in the Harvester Settings file.");
					}

					boolean found = false;
					if ((catType != null) && (catType.isExclusiveAssign())) {
						for (int x = 0; x < categorizationsList.size(); x++) {
							com.flashline.registry.openapi.entity.Categorization cat = (com.flashline.registry.openapi.entity.Categorization) categorizationsList
									.get(x);
							if (cat.getTypeID() == catType.getID()) {
								categorizationsList.set(x, oerCategorization);
								found = true;
								break;
							}
						}
					}
					if (!found) {
						categorizationsList.add(oerCategorization);
					}
					saveAsset = true;
				}
			}

			if (saveAsset) {
				asset.setCategorizations((com.flashline.registry.openapi.entity.Categorization[]) categorizationsList
						.toArray(new com.flashline.registry.openapi.entity.Categorization[categorizationsList.size()]));
				assetUpdate(asset);
			}
		}
	}

	public void registerAsset(Asset asset, boolean checkConfig) throws Exception {
		if (this.configManager.getApplyRegistrationStatusToExistingAssets()) {
			String registrationStatus = this.configManager.getRegistrationStatus();
			int assetRegStatus = asset.getRegistrationStatus();

			if (registrationStatus.equals("Unsubmitted")) {
				if (assetRegStatus == REGISTERED.intValue()) {
					this.facadeTr.assetUnregister(asset.getID());
					this.facadeTr.assetUnaccept(asset.getID());
					this.facadeTr.assetUnsubmit(asset.getID());
				} else if (assetRegStatus == UNDER_REVIEW.intValue()) {
					this.facadeTr.assetUnaccept(asset.getID());
					this.facadeTr.assetUnsubmit(asset.getID());
				} else if (assetRegStatus == PENDING_REVIEW.intValue()) {
					this.facadeTr.assetUnsubmit(asset.getID());
				}
			} else if ((registrationStatus.equals("Submitted - Pending Review"))
					|| (registrationStatus.equals("PendingReview"))) {
				if (assetRegStatus == REGISTERED.intValue()) {
					this.facadeTr.assetUnregister(asset.getID());
					this.facadeTr.assetUnaccept(asset.getID());
				} else if (assetRegStatus == UNDER_REVIEW.intValue()) {
					this.facadeTr.assetUnaccept(asset.getID());
				} else if (assetRegStatus == UNSUBMITTED.intValue()) {
					this.facadeTr.assetSubmit(asset.getID());
				}
			} else if ((registrationStatus.equals("Submitted - Under Review"))
					|| (registrationStatus.equals("UnderReview"))) {
				if (assetRegStatus == REGISTERED.intValue())
					this.facadeTr.assetUnregister(asset.getID());
				else if (assetRegStatus != UNDER_REVIEW.intValue())
					this.facadeTr.assetAccept(asset.getID());
			} else if (registrationStatus.equals("Registered")) {
				if (assetRegStatus != REGISTERED.intValue())
					this.facadeTr.assetRegister(asset.getID());
			} else if (!StringUtils.isEmpty(registrationStatus))
				throw new MetadataIntrospectionException("Invalid registration status: " + registrationStatus + ".  "
						+ "The following statuses are valid: \"Unsubmitted\", \"Submitted - Pending Review\", \"Submitted - Under Review\", \"Registered\".");
		}
	}

	private void registerAsset(Asset asset) throws Exception {
		String registrationStatus = this.configManager.getRegistrationStatus();
		if (!registrationStatus.equals("Unsubmitted")) {
			if ((registrationStatus.equals("Submitted - Pending Review"))
					|| (registrationStatus.equals("PendingReview")))
				this.facadeTr.assetSubmit(asset.getID());
			else if ((registrationStatus.equals("Submitted - Under Review"))
					|| (registrationStatus.equals("UnderReview")))
				this.facadeTr.assetAccept(asset.getID());
			else if (registrationStatus.equals("Registered"))
				this.facadeTr.assetRegister(asset.getID());
			else if (!StringUtils.isEmpty(registrationStatus))
				throw new MetadataIntrospectionException("Invalid registration status: " + registrationStatus + ".  "
						+ "The following statuses are valid: \"Unsubmitted\", \"Submitted - Pending Review\", \"Submitted - Under Review\", \"Registered\".");
		}
	}

	public Asset assetUpdate(Asset asset) throws MetadataIntrospectionException {
		try {
			return this.facadeTr.assetUpdate(asset);
		} catch (Exception e) {
			throw new MetadataIntrospectionException(e);
		}
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
	
//	public List<Asset> createArtifactEntity (ArtifactEntity entity, Date publishedDate) throws MetadataIntrospectionException {
//		ArtifactAlgorithm algo = entity.getArtifactAlgorithm();
//		List<Asset> assetList = new ArrayList<Asset>();
//		try {
//			long assetType = this.facadeTr.getAssetTypeNoCache(entity.getAssetType());
//			assetList = this.artifactSaver.findOrCreateArtifact(assetType, entity, algo, publishedDate, this);
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new MetadataIntrospectionException(e);
//		}
//		
//		return assetList;
//	}
	
//	public ArtifactResult[] findOrCreateArtifactAssets(ArtifactEntity entity, Date publishedDate)
//			throws MetadataIntrospectionException {
//		List artifactList = getMappedArtifactResultList(entity);
//		if (artifactList != null) {
//			this.logger.debug(
//					"Returning ArtifactResultList from cache [List size: " + artifactList.size() + "] for Entity : ["
//							+ entity.getNamespace() + "::" + entity.getName() + "] ID: [" + entity.getId() + "].");
//
//			return (ArtifactResult[]) artifactList.toArray(new ArtifactResult[artifactList.size()]);
//		}
//		
//		ArtifactAlgorithm algo = entity.getArtifactAlgorithm();
//		
//		ArtifactResult[] artifactInfoArray = null;
//		try {
//			long assetType = this.facadeTr.getAssetTypeNoCache(entity.getAssetType());
//			String relPath = entity.getRelativePath();
//			this.artifactSaver.findOrCreateArtifact(assetType, entity, algo, publishedDate, this);
//			
//			mapArtifactResultListToEntity(artifactList, entity);			
//			artifactInfoArray = new ArtifactResult[artifactList.size()];
//			
//			for (int i = 0; i < artifactList.size(); i++) {
//				ArtifactResult tmpArtifact = (ArtifactResult) artifactList.get(i);
//				artifactInfoArray[i] = tmpArtifact;
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new MetadataIntrospectionException(e);
//		}
//
//		return artifactInfoArray;
//	}
//	
//	public ArtifactResult[] findOrCreateArtifactAssetsOld(ArtifactEntity entity, Date publishedDate)
//			throws MetadataIntrospectionException {
//		List artifactList = getMappedArtifactResultList(entity);
//		if (artifactList != null) {
//			this.logger.debug(
//					"Returning ArtifactResultList from cache [List size: " + artifactList.size() + "] for Entity : ["
//							+ entity.getNamespace() + "::" + entity.getName() + "] ID: [" + entity.getId() + "].");
//
//			return (ArtifactResult[]) artifactList.toArray(new ArtifactResult[artifactList.size()]);
//		}
//
//		ArtifactAlgorithm algo = entity.getArtifactAlgorithm();
//
//		ArtifactResult[] artifactInfoArray = null;
//		try {
//			MetadataManager metadataManager = MetadataManager.getInstance();
//			long assetType = this.facadeTr.getAssetTypeNoCache(entity.getAssetType());
//
//			URI artifactURI = new URI(entity.getPath());
//			byte[] artifactContents = metadataManager.loadBytesFromURI(artifactURI);
//
//			String relPath = entity.getRelativePath();
//
//			if (StringUtils.isEmpty(relPath)) {
//				String path = entity.getPath();
//				if (FileUtils.isURL(path))
//					relPath = path;
//				else
//					relPath = StringUtils.getShortName(path);
//			}
//
//			String downloadURI = null;
//			String artifactURIString = artifactURI.normalize().toString();
//			ArtifactStoreBean artifactStore = metadataManager.getArtifactStore();
//
//			if (artifactStore != null) {
//				int index = artifactURIString.indexOf("path=");
//				if ((index > 0) && (artifactURIString.contains("com.flashline.cmee.servlet.enterprisetab.Download"))) {
//					downloadURI = artifactURIString.substring(index + "path=".length());
//
//					downloadURI = URLDecoder.decode(downloadURI, "UTF-8");
//
//					String prefix = "rep://" + artifactStore.getName() + "/";
//					relPath = downloadURI.substring(prefix.length());
//				}
//			} else if (FileUtils.isRemoteURL(artifactURIString)) {
//				relPath = artifactURIString;
//			}
//
//			if (artifactURIString.startsWith("oramds:/")) {
//				relPath = artifactURIString;
//			}
//
//			artifactList = this.artifactSaver.findOrCreateArtifact(artifactURI, artifactContents, assetType, entity,
//					relPath, downloadURI, algo, publishedDate);
//
//			mapArtifactResultListToEntity(artifactList, entity);
//
//			artifactInfoArray = new ArtifactResult[artifactList.size()];
//
//			for (int i = 0; i < artifactList.size(); i++) {
//				ArtifactResult tmpArtifact = (ArtifactResult) artifactList.get(i);
//				artifactInfoArray[i] = tmpArtifact;
//			}
//		} catch (Exception e) {
//			throw new MetadataIntrospectionException(e);
//		}
//
//		return artifactInfoArray;
//	}

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

	public void setProducingProjects(Asset pAsset) throws MetadataIntrospectionException {
		ProducingProject[] lProjects = this.configManager.getSettings().getProducingProjectArray();
		List lProjectIdsList = new ArrayList();
		if (lProjects != null) {
			for (ProducingProject lProject : lProjects) {
				ProjectSummary lProjectSummary = null;
				try {
					lProjectSummary = this.facadeTr.getProject(lProject.getName());
				} catch (Exception e) {
					throw new MetadataIntrospectionException(e);
				}

				if (lProjectSummary == null) {
					throw new MetadataIntrospectionException("The project, " + lProject.getName()
							+ ", does not exist in OER.  Please create it before harvesting.");
				}

				lProjectIdsList.add(Long.valueOf(lProjectSummary.getID()));
			}

			if (pAsset.getProducingProjectsIDs() != null) {
				for (long lId : pAsset.getProducingProjectsIDs()) {
					if (!lProjectIdsList.contains(Long.valueOf(lId))) {
						lProjectIdsList.add(Long.valueOf(lId));
					}
				}
			}
			long[] lIdsArray = new long[lProjectIdsList.size()];

			for (int i = 0; i < lProjectIdsList.size(); i++) {
				lIdsArray[i] = ((Long) lProjectIdsList.get(i)).longValue();
			}

			pAsset.setProducingProjectsIDs(lIdsArray);
			pAsset = assetUpdate(pAsset);
		}
	}
	
	public void setCategorizations(Entity entity, Asset asset) throws MetadataIntrospectionException {
		try {
			Set results = new HashSet();
			com.flashline.registry.openapi.entity.Categorization[] categorizations = asset.getCategorizations();
			if (categorizations != null) {
				for (com.flashline.registry.openapi.entity.Categorization oldCat : categorizations) {
					results.add(oldCat);
				}

			}
			
			for (Attribute newCatAttr : entity.getCategorizations()) {
				logger.debug("Looking for Categorization "+ newCatAttr.getName());
				CategorizationType newCatType = this.facadeTr.getCategorizationType(newCatAttr.getName());
				List<com.flashline.registry.openapi.entity.Categorization> newCats = this.facadeTr.getCategorizations(newCatAttr.getName(), newCatAttr.getValues());
				
				for (com.flashline.registry.openapi.entity.Categorization newCat : newCats)
					if (newCat != null) {
						Iterator iter;
						if (newCatType.isExclusiveAssign()) {
							for (iter = results.iterator(); iter.hasNext();) {
								com.flashline.registry.openapi.entity.Categorization next = (com.flashline.registry.openapi.entity.Categorization) iter
										.next();
								if (next.getTypeID() == newCat.getTypeID()) {
									iter.remove();
								}
							}
						}
						results.add(newCat);
					}
			}
			CategorizationType newCatType;
			asset.setCategorizations((com.flashline.registry.openapi.entity.Categorization[]) results
					.toArray(new com.flashline.registry.openapi.entity.Categorization[results.size()]));
		} catch (Exception e) {
			throw new MetadataIntrospectionException(e);
		}
	}

	public Asset saveCustomAttributes(Entity entity, Asset srcAsset) throws Exception {
		Set<Map.Entry<String,String>> entrySet = entity.getCustomData().entrySet();
		String[] xpathFields = new String[entrySet.size()];
		String[] values = new String[entrySet.size()];
		int index = 0;
		for (Map.Entry<String,String> entry : entrySet) {
			xpathFields[index] = ((String) entry.getKey());
			values[index] = ((String) entry.getValue());
			index++;
		}
		return saveCustomAttributes(srcAsset, xpathFields, values);
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
				logger.info("***********************Buscando assetType para " + entity.getAssetType());
				long assTypeId = this.facadeTr.getAssetTypeNoCache(entity.getAssetType());
				logger.info("***********************ID de assetType para " + entity.getAssetType() + ": " + assTypeId);
				
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

	public Asset[] queryAssetByInternalName(String assetInternalName, long assetTypeId) throws Exception {
		if (this.internalNameAssetMap.containsKey(assetInternalName)) {
			Asset asset = (Asset) this.internalNameAssetMap.get(assetInternalName);
			this.logger.debug("Returning asset - Name: [" + asset.getName() + "] ID: [" + asset.getID()
					+ "] from MAP for internal name: [" + assetInternalName + "]");

			return new Asset[] { asset };
		}

		Asset[] assets = null;
		assets = this.facadeTr.queryAssetByInternalName(assetTypeId, assetInternalName);

		return assets;
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
	          AbstractEntity result = new AbstractEntity(asset.getTypeName(), null, asset.getName(), null, asset.getVersion(), null);

	          result.setInternalName(attribute.getValue());
	          result.setName(asset.getName());
	          results.put(result, asset);
	        }
	      }
	    }

	    return results;
	  }

	public Asset getMappedAsset(Entity entity) {
		this.logger.debug("Finding asset for Entity : [" + entity.getNamespace() + "::" + entity.getName() + "] ID: ["
				+ entity.getId() + "] in the map.");
		
		Asset asset = (Asset) this.entityAssetMap.get(entity.getId());
		if (asset != null) {
			this.logger.debug("Returning asset from entity-asset map. AssetId: [" + asset.getID() + "] for Entity : ["
					+ entity.getNamespace() + "::" + entity.getName() + "] ID: [" + entity.getId() + "].");
		} else {
			this.logger.debug("No asset exists in entity-asset map for Entity : [" + entity.getNamespace() + "::"
					+ entity.getName() + "] ID: [" + entity.getId() + "].");
		}

		return asset;
	}

	public void mapAssetToEntity(Asset asset, Entity entity) {
		this.entityAssetMap.put(entity.getId(), asset);
		this.logger.debug("Mapped Asset Id: [" + asset.getID() + "] with Entity : [" + entity.getNamespace() + "::"
				+ entity.getName() + "] ID: [" + entity.getId() + "].");
	}

	public List<ArtifactResult> getMappedArtifactResultList(Entity entity) {
		this.logger.debug("Finding ArtifactRestult list for Entity : [" + entity.getNamespace() + "::"
				+ entity.getName() + "] ID: [" + entity.getId() + "] in entity-artifact-result.");

		List artifactList = (List) this.entityArtifactResultMap.get(entity.getId());
		if (artifactList != null) {
			this.logger.debug("Returning ArtifactRestult list from entity-artifact-result map [List size: "
					+ artifactList.size() + "] for Entity : [" + entity.getNamespace() + "::" + entity.getName()
					+ "] ID: [" + entity.getId() + "].");
		} else {
			this.logger.debug("No ArtifactRestult list found in entity-artifact-result map for Entity : ["
					+ entity.getNamespace() + "::" + entity.getName() + "] ID: [" + entity.getId() + "].");
		}

		return artifactList;
	}

	private void mapArtifactResultListToEntity(List artifactList, Entity entity) {
		this.entityArtifactResultMap.put(entity.getId(), artifactList);
		this.logger.debug("Put ArtifactRestult list [List size: " + artifactList.size() + "] with Entity : ["
				+ entity.getNamespace() + "::" + entity.getName() + "] ID: [" + entity.getId()
				+ "] entity-artifact-result map.");
	}

	public MetadataEntrySummary assetMetadataCreate(MetadataEntrySummary entry, String data)
			throws MetadataIntrospectionException {
		try {
			return this.facadeTr.assetMetadataCreate(entry, data);
		} catch (Exception e) {
			throw new MetadataIntrospectionException(e);
		}
	}

	public MetadataEntrySummary assetMetadataCreate(MetadataEntrySummary entry, byte[] pData)
			throws MetadataIntrospectionException {
		try {
			return this.facadeTr.assetMetadataCreate(entry, pData);
		} catch (Exception e) {
			throw new MetadataIntrospectionException(e);
		}
	}

	public void assetMetadataUpdate(MetadataEntrySummary entry, String data) throws MetadataIntrospectionException {
		try {
			this.facadeTr.assetMetadataUpdate(entry, data);
		} catch (Exception e) {
			throw new MetadataIntrospectionException(e);
		}
	}

	public Map<MetadataEntrySummary, String> assetMetadataRead(long assetID) throws MetadataIntrospectionException {
		try {
			return this.facadeTr.assetMetadataRead(assetID);
		} catch (Exception e) {
			throw new MetadataIntrospectionException(e);
		}
	}

	public MetadataEntrySummary[] assetMetadataReadSummary(long assetID) throws MetadataIntrospectionException {
		try {
			return this.facadeTr.assetMetadataReadSummary(assetID);
		} catch (Exception e) {
			throw new MetadataIntrospectionException(e);
		}
	}

	public void assetMetadataDelete(long entryID) throws MetadataIntrospectionException {
		try {
			this.facadeTr.assetMetadataDelete(entryID);
		} catch (Exception e) {
			throw new MetadataIntrospectionException(e);
		}
	}

	public MetadataEntryTypeSummary findOrCreateMetadataEntryType(String typeName)
			throws MetadataIntrospectionException {
		try {
			return this.facadeTr.findOrCreateMetadataEntryType(typeName);
		} catch (Exception e) {
			throw new MetadataIntrospectionException(e);
		}
	}

	public ArtifactStoreBean getArtifactStore(String artifactStoreName) throws MetadataIntrospectionException {
		try {
			return this.facadeTr.getArtifactStore(artifactStoreName);
		} catch (Exception e) {
			throw new MetadataIntrospectionException(e);
		}
	}

	public Map<AbstractEntity, Asset> getRelatedAssets(Asset srcAsset, String relationship) throws Exception {
		Asset[] related1 = this.alerQueryTr.getRelatedAssets(srcAsset, relationship);
		return convert(related1);
	}

	private void createRelations(Entity entity) throws Exception {
		if (entity.getRelationshipsProcessed()) {
			this.logger.debug("Entity: [" + entity.getNamespace() + "::" + entity.getName() + "] ID: [" + entity.getId()
					+ "] is already processed processed for relationship.");

			return;
		}
		
		entity.setRelationshipsProcessed(true);
		
		this.logger.debug("Creating relations for entity: [" + entity.getNamespace() + "::" + entity.getName()
				+ "] ID: [" + entity.getId() + "]");
		Asset entityAsset = getMappedAsset(entity);
		if (entityAsset == null) {
			throw new MetadataIntrospectionException("No asset found for entity: [" + entity.getNamespace() + "::"
					+ entity.getName() + "] ID: [" + entity.getId() + "].");
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

	public void saveAttributes(Asset asset, Entity entity, Date publishedDate) throws Exception {
		HarvesterSettings settings = this.configManager.getSettings();
		String internalName = null;
		if ((entity instanceof AbstractEntity)) {
			internalName = ((AbstractEntity) entity).getInternalName();
		}

		boolean lProcessService = (MetadataManager.getInstance().getProcessServiceMap().get(internalName) == null)
				|| (!((String) MetadataManager.getInstance().getProcessServiceMap().get(internalName)).equals("false"));

		if (!lProcessService) {
			return;
		}

		this.logger.debug("Saving the attributes for Entity " + asset.getName());
		AttributeSaverImpl saver = new AttributeSaverImpl(this);

		List<Attribute> attributes = entity.getHarvesterProperties();
		if (attributes == null) {
			attributes = new ArrayList();
		}

		boolean scopeSet = false;
		for (Attribute attribute : attributes) {
			if (attribute.getName().equals("Scope")) {
				scopeSet = true;
			}
		}
		if (!scopeSet) {
			attributes.add(new Attribute("Scope", "global", true));
		}

		if ((internalName != null) && (internalName.trim().length() > 0)) {
			attributes.add(new Attribute("intname", internalName, true));
			this.internalNameAssetMap.put(internalName, asset);
		}

		String publishedString = DATE_FORMAT.format(publishedDate);
		attributes.add(new Attribute("Date Harvested", publishedString, true));
		attributes.add(new Attribute("Harvested by", this.user, true));

		if ((entity instanceof AbstractEntity)) {
			attributes.add(new Attribute("Short Name", ((AbstractEntity) entity).getShortName(), true));
			attributes.add(new Attribute("Namespace", ((AbstractEntity) entity).getAbstractNamespace(), true));
		} else if ((entity instanceof ArtifactEntity)) {
			attributes.add(new Attribute("Short Name", ((ArtifactEntity) entity).getShortPath(), true));
			attributes.add(new Attribute("Namespace", entity.getNamespace(), true));
		}

		attributes.add(new Attribute("Harvester Description", settings.getHarvesterDescription(), true));
		attributes.add(new Attribute("Product Name", settings.getProductName(), true));
		attributes.add(new Attribute("Product Version", settings.getProductVersion(), true));

		String deploymentStatus = this.configManager.getDeploymentStatus();
		if ((deploymentStatus == null) && (this.configManager.getDeploymentServerURL() != null)) {
			deploymentStatus = DeploymentStatus.RUN_TIME.toString();
		}

		attributes.add(new Attribute("Deployment Status", deploymentStatus, true));

		if ((entity.getSummaryXML() != null) && (entity.getSummaryXML().length() > 0)) {
			attributes.add(new Attribute("Summary XML", entity.getSummaryXML(), false));
		}

		saver.saveAttributes(asset.getID(), attributes, entity.getWsdlSummary());
	}

	public Asset saveCustomAttribute(Asset asset, String xpathField, String value) throws Exception {
		return this.facadeTr.setCustomData(asset.getID(), xpathField, value);
	}

	public Asset saveCustomAttributes(Asset asset, String[] xpathFields, String[] values) throws Exception {
		return this.facadeTr.setCustomData(asset.getID(), xpathFields, values);
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

	public OERWriterStats getStats() {
		return this.stats;
	}

	public AuthToken getAuthToken() throws Exception {
		FlashlineRegistryTr oer = ALERConnectionCache.getInstance().getFlashlineRegistry(this.uri);
		return ALERConnectionCache.getInstance().getAuthToken(this.uri, this.user, this.pass, oer);
	}

	public String getUDDIServiceKey(Asset asset) throws Exception {
		return this.facadeTr.getCustomData(asset.getID(), "uddi/service-key");
	}

	public String getUDDIServiceKey(AbstractEntity serviceEntity) {
		return serviceEntity.getCustomDataValue("uddi/service-key");
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
}
