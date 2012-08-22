<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:template match="/">
	<html>
		<head>
			<link rel="Stylesheet" type="text/css" media="all" href="/XDSServlet.css"></link>
			<title>Retrieve Studies</title>
		</head>
		<body>
			<h1>Retrieve Studies from the Clearinghouse</h1>

			<form id="formID" action="" method="POST" accept-charset="UTF-8" >

				<xsl:if test="Study">
					<p>
						<table border="1">
							<tr>
								<td>Patient:</td>
								<td><input name="usertoken" type="text"/></td>
							</tr>
							<tr>
								<td>Date of Birth (YYYYMMDD):</td>
								<td><input name="dateofbirth" type="text"/></td>
							</tr>
							<tr>
								<td>Password:</td>
								<td><input name="password" type="text"/></td>
							</tr>
						</table>
					</p>
					<p>
						<input type="submit" class="button" value="Query"/>
					</p>
				</xsl:if>
			</form>
		</body>
	</html>
</xsl:template>

</xsl:stylesheet>