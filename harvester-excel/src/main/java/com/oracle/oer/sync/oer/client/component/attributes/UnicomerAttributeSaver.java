/**
 * 
 */
package com.oracle.oer.sync.oer.client.component.attributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.xmlbeans.XmlException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;

import com.bea.aler.util.xml.JDOMUtil;
import com.bea.aler.util.xml.JDOMVisitor;
import com.flashline.registry.openapi.entity.MetadataEntrySummary;
import com.flashline.registry.openapi.entity.MetadataEntryTypeSummary;
import com.oracle.bpelModel.WsdlSummaryDocument;
import com.oracle.bpelModel.WsdlSummaryDocument.WsdlSummary;
import com.oracle.oer.sync.framework.MetadataIntrospectionException;
import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.oracle.oer.sync.oer.client.component.attributes.CustomDataEntity.CustomDataProperty;
import com.unicomer.oer.harvester.writer.UnicomerOERWriter;

/**
 * @author carlosj_rodriguez
 *
 */
public class UnicomerAttributeSaver implements AttributeSaver {
	private UnicomerOERWriter writer;
	private MetadataLogger logger = MetadataManager.getLogger(UnicomerAttributeSaver.class);
	
	public UnicomerAttributeSaver() {

	}

	public UnicomerAttributeSaver(UnicomerOERWriter writer) {
		this.writer = writer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.oracle.oer.sync.oer.client.component.attributes.AttributeSaver#
	 * readAttributes(long)
	 */
	public List<Attribute> readAttributes(long assetID) throws MetadataIntrospectionException {
		List<Attribute> results = new ArrayList<Attribute>();
		Map<MetadataEntrySummary, String> entries = this.writer.assetMetadataRead(assetID);

		Entry<MetadataEntrySummary, String> entry = findMetadataEntry(entries, "internal.introspector.store",
				"introspectorStore");
		if (entry == null) {
			return results;
		}
		String text = entry.getValue();

		CustomDataEntity cde = new CustomDataEntity();
		cde.setContent(text);

		Map<String, CustomDataProperty> props = cde.getPropertiesMap();
		for (Iterator<String> it = props.keySet().iterator(); it.hasNext();) {
			String key = it.next();
			CustomDataEntity.CustomDataProperty prop = props.get(key);

			Object value = prop.getValue();
			if (value != null) {
				if ((value instanceof List))
					results.add(new Attribute(key, (List<String>) value, prop.isViewable()));
				else {
					results.add(new Attribute(key, value.toString(), prop.isViewable()));
				}
			}
		}

		return results;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.oracle.oer.sync.oer.client.component.attributes.AttributeSaver#
	 * readWSDLSummary(long)
	 */
	public WsdlSummary readWSDLSummary(long assetID) throws MetadataIntrospectionException {
		Map<MetadataEntrySummary, String> entries = this.writer.assetMetadataRead(assetID);

		for (Iterator<Entry<MetadataEntrySummary, String>> it = entries.entrySet().iterator(); it.hasNext();) {
			Map.Entry<MetadataEntrySummary, String> entry = it.next();
			MetadataEntrySummary summary = entry.getKey();
			if ("internal.wsdl.summary".equals(summary.getEntryType())) {
				String content = entry.getValue();
				WsdlSummaryDocument.WsdlSummary wsdlSummary = unwrapWsdlSummary(content);
				return wsdlSummary;
			}
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.oracle.oer.sync.oer.client.component.attributes.AttributeSaver#
	 * saveAttributes(long, java.util.List,
	 * com.oracle.bpelModel.WsdlSummaryDocument.WsdlSummary)
	 */
	public void saveAttributes(long assetID, List<Attribute> attributes, WsdlSummary wsdlSummary)
			throws MetadataIntrospectionException {
		Map<MetadataEntrySummary, String> entries = null;
		boolean hasEntry = hasEntrySummary(assetID, "internal.introspector.store", "introspectorStore");
		boolean hasSummary = false;
		String summaryXml = null;

		for (int i = 0; i < attributes.size(); i++) {
			Attribute attr = attributes.get(i);
			if (attr.getName().equals("Summary XML")) {
				hasSummary = true;
				summaryXml = attr.getValue();
				attributes.remove(attr);
			}

		}

		if (!hasEntry) {
			findOrCreateMetadataEntryType("internal.introspector.store");
			createMetadataEntry(attributes, assetID);
		} else {
			entries = this.writer.assetMetadataRead(assetID);
			Entry<MetadataEntrySummary, String> entry = findMetadataEntry(entries, "internal.introspector.store",
					"introspectorStore");
			updateMetadataEntry(entry, attributes, assetID);
		}

		if ((hasSummary) && (summaryXml != null)) {
			hasEntry = hasEntrySummary(assetID, "internal.wsdl.summary", "internal.wsdl.summary");

			if (!hasEntry) {
				createSummary("internal.wsdl.summary", summaryXml, assetID);
			} else {
				if (entries == null) {
					entries = this.writer.assetMetadataRead(assetID);
				}
				Entry<MetadataEntrySummary, String> entry = findMetadataEntry(entries, "internal.wsdl.summary",
						"internal.wsdl.summary");
				updateSummary(entry, summaryXml);
			}
		} else if (wsdlSummary != null) {
			hasEntry = hasEntrySummary(assetID, "internal.wsdl.summary", "internal.wsdl.summary");

			if (!hasEntry) {
				createWSDLSummary("internal.wsdl.summary", wsdlSummary, assetID);
			} else {
				if (entries == null) {
					entries = this.writer.assetMetadataRead(assetID);
				}
				Entry<MetadataEntrySummary, String> entry = findMetadataEntry(entries, "internal.wsdl.summary",
						"internal.wsdl.summary");
				updateWSDLSummary(entry, wsdlSummary);
			}
		}
	}

	// Internal Methods
	private void findOrCreateMetadataEntryType(String entryTypeIntrospector) throws MetadataIntrospectionException {
		MetadataEntryTypeSummary met = this.writer.findOrCreateMetadataEntryType(entryTypeIntrospector);

		MetadataEntryTypes.setInternalIntrospectorStoreID(met.getID());
	}

	private boolean hasEntrySummary(long assetID, String entryType, String entryName)
			throws MetadataIntrospectionException {
		MetadataEntrySummary[] summaries = this.writer.assetMetadataReadSummary(assetID);

		if (summaries == null) {
			return false;
		}

		for (MetadataEntrySummary summary : summaries) {
			if ((entryType.equals(summary.getEntryType())) && (entryName.equals(summary.getName()))) {
				return true;
			}
		}

		return false;
	}

	private Map.Entry<MetadataEntrySummary, String> findMetadataEntry(Map<MetadataEntrySummary, String> entries,
			String entryType, String name) throws MetadataIntrospectionException {
		if (entries == null) {
			return null;
		}

		Map.Entry<MetadataEntrySummary, String> result = null;
		int numFound = 0;
		for (Iterator<Entry<MetadataEntrySummary, String>> it = entries.entrySet().iterator(); it.hasNext();) {
			Map.Entry<MetadataEntrySummary, String> entry = it.next();
			MetadataEntrySummary summary = entry.getKey();
			if ((entryType.equals(summary.getEntryType())) && (name.equals(summary.getName()))) {
				numFound++;
				result = entry;
			}
		}

		if (numFound > 1) {
			this.logger.warn(
					"Found more than one " + entryType + " MetadataEntry for asset.  May cause unexpected results.");
		}
		return result;
	}

	private void createMetadataEntry(List<Attribute> attributes, long assetID) throws MetadataIntrospectionException {
		CustomDataEntity cde = new CustomDataEntity();
		for (Iterator<Attribute> it = attributes.iterator(); it.hasNext();) {
			Attribute attr = it.next();

			if (attr.getType().equals(String.class)) {
				String key = attr.getName();
				Collection<String> values = attr.getValues();
				boolean visibile = attr.isVisible();

				if (values.size() == 1)
					cde.addProperty(key, values.iterator().next(), visibile);
				else {
					cde.addProperty(key, values, visibile);
				}
			}
		}

		cde.setAssetID(assetID);

		String content = getStringContent(cde);

		if (content != null) {
			MetadataEntrySummary mes = buildMES(assetID, "internal.introspector.store", "introspectorStore");
			this.writer.assetMetadataCreate(mes, content);
		}
	}

	private MetadataEntrySummary buildMES(long assetID, String entryType, String entryName) {
		MetadataEntrySummary mEntry = null;
		mEntry = new MetadataEntrySummary();
		mEntry.setID(0L);
		mEntry.setEntityTypeDef("asset");
		mEntry.setEntityID(assetID);
		mEntry.setEntryType(entryType);
		mEntry.setName(entryName);
		mEntry.setSFID(null);
		return mEntry;
	}

	private void updateMetadataEntry(Entry<MetadataEntrySummary, String> entry, List<Attribute> attributes,
			long assetID) throws MetadataIntrospectionException {
		CustomDataEntity cde = new CustomDataEntity();
		String text = (String) entry.getValue();
		cde.setContent(text);

		for (Iterator<Attribute> it = attributes.iterator(); it.hasNext();) {
			Attribute attr = it.next();

			if (attr.getType().equals(String.class)) {
				String key = attr.getName();
				Collection<String> values = attr.getValues();
				boolean visibile = attr.isVisible();

				if ((values == null) || (values.size() == 0)) {
					cde.removeProperty(key);
				} else if (values.size() == 1) {
					String value = values.iterator().next();
					if (value == null)
						cde.removeProperty(key);
					else
						cde.addProperty(key, value, visibile);
				} else {
					cde.addProperty(key, values, visibile);
				}
			}

		}

		String newContent = getStringContent(cde);
		if (!newContent.equals(text)) {
			MetadataEntrySummary mes = (MetadataEntrySummary) entry.getKey();
			this.writer.assetMetadataUpdate(mes, newContent);
		}
	}

	private String getStringContent(CustomDataEntity cde) throws MetadataIntrospectionException {
		Document jdomDoc = cde.toXML();
		Element data = jdomDoc.getRootElement().getChild("data");
		if (data != null) {
			Document doc = new Document();
			doc.addContent(data.cloneContent());

			return JDOMUtil.getXMLFragment(doc);
		}
		return null;
	}

	private void createSummary(String key, String value, long assetID) throws MetadataIntrospectionException {
		if (value != null) {
			MetadataEntrySummary mes = buildMES(assetID, "internal.wsdl.summary", key);
			this.writer.assetMetadataCreate(mes, value);
		}
	}

	private void updateSummary(Entry<MetadataEntrySummary, String> entry, String value)
			throws MetadataIntrospectionException {
		String oldContent = (String) entry.getValue();
		if (!value.equals(oldContent)) {
			MetadataEntrySummary mes = (MetadataEntrySummary) entry.getKey();
			this.writer.assetMetadataUpdate(mes, value);
		}
	}

	private void createWSDLSummary(String key, WsdlSummaryDocument.WsdlSummary value, long assetID)
			throws MetadataIntrospectionException {
		String wrappedContent = wrapWsdlSummary(value);

		if (wrappedContent != null) {
			MetadataEntrySummary mes = buildMES(assetID, "internal.wsdl.summary", key);
			this.writer.assetMetadataCreate(mes, wrappedContent);
		}
	}

	private void updateWSDLSummary(Entry<MetadataEntrySummary, String> entry, WsdlSummaryDocument.WsdlSummary value)
			throws MetadataIntrospectionException {
		String wrappedContent = wrapWsdlSummary(value);

		String oldContent = (String) entry.getValue();
		if (!wrappedContent.equals(oldContent)) {
			MetadataEntrySummary mes = (MetadataEntrySummary) entry.getKey();
			this.writer.assetMetadataUpdate(mes, wrappedContent);
		}
	}

	private String wrapWsdlSummary(WsdlSummaryDocument.WsdlSummary value) throws MetadataIntrospectionException {
		try {
			WsdlSummaryDocument doc = WsdlSummaryDocument.Factory.newInstance();
			doc.setWsdlSummary(value);
			String content = doc.toString();
			Element contentElement = JDOMUtil.createDocument(content).detachRootElement();

			JDOMVisitor visitor = new NamespaceSettingVisitor(null);
			visitor.visit(contentElement);

			Element customData = new Element("custom-data");
			customData.addContent(contentElement);

			return JDOMUtil.getXMLFragment(customData);
		} catch (IOException e) {
			throw new MetadataIntrospectionException(e);
		} catch (JDOMException e) {
			throw new MetadataIntrospectionException(e);
		}
	}

	private static WsdlSummaryDocument.WsdlSummary unwrapWsdlSummary(String content)
			throws MetadataIntrospectionException {
		try {
			Element root = JDOMUtil.createDocument(content).detachRootElement();
			Element summary = (Element) root.getChildren().get(0);

			JDOMVisitor visitor = new NamespaceSettingVisitor(Namespace.getNamespace("http://oracle.com/BPELModel"));
			visitor.visit(summary);

			String summaryContent = JDOMUtil.getXMLFragment(summary);

			WsdlSummaryDocument document = WsdlSummaryDocument.Factory.parse(summaryContent);
			return document.getWsdlSummary();
		} catch (JDOMException e) {
			throw new MetadataIntrospectionException(e);
		} catch (IOException e) {
			throw new MetadataIntrospectionException(e);
		} catch (XmlException e) {
			throw new MetadataIntrospectionException(e);
		}
	}
}
