/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rsna.isn.ctp.xds.sender.ihe;

import java.util.Date;
import org.eclipse.emf.common.util.EList;
import org.openhealthtools.ihe.common.hl7v2.CX;
import org.openhealthtools.ihe.common.hl7v2.Hl7v2Factory;
import org.openhealthtools.ihe.common.hl7v2.SourcePatientInfoType;
import org.openhealthtools.ihe.common.hl7v2.XCN;
import org.openhealthtools.ihe.common.hl7v2.XON;
import org.openhealthtools.ihe.common.hl7v2.XPN;
import org.openhealthtools.ihe.xds.metadata.AuthorType;
import org.openhealthtools.ihe.xds.metadata.CodedMetadataType;
import org.openhealthtools.ihe.xds.metadata.DocumentEntryType;
import org.openhealthtools.ihe.xds.metadata.MetadataFactory;
import org.rsna.isn.ctp.xds.sender.dicom.DicomStudy;

/**
 * Utility class for initializing XDS documents. 
 * 
 * @author Wyatt Tellis
 * @version 3.0.0
 */
class XdsDocumentInitializer
{
	private static final MetadataFactory xdsFactory = MetadataFactory.eINSTANCE;

	private static final Hl7v2Factory hl7Factory = Hl7v2Factory.eINSTANCE;

	private final DicomStudy study;

	private final String hash;

	XdsDocumentInitializer(DicomStudy study, String hash)
	{
		this.study = study;
		this.hash = hash;
	}

	private XCN getLegalAuthenticator()
	{
		XCN legalAuthenticator = hl7Factory.createXCN();

		legalAuthenticator.setFamilyName("RSNA ISN");
		legalAuthenticator.setGivenName("RSNA ISN");
		legalAuthenticator.setIdNumber("RSNA ISN");
		legalAuthenticator.setAssigningAuthorityUniversalIdType("ISO");

		return legalAuthenticator;
	}

	public AuthorType getAuthor()
	{
		XCN legalAuthenticator = getLegalAuthenticator();
		AuthorType author = xdsFactory.createAuthorType();
		author.setAuthorPerson(legalAuthenticator);



		XON institution = Hl7v2Factory.eINSTANCE.createXON();
		institution.setOrganizationName("RSNA ISN");

		EList institutions = author.getAuthorInstitution();
		institutions.add(institution);


		return author;
	}

	private SourcePatientInfoType getSrcPatInfo()
	{
		XPN rsnaPatName = hl7Factory.createXPN();
		rsnaPatName.setFamilyName("RSNA ISN");
		rsnaPatName.setGivenName("RSNA ISN");

		SourcePatientInfoType srcPatInfo = hl7Factory.createSourcePatientInfoType();
		srcPatInfo.getPatientIdentifier().add(getHash());
		srcPatInfo.getPatientName().add(rsnaPatName);

		return srcPatInfo;
	}

	private CodedMetadataType getClassCode()
	{
		CodedMetadataType classCode = xdsFactory.createCodedMetadataType();
		classCode.setCode("Imaging Exam");
		classCode.setDisplayName(XdsUtil.toInternationalString("Imaging Exam"));
		classCode.setSchemeName("RSNA ISN");

		return classCode;
	}

	private CodedMetadataType getConfidentialityCode()
	{
		CodedMetadataType confidentialityCode = xdsFactory.createCodedMetadataType();
		confidentialityCode.setCode("GRANT");
		confidentialityCode.setSchemeName("RSNA ISN");

		return confidentialityCode;
	}

	private CodedMetadataType getHealthcareFacilityTypeCode()
	{
		CodedMetadataType healthcareFacilityTypeCode = xdsFactory.createCodedMetadataType();
		healthcareFacilityTypeCode.setCode("GENERAL HOSPITAL");
		healthcareFacilityTypeCode.setDisplayName(XdsUtil.toInternationalString("GENERAL HOSPITAL"));
		healthcareFacilityTypeCode.setSchemeName("RSNA-ISN");

		return healthcareFacilityTypeCode;
	}

	private CodedMetadataType getPracticeSettingCode()
	{
		CodedMetadataType practiceSettingCode = xdsFactory.createCodedMetadataType();
		practiceSettingCode.setCode("Radiology");
		practiceSettingCode.setDisplayName(XdsUtil.toInternationalString("Radiology"));
		practiceSettingCode.setSchemeName("RSNA-ISN");

		return practiceSettingCode;
	}

	private CodedMetadataType getTypeCode()
	{
		CodedMetadataType typeCode = xdsFactory.createCodedMetadataType();
		//typeCode.setCode(study.getStudyDescription());
		//typeCode.setDisplayName(toInternationalString(study.getStudyDescription()));
		//typeCode.setSchemeName("RSNA-ISN");

		typeCode.setCode("18748-4");
		typeCode.setDisplayName(XdsUtil.toInternationalString("Diagnostic Imaging Report"));
		typeCode.setSchemeName("LOINC");

		return typeCode;
	}

	public CX getHash()
	{
		CX rsnaId = hl7Factory.createCX();
		rsnaId.setIdNumber(hash);
		rsnaId.setAssigningAuthorityUniversalId(Constants.RSNA_UNIVERSAL_ID);
		rsnaId.setAssigningAuthorityUniversalIdType(Constants.RSNA_UNIVERSAL_ID_TYPE);

		return rsnaId;
	}

	public void initDocEntry(DocumentEntryType docEntry)
	{		
		docEntry.setAuthor(getAuthor());

		docEntry.setClassCode(getClassCode());
		docEntry.getConfidentialityCode().add(getConfidentialityCode());
		docEntry.setCreationTime(XdsUtil.toGmtString(new Date()));
		docEntry.setHealthCareFacilityTypeCode(getHealthcareFacilityTypeCode());
		docEntry.setLanguageCode("en-US");

		docEntry.setPatientId(getHash());
		docEntry.setPracticeSettingCode(getPracticeSettingCode());
		docEntry.setServiceStartTime(XdsUtil.toGmtString(study.getStudyDateTime()));
		docEntry.setServiceStopTime(XdsUtil.toGmtString(study.getStudyDateTime()));
		docEntry.setSourcePatientId(getHash());
		docEntry.setSourcePatientInfo(getSrcPatInfo());
		docEntry.setTitle(XdsUtil.toInternationalString(study.getStudyDescription()));
		docEntry.setTypeCode(getTypeCode());
	}

}
