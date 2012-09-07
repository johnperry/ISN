<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:template match="/Studies">
	<table id="SentStudiesTable" border="1">
		<xsl:call-template name="SentStudyHeadings"/>
		<xsl:for-each select="Study">
			<xsl:sort select="@patientID"/>
			<tr>
				<td><xsl:value-of select="@patientID"/></td>
				<td><xsl:value-of select="@patientName"/></td>
				<td title="UID: {@studyUID}"><xsl:value-of select="@studyDate"/></td>
				<xsl:call-template name="ModalityBodypart"/>
				<td class="right"><xsl:value-of select="@size"/></td>
				<td class="right"><xsl:value-of select="@objectsSent"/></td>
				<td>
					<xsl:variable name="dn" select="normalize-space(@destinationName)"/>
					<xsl:if test="$dn"><xsl:value-of select="@destinationName"/></xsl:if>
					<xsl:if test="not($dn)">&#160;</xsl:if>
				</td>
				<td class="center"><xsl:value-of select="@status"/></td>
			</tr>
		</xsl:for-each>
	</table>
</xsl:template>

<xsl:template name="SentStudyHeadings">
	<tr>
		<th>Patient ID</th>
		<th>Patient Name</th>
		<th>Study Date</th>
		<th>Modality</th>
		<th>Study Size</th>
		<th>Files Sent</th>
		<th>Destination</th>
		<th>Status</th>
	</tr>
</xsl:template>

<xsl:template name="ModalityBodypart">
	<xsl:variable name="modality" select="normalize-space(@modality)"/>
	<xsl:variable name="bodypart" select="normalize-space(@bodypart)"/>
	<td class="center">
		<xsl:if test="$modality)">
			<xsl:value-of select="$modality"/>
			<xsl:if test="$bodypart">
				<xsl:text> - </xsl:text>
				<xsl:value-of select="$bodypart"/>
			</xsl:if>
		</xsl:if>
		<xsl:if test="not($modality)">&#160;</xsl:if>
	</td>
</xsl:template>

</xsl:stylesheet>
