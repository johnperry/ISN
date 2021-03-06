<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="today"/>
<xsl:param name="email"/>
<xsl:param name="dateofbirth"/>
<xsl:param name="accesscode"/>
<xsl:param name="key"/>

<xsl:template match="/">
	<html>
		<head>
			<link rel="Stylesheet" type="text/css" media="all" href="/BaseStyles.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/XDSToolServlet.css"></link>
			<title>ISN Tool</title>
		</head>
		<body>

			<center>

			<h1>ISN Key Tool</h1>

			<div class="form">
				<form id="form3" action="" method="POST" accept-charset="UTF-8" >

					<p class="center">
						Use this section to generate keys based on the three standard parameters.
						<br/>
						(For site-to-site keys, leave the Email parameter blank.)
					</p>

					<p class="center">
						<table border="1">
							<tr>
								<td>Email:</td>
								<td><input name="email" type="text"/></td>
							</tr>
							<tr>
								<td title="Date of Birth">DOB (YYYYMMDD):</td>
								<td><input name="dateofbirth" type="text" value="{$today}"/></td>
							</tr>
							<tr>
								<td>Access Code:</td>
								<td><input name="accesscode" type="text"/></td>
							</tr>
						</table>
					</p>
					<p class="center">
						<input type="submit" class="button" value="Get Key"/>
					</p>
				</form>
			</div>

			<div class="form">
				<form id="form0" action="" method="POST" accept-charset="UTF-8" >

					<p class="center">
						Use this section to generate keys automatically for use in clinical trials.
					</p>

					<p class="center">
						<input type="submit" class="button" value="Get Key"/>
					</p>
				</form>
			</div>

			<xsl:if test="$dateofbirth">
				<br/>
				<h1>Result</h1>
				<p class="center">
					<table border="1">
						<tr>
							<td>Email:</td>
							<td><xsl:value-of select="$email"/></td>
						</tr>
						<tr>
							<td>Date of Birth:</td>
							<td><xsl:value-of select="$dateofbirth"/></td>
						</tr>
						<tr>
							<td>Access Code:</td>
							<td><xsl:value-of select="$accesscode"/></td>
						</tr>
						<tr>
							<td>Key:</td>
							<td><xsl:value-of select="$key"/></td>
						</tr>
					</table>
				</p>
			</xsl:if>

			<xsl:if test="$key and not($dateofbirth)">
				<br/>
				<h1>Result</h1>
				<p class="center">
					<table border="1">
						<tr>
							<td>Key:</td>
							<td><xsl:value-of select="$key"/></td>
						</tr>
					</table>
				</p>
			</xsl:if>

			</center>

		</body>
	</html>
</xsl:template>

</xsl:stylesheet>