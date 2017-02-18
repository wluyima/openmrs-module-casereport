/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * 
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.casereport;

import static org.dcm4chee.xds2.infoset.ihe.ProvideAndRegisterDocumentSetRequestType.Document;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.ihe.ProvideAndRegisterDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.rim.AssociationType1;
import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExternalIdentifierType;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.IdentifiableType;
import org.dcm4chee.xds2.infoset.rim.InternationalStringType;
import org.dcm4chee.xds2.infoset.rim.LocalizedStringType;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectListType;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectType;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.marc.everest.formatters.xml.datatypes.r1.DatatypeFormatter;
import org.marc.everest.formatters.xml.datatypes.r1.R1FormatterCompatibilityMode;
import org.marc.everest.formatters.xml.its1.XmlIts1Formatter;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.openmrs.api.context.Context;
import org.openmrs.module.casereport.api.CaseReportService;
import org.springframework.http.MediaType;

/**
 * Generates an XDS.b ProvideAndRegisterDocumentSetRequestType object from a CaseReportForm
 */
public final class ProvideAndRegisterDocGenerator {
	
	protected static final Log log = LogFactory.getLog(ProvideAndRegisterDocGenerator.class);
	
	private int idCounter;
	
	private CaseReportForm form;
	
	/**
	 * @param form the CaseReportForm from which to generate for a document set request
	 */
	ProvideAndRegisterDocGenerator(CaseReportForm form) {
		this.form = form;
	}
	
	/**
	 * Generates a ProvideAndRegisterDocumentSetRequestType object from its backing CaseReportForm
	 * object
	 * 
	 * @return ProvideAndRegisterDocumentSetRequestType object
	 * @throws Exception
	 */
	public ProvideAndRegisterDocumentSetRequestType generate() throws Exception {
		if (log.isDebugEnabled()) {
			CaseReportService crs = Context.getService(CaseReportService.class);
			CaseReport cr = crs.getCaseReportByUuid(form.getReportUuid());
			log.debug("Generating ProvideAndRegisterDocumentSetRequest for: " + cr);
		}
		
		//reset in case this method is called multiple times on the same instance
		idCounter = 0;
		
		//Create DocumentEntry/ExtrinsicObject
		ExtrinsicObjectType extrinsicObj = new ExtrinsicObjectType();
		extrinsicObj.setId(DocumentConstants.XDS_DOC_ID);
		extrinsicObj.setMimeType(MediaType.TEXT_XML.toString());
		extrinsicObj.setObjectType(XDSConstants.UUID_XDSDocumentEntry);
		extrinsicObj.setName(createName(DocumentConstants.TEXT_TITLE));
		String reportDate = DocUtil.createTS(form.getReportDate()).getValue();
		InfosetUtil.addOrOverwriteSlot(extrinsicObj, XDSConstants.SLOT_NAME_CREATION_TIME, reportDate);
		InfosetUtil.addOrOverwriteSlot(extrinsicObj, XDSConstants.SLOT_NAME_LANGUAGE_CODE, DocumentConstants.LANGUAGE_CODE);
		String patientId = String.format(DocumentConstants.PATIENT_ID_PATTERN, form.getPatientIdentifier().getValue()
		        .toString(), form.getIdentifierType().getValue().toString());
		InfosetUtil.addOrOverwriteSlot(extrinsicObj, XDSConstants.SLOT_NAME_SOURCE_PATIENT_ID, patientId);
		addClassification(extrinsicObj, DocumentConstants.LOINC_CODE_CR, DocumentConstants.CODE_SYSTEM_LOINC,
		    XDSConstants.UUID_XDSDocumentEntry_classCode, DocumentConstants.TEXT_DOCUMENT_NAME);
		
		addClassification(extrinsicObj, DocumentConstants.CODE_CONFIDENTIALITY_N,
		    DocumentConstants.CODE_SYSTEM_CONFIDENTIALITY, XDSConstants.UUID_XDSDocumentEntry_confidentialityCode,
		    DocumentConstants.TEXT_NORMAL);
		
		addClassification(extrinsicObj, DocumentConstants.CONNECTATHON_CODE_FACILITY,
		    DocumentConstants.CODE_SYSTEM_CONNECTATHON_FACILITY,
		    XDSConstants.UUID_XDSDocumentEntry_healthCareFacilityTypeCode, DocumentConstants.TEXT_FACILITY);
		
		addClassification(extrinsicObj, DocumentConstants.IHE_PCC_CODE_FORMAT,
		    DocumentConstants.CODE_SYSTEM_FORMAT_CODE_SET, XDSConstants.UUID_XDSDocumentEntry_formatCode,
		    DocumentConstants.TEXT_FORMAT);
		
		addClassification(extrinsicObj, DocumentConstants.CONNECTATHON_CODE_PRACTICE,
		    DocumentConstants.CODE_SYSTEM_CONNECTATHON_PRACTICE, XDSConstants.UUID_XDSDocumentEntry_practiceSettingCode,
		    DocumentConstants.TEXT_PRACTICE);
		
		addClassification(extrinsicObj, DocumentConstants.LOINC_CODE_TYPE_CODE_CR, DocumentConstants.CODE_SYSTEM_LOINC,
		    XDSConstants.UUID_XDSDocumentEntry_typeCode, DocumentConstants.TEXT_DOCUMENT_NAME);
		
		addExternalIdentifier(extrinsicObj, patientId, XDSConstants.UUID_XDSDocumentEntry_patientId,
		    DocumentConstants.TEXT_DOC_PATIENT_ID);
		
		//String docUniqueId = generateOIDFromUuid(UUID.fromString(form.getReportUuid()));
		String docUniqueId = generateOIDFromUuid(UUID.randomUUID());
		addExternalIdentifier(extrinsicObj, docUniqueId, XDSConstants.UUID_XDSDocumentEntry_uniqueId,
		    DocumentConstants.TEXT_DOC_UNIQUE_ID);
		
		SubmitObjectsRequest registryRequest = new SubmitObjectsRequest();
		registryRequest.setRegistryObjectList(new RegistryObjectListType());
		addObjectToRequest(registryRequest, extrinsicObj, DocumentConstants.XDS_EXTRINSIC_OBJECT);
		
		//Create RegistryPackage/SubmissionSet
		RegistryPackageType regPackage = new RegistryPackageType();
		regPackage.setId(DocumentConstants.XDS_SUBSET_ID);
		regPackage.setObjectType(DocumentConstants.XDS_SYMBOLIC_LINKS_PREFIX + DocumentConstants.XDS_REG_PACKAGE);
		regPackage.setName(createName(DocumentConstants.TEXT_TITLE));
		String dateSubmitted = DocUtil.createTS(new Date()).getValue();
		InfosetUtil.addOrOverwriteSlot(regPackage, XDSConstants.SLOT_NAME_SUBMISSION_TIME, dateSubmitted);
		addClassification(regPackage, DocumentConstants.LOINC_CODE_CR, DocumentConstants.CODE_SYSTEM_LOINC,
		    XDSConstants.UUID_XDSSubmissionSet_contentTypeCode, DocumentConstants.TEXT_DOCUMENT_NAME);
		
		addExternalIdentifier(regPackage, patientId, XDSConstants.UUID_XDSSubmissionSet_patientId,
		    DocumentConstants.TEXT_SUBSET_PATIENT_ID);
		
		String subUniqueId = generateOIDFromUuid(UUID.randomUUID());
		addExternalIdentifier(regPackage, subUniqueId, XDSConstants.UUID_XDSSubmissionSet_uniqueId,
		    DocumentConstants.TEXT_SUBSET_UNIQUE_ID);
		
		//TODO use GP for sourceId
		addExternalIdentifier(regPackage, "1.3.6.1.4.1.21367.2010.1.2", XDSConstants.UUID_XDSSubmissionSet_sourceId,
		    DocumentConstants.TEXT_SUBSET_SOURCE_ID);
		
		addObjectToRequest(registryRequest, regPackage, DocumentConstants.XDS_REG_PACKAGE);
		
		//Create the classification of the TX
		ClassificationType classification = new ClassificationType();
		classification.setId(DocumentConstants.XDS_CLASSIFICATION_ID);
		classification.setClassificationNode(XDSConstants.UUID_XDSSubmissionSet);
		classification.setClassifiedObject(DocumentConstants.XDS_SUBSET_ID);
		classification.setObjectType(DocumentConstants.XDS_SYMBOLIC_LINKS_PREFIX + DocumentConstants.XDS_CLASSIFICATION);
		addObjectToRequest(registryRequest, classification, DocumentConstants.XDS_CLASSIFICATION);
		
		//Create the association that links the DocumentEntry to the RegistryPackage
		AssociationType1 assoc = new AssociationType1();
		assoc.setId(DocumentConstants.XDS_ASSOCIATION_ID);
		assoc.setAssociationType(XDSConstants.HAS_MEMBER);
		assoc.setSourceObject(DocumentConstants.XDS_SUBSET_ID);
		assoc.setTargetObject(DocumentConstants.XDS_DOC_ID);
		addObjectToRequest(registryRequest, assoc, DocumentConstants.XDS_ASSOCIATION);
		InfosetUtil.addOrOverwriteSlot(assoc, XDSConstants.SLOT_NAME_SUBMISSIONSET_STATUS, DocumentConstants.TEXT_ORIGINAL);
		
		ProvideAndRegisterDocumentSetRequestType docRequest = new ProvideAndRegisterDocumentSetRequestType();
		docRequest.setSubmitObjectsRequest(registryRequest);
		ClinicalDocument cdaDocument = new ClinicalDocumentGenerator(form).generate();
		
		XmlIts1Formatter fmtr = new XmlIts1Formatter();
		//This instructs the XML ITS1 Formatter we want to use CDA datatypes
		fmtr.getGraphAides().add(new DatatypeFormatter(R1FormatterCompatibilityMode.ClinicalDocumentArchitecture));
		//The cda is ~8KB, might as well initialize the
		//byte array to a fairly large size
		ByteArrayOutputStream cdaOutput = new ByteArrayOutputStream(8192);
		fmtr.graph(cdaOutput, cdaDocument);
		
		Document document = new Document();
		document.setId(DocumentConstants.XDS_DOC_ID);
		document.setValue(cdaOutput.toByteArray());
		docRequest.getDocument().add(document);
		
		return docRequest;
	}
	
	/**
	 * Adds an IdentifiableType object to the specified SubmitObjectsRequest
	 * 
	 * @param registryRequest the SubmitObjectsRequest object
	 * @param object IdentifiableType object
	 * @param objectName the name to use for the element
	 */
	private <T extends IdentifiableType> void addObjectToRequest(SubmitObjectsRequest registryRequest, T object,
	                                                             String objectName) {
		QName qName = new QName(DocumentConstants.XDS_NAMESPACE_URI, objectName);
		JAXBElement<T> element = new JAXBElement<>(qName, (Class<T>) object.getClass(), object);
		registryRequest.getRegistryObjectList().getIdentifiable().add(element);
	}
	
	/**
	 * Adds a classification to the specified RegistryObjectType.
	 * 
	 * @param classifiedObj the object to which to add the classified object
	 * @param code
	 * @param codeSystem the OID of teh coding scheme the code belongs
	 * @param scheme the XDS.b classification scheme URN
	 * @param localizedString the internationalized text label for the scheme
	 * @throws JAXBException
	 */
	private void addClassification(RegistryObjectType classifiedObj, String code, String codeSystem, String scheme,
	                               String localizedString) throws JAXBException {
		ClassificationType classification = new ClassificationType();
		classification.setId("id_" + idCounter++);
		classification.setClassifiedObject(classifiedObj.getId());
		classification.setClassificationScheme(scheme);
		classification.setNodeRepresentation(code);
		if (StringUtils.isNotBlank(localizedString)) {
			classification.setName(createName(localizedString));
		}
		InfosetUtil.addOrOverwriteSlot(classification, DocumentConstants.XDS_SLOT_CODING_SCHEME, codeSystem);
		classifiedObj.getClassification().add(classification);
	}
	
	/**
	 * Adds an externalIdentifier to the specified RegistryObjectType.
	 *
	 * @param classifiedObj the object to which to add the external identifier object
	 * @param value the identifier
	 * @param scheme the XDS.b identification scheme URN
	 * @param localizedString the internationalized text label for the scheme
	 */
	private void addExternalIdentifier(final RegistryObjectType classifiedObj, String value, final String scheme,
	                                   final String localizedString) {
		
		ExternalIdentifierType extId = new ExternalIdentifierType();
		extId.setRegistryObject(classifiedObj.getId());
		extId.setId("id_" + idCounter++);
		extId.setValue(value);
		extId.setIdentificationScheme(scheme);
		if (StringUtils.isNotBlank(localizedString)) {
			extId.setName(createName(localizedString));
		}
		classifiedObj.getExternalIdentifier().add(extId);
	}
	
	/**
	 * Creates an InternationalStringType for the specified name
	 * 
	 * @param name
	 * @return the InternationalStringType object
	 */
	private InternationalStringType createName(String name) {
		InternationalStringType iName = new InternationalStringType();
		LocalizedStringType localizedStringType = new LocalizedStringType();
		localizedStringType.setValue(name);
		iName.getLocalizedString().add(localizedStringType);
		return iName;
	}
	
	/**
	 * Generates and returns an OID from the specified uuid object
	 * 
	 * @param uuid the uuid object
	 * @return the generated OID
	 */
	private String generateOIDFromUuid(UUID uuid) {
		ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		return DocumentConstants.OID_PREFIX + new BigInteger(bb.array()).abs().toString();
	}
}
