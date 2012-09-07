<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="context"/>
<xsl:param name="sentStudies"/>
<xsl:param name="destinations"/>

<xsl:template match="/Studies">
	<html>
		<head>
			<link rel="Stylesheet" type="text/css" media="all" href="/XDSServlet.css"></link>
			<script>var context = "<xsl:value-of select="$context"/>";</script>
			<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/JSAJAX.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/XDSSenderServlet.js">;</script>
			<title>Send Studies</title>
		</head>
		<body>
			<h1>Send Studies to the Clearinghouse</h1>

			<form id="formID" action="" method="POST" accept-charset="UTF-8" >

				<xsl:if test="Study">
					<p>
						Select a destination:
						<select name="key">
							<option value=""></option>
							<xsl:for-each select="$destinations/Destinations/Destination">
								<option value="{@key}"><xsl:value-of select="@name"/></option>
							</xsl:for-each>
							<option value="0">Trash</option>
						</select>
					</p>
					<p>
						<table border="1">
							<xsl:call-template name="StudyHeadings"/>
							<xsl:for-each select="Study">
								<xsl:sort select="@patientID"/>
								<tr>
									<td class="center"><input type="checkbox" name="study" value="{@studyUID}"/></td>
									<td><xsl:value-of select="@patientID"/></td>
									<td><xsl:value-of select="@patientName"/></td>
									<td title="UID: {@studyUID}"><xsl:value-of select="@studyDate"/></td>
									<xsl:call-template name="ModalityBodypart"/>
									<td class="right"><xsl:value-of select="@size"/></td>
									<td class="center"><xsl:value-of select="@status"/></td>
								</tr>
							</xsl:for-each>
						</table>
					</p>
					<p>
						<input type="submit" class="button" value="Send"/>
					</p>
				</xsl:if>
			</form>

			<xsl:if test="not(Study)">
				<p class="bold">No studies are available for transmission.</p>
			</xsl:if>

			<xsl:if test="$sentStudies/Studies/Study">
				<hr/>
				<h2>Sent Studies</h2>
				<p>
					<table id="SentStudiesTable" border="1">
						<xsl:call-template name="SentStudyHeadings"/>
						<xsl:for-each select="$sentStudies/Studies/Study">
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
				</p>
			</xsl:if>

			<xsl:call-template name="footer"/>
		</body>
	</html>
</xsl:template>

<xsl:template name="StudyHeadings">
	<tr>
		<th>Select</th>
		<th>Patient ID</th>
		<th>Patient Name</th>
		<th>Study Date</th>
		<th>Modality</th>
		<th>Study Size</th>
		<th>Status</th>
	</tr>
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

<xsl:template name="footer">
	<hr/>
	<p class="left">
		Study status values:
		<ul>
			<li>OPEN: An object has been received for the study in the last five minutes.</li>
			<li>COMPLETE: An object has not been received for the study in the last five minutes.</li>
			<li>QUEUED: The study has been entered into the export queue, but transmission has not yet started.</li>
			<li>INTRANSIT: The study is actively being transmitted.</li>
			<li>SUCCESS: The transmission completed and was successful.</li>
			<li>FAILED: The transmission failed.</li>
		</ul>
	</p>
</xsl:template>

</xsl:stylesheet>
