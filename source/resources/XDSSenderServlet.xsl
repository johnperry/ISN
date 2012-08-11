<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="admin"/>
<xsl:param name="update"/>
<xsl:param name="isEdgeServer"/>
<xsl:param name="destinations"/>

<xsl:template match="/Studies">
	<html>
		<head>
			<title>XDS Sender Servlet</title>
		</head>
		<body>
			<h1>XDS Sender Servlet</h1>

			<p>
				<select name="key">
					<xsl:for-each select="$destinations/Destinations/Destination">
						<option value="{@key}"><xsl:value-of select="@name"/></option>
					</xsl:for-each>
				</select>
			</p>

			<xsl:if test="Study">
				<table border="1">
					<xsl:call-template name="Headings"/>
					<xsl:apply-templates select="Study">
						<xsl:sort select="@patientID"/>
					</xsl:apply-templates>
				</table>
			</xsl:if>

			<xsl:if test="not(Study)">
				No studies have been stored for transmission.
			</xsl:if>

		</body>
	</html>
</xsl:template>

<xsl:template name="Headings">
	<tr>
		<td>Patient ID</td>
		<td>Patient Name</td>
		<td>Study UID</td>
		<td>Study Size</td>
		<td>Files Sent</td>
		<td>Study Status</td>
	</tr>
</xsl:template>

<xsl:template match="Study">
	<tr>
		<td><xsl:value-of select="@patientID"/></td>
		<td><xsl:value-of select="@patientName"/></td>
		<td><xsl:value-of select="@studyUID"/></td>
		<td><xsl:value-of select="@size"/></td>
		<td><xsl:value-of select="@objectsSent"/></td>
		<td><xsl:value-of select="@status"/></td>
	</tr>
</xsl:template>

</xsl:stylesheet>
