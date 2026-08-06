import sh.stubborn.contract.spec.Contract
Contract.make {
	request {
		method 'GET'
		url '/get'
		headers {
			contentType(applicationXml())
		}
		body """
<test>
<duck type='xtype'>123</duck>
<alpha>abc</alpha>
<list>
<elem>abc</elem>
<elem>def</elem>
<elem>ghi</elem>
</list>
<number>123</number>
<aBoolean>true</aBoolean>
<date>2017-01-01</date>
<dateTime>2017-01-01T01:23:45</dateTime>
<time>01:02:34</time>
<valueWithoutAMatcher>foo</valueWithoutAMatcher>
<valueWithTypeMatch>string</valueWithTypeMatch>
<key><complex>foo</complex></key>
</test>"""
		bodyMatchers {
			xPath('/test/duck/text()', byRegex("[0-9]{3}"))
		}
	}
	response {
		status(OK())
		body """
<test>
<duck type='xtype'>123</duck>
<alpha>abc</alpha>
<list>
<elem>abc</elem>
<elem>def</elem>
<elem>ghi</elem>
</list>
<number>123</number>
<aBoolean>true</aBoolean>
<date>2017-01-01</date>
<dateTime>2017-01-01T01:23:45</dateTime>
<time>01:02:34</time>
<valueWithoutAMatcher>foo</valueWithoutAMatcher>
<valueWithTypeMatch>string</valueWithTypeMatch>
<key><complex>foo</complex></key>
</test>"""
		bodyMatchers {
			xPath('/test/duck/xxx', byNull())
		}
	}
}
