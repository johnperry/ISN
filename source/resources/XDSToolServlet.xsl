<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="today"/>
<xsl:param name="token"/>
<xsl:param name="dob"/>
<xsl:param name="pw"/>
<xsl:param name="key"/>
<xsl:param name="tokens"/>

<xsl:template match="/">
	<html>
		<head>
			<link rel="Stylesheet" type="text/css" media="all" href="/BaseStyles.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/XDSToolServlet.css"></link>
			<title>ISN Tool</title>
		</head>
		<body>

			<xsl:if test="$tokens">
				<div class="tokens">
					<p class="heading">Random<br/><u>Tokens</u></p>
					<p class="values"><xsl:value-of select="$tokens"/></p>
				</div>
			</xsl:if>

			<center>

			<h1>ISN Tool</h1>

			<form id="formID" action="" method="POST" accept-charset="UTF-8" >

				<p class="center">
					<table border="1">
						<tr>
							<td>Token/Exam ID:</td>
							<td><input name="usertoken" type="text"/></td>
						</tr>
						<tr>
							<td>Password/PIN:</td>
							<td><input name="password" type="text"/></td>
						</tr>
						<tr>
							<td title="Date of Birth">DOB (YYYYMMDD):</td>
							<td><input name="dateofbirth" type="text" value="{$today}"/></td>
						</tr>
					</table>
				</p>
				<p class="center">
					<input type="submit" class="button" value="Get Key"/>
				</p>

				<xsl:if test="$token">
					<br/>
					<h1>Results</h1>
					<p class="center">
						<table border="1">
							<tr>
								<td>Token/Exam ID:</td>
								<td><xsl:value-of select="$token"/></td>
							</tr>
							<tr>
								<td>Password/PIN:</td>
								<td><xsl:value-of select="$pw"/></td>
							</tr>
							<tr>
								<td>Date of Birth:</td>
								<td><xsl:value-of select="$dob"/></td>
							</tr>
							<tr>
								<td>Key:</td>
								<td><xsl:value-of select="$key"/></td>
							</tr>
						</table>
					</p>
				</xsl:if>

			</form>

			</center>

		</body>
	</html>
</xsl:template>

</xsl:stylesheet>