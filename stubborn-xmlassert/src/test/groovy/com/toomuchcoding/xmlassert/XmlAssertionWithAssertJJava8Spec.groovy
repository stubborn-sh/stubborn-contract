package com.toomuchcoding.xmlassert

import groovy.xml.MarkupBuilder
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static com.toomuchcoding.xmlassert.BDDXmlAssertions.then
import static com.toomuchcoding.xmlassert.XmlAsString.asXml
import static com.toomuchcoding.xmlassert.XmlAssertions.assertThat

/**
 * @author Marcin Grzejszczak
 */
class XmlAssertionWithAssertJJava8Spec extends Specification {

    @Shared String xml1 = '''<?xml version="1.0" encoding="UTF-8" ?>
    <some>
        <nested>
            <json>with &quot;val&apos;ue</json>
            <anothervalue>4</anothervalue>
            <withattr id="a" id2="b">foo</withattr>
            <withlist>
                <name>name1</name>
            </withlist>
            <withlist>
                <name>name2</name>
            </withlist>
            <withlist>
                8
            </withlist>
            <withlist>
                <name id="10" surname="kowalski">name3</name>
            </withlist>
        </nested>
    </some>'''

    @Unroll
    def 'should convert an xml with a map as root to a map of path to value '() {
        expect:
        assertThat(new XmlAsString(xml1)).node("some").node("nested").node("anothervalue").isEqualTo(4)
        assertThat(new XmlAsString(xml1)).node("some").node("nested").node("anothervalue")
        assertThat(new XmlAsString(xml1)).node("some").node("nested").node("withattr").withAttribute("id", "a").withAttribute("id2", "b")
        assertThat(new XmlAsString(xml1)).node("some").node("nested").node("withattr").isEqualTo("foo").withAttribute("id", "a").withAttribute("id2", "b")
        then(new XmlAsString(xml1)).node("some").node("nested").node("anothervalue").isEqualTo(4)
        then(new XmlAsString(xml1)).node("some").node("nested").node("anothervalue").isEqualTo(4)
        assertThat(new XmlAsString(xml1)).node("some").node("nested").array("withlist").contains("name").isEqualTo("name1")
        assertThat(new XmlAsString(xml1)).node("some").node("nested").array("withlist").contains("name").isEqualTo("name2")
        assertThat(new XmlAsString(xml1)).node("some").node("nested").array("withlist").contains("name").isEqualTo("name3").withAttribute("id", "10").withAttribute("surname", "kowalski")
        assertThat(new XmlAsString(xml1)).node("some").node("nested").array("withlist").isEqualTo(8)
        assertThat(new XmlAsString(xml1)).node("some").node("nested").node("json").isEqualTo("with \"val'ue")
        assertThat(asXml(xml1)).node("some", "nested", "json").isEqualTo("with \"val'ue")
    }

    @Shared String xml2 =  '''<?xml version="1.0" encoding="UTF-8" ?>
    <root>
        <property1>a</property1>
        <property2>b</property2>
    </root>
'''

    @Unroll
    def "should generate assertions for simple response body"() {
        expect:
        assertThat(asXml(xml2)).node("root").node("property1").isEqualTo("a")
        assertThat(asXml(xml2)).node("root").node("property2").isEqualTo("b")
    }

    @Shared String xml3 =  '''<?xml version="1.0" encoding="UTF-8" ?>
    <root>
        <property1>true</property1>
        <property2 />
        <property3>false</property3>
        <property4>5</property4>
    </root>
'''

    @Unroll
    def "should generate assertions for null and boolean values"() {
        expect:
        assertThat(asXml(xml3)).node("root").node("property1").isEqualTo("true")
        assertThat(asXml(xml3)).node("root").node("property2").isNull()
        assertThat(asXml(xml3)).node("root").node("property3").isEqualTo(false)
        assertThat(asXml(xml3)).node("root").node("property4").isEqualTo(5)
    }

    @Shared StringWriter xml4 = new StringWriter()
    @Shared def root4 = new MarkupBuilder(xml4).root {
        property1('a')
        property2 {
            a('sth')
            b('sthElse')
        }
    }

    @Unroll
    def "should generate assertions for simple response body constructed from map with a list"() {
        expect:
        assertThat(asXml(xml4.toString())).node("root").node("property1").isEqualTo("a")
        assertThat(asXml(xml4.toString())).node("root").array("property2").contains("a").isEqualTo("sth")
        assertThat(asXml(xml4.toString())).node("root").array("property2").contains("b").isEqualTo("sthElse")
    }

    @Shared String xml7 =  '''<?xml version="1.0" encoding="UTF-8" ?>
    <root>
        <property1>
            <property2>test1</property2>
        </property1>
        <property1>
            <property3>test2</property3>
        </property1>
    </root>
'''

    @Unroll
    def "should generate assertions for array inside response body element"() {
        expect:
        assertThat(asXml(xml7)).node("root").array("property1").contains("property2").isEqualTo("test1")
        assertThat(asXml(xml7)).node("root").array("property1").contains("property3").isEqualTo("test2")
    }

    @Shared String xml8 =  """<?xml version="1.0" encoding="UTF-8" ?>
    <root>
        <property1>a</property1>
        <property2>
            <property3>b</property3>
        </property2>
    </root>
"""

    def "should generate assertions for nested objects in response body"() {
        expect:
        assertThat(asXml(xml8)).node("root").node("property2").node("property3").isEqualTo("b")
        assertThat(asXml(xml8)).node("root").node("property1").isEqualTo("a")
    }

    @Shared StringWriter xml9 = new StringWriter()
    @Shared def root9 = new MarkupBuilder(xml9).root {
        property1('a')
        property2(123)
    }

    @Unroll
    def "should generate regex assertions for map objects in response body"() {
        expect:
        assertThat(asXml(xml9.toString())).node("root").node("property2").matches("[0-9]{3}")
        assertThat(asXml(xml9.toString())).node("root").node("property1").isEqualTo("a")
    }

    def "should generate escaped regex assertions for string objects in response body"() {
        given:
        StringWriter sw = new StringWriter()
        def root = new MarkupBuilder(sw).root {
            property2(123123)
        }
        expect:
        assertThat(asXml(sw.toString())).node("root").node("property2").matches("\\d+")
    }

    @Shared StringWriter xml10 = new StringWriter()
    @Shared def root10 = new MarkupBuilder(xml10).root {
        errors {
            property('bank_account_number')
            message('incorrect_format')
        }
    }

    @Unroll
    def "should work with more complex stuff and xpaths"() {
        expect:
        assertThat(asXml(xml10.toString())).node("root").array("errors").contains("property").isEqualTo("bank_account_number")
        assertThat(asXml(xml10.toString())).node("root").array("errors").contains("message").isEqualTo("incorrect_format")
    }

    @Shared String xml11 = '''<?xml version="1.0" encoding="UTF-8" ?>
    <root>
        <place>
            <bounding_box>
                <coordinates>-77.119759</coordinates>
                <coordinates>38.995548</coordinates>
                <coordinates>-76.909393</coordinates>
                <coordinates>38.791645</coordinates>
            </bounding_box>
        </place>
    </root>
'''

    @Unroll
    def "should manage to parse a double array"() {
        expect:
        assertThat(asXml(xml11)).node("root").node("place").node("bounding_box").array("coordinates").isEqualTo(38.995548)
        assertThat(asXml(xml11)).node("root").node("place").node("bounding_box").array("coordinates").isEqualTo(-77.119759)
        assertThat(asXml(xml11)).node("root").node("place").node("bounding_box").array("coordinates").isEqualTo(-76.909393)
        assertThat(asXml(xml11)).node("root").node("place").node("bounding_box").array("coordinates").isEqualTo(38.791645)

    }

    def "should run XPath when provided manually"() {
        given:
        String xml = """<?xml version="1.0" encoding="UTF-8" ?>
        <root>
            <property1>a</property1>
            <property2>
                <property3>b</property3>
            </property2>
        </root>
    """
        and:
        String xPath = '''/root/property2[property3='b']'''
        expect:
        assertThat(asXml(xml)).matchesXPath(xPath)
    }

    def "should throw exception when XPath is not matched"() {
        given:
        String xml = """<?xml version="1.0" encoding="UTF-8" ?>
        <root>
            <property1>a</property1>
            <property2>
                <property3>b</property3>
            </property2>
        </root>
    """
        and:
        String xPath = '''/root/property2[property3='non-existing']'''
        when:
        assertThat(asXml(xml)).matchesXPath(xPath)
        then:
        AssertionError illegalStateException = thrown(AssertionError)
        illegalStateException.message.contains("Expected XML [")
        illegalStateException.message.contains("to match XPath")
    }

    def "should generate escaped regex assertions for text with regular expression values"() {
        given:
        // '"<>[]()
        String xml = """<?xml version="1.0" encoding="UTF-8" ?>
            <root>
                <property1>&apos;&quot;&lt;&gt;[]()</property1>
            </root>"""
        expect:
        assertThat(asXml(xml)).node("root").node("property1").matches('\'"<>\\[\\]\\(\\)')
    }

    def "should escape regular expression properly"() {
        given:
        String xml = """<?xml version="1.0" encoding="UTF-8" ?>
                <root>
                    <path>/api/12</path>
                    <correlationId>123456</correlationId>
                </root>"""
        expect:
        assertThat(asXml(xml)).node("root").node("path").matches("^/api/[0-9]{2}\$")
    }

    def "should escape single quotes in a quoted string"() {
        given:
        String xml = """<?xml version="1.0" encoding="UTF-8" ?>
            <root>
                <text>text with &apos;quotes&apos; inside</text>
            </root>
                """
        expect:
        assertThat(asXml(xml)).node("root").node("text").isEqualTo("text with 'quotes' inside")
    }

    def "should escape brackets in a string"() {
        given:
        String xml = """<?xml version="1.0" encoding="UTF-8" ?>
            <root>
                <id>&lt;escape me&gt;</id>
            </root>
                """
        expect:
        assertThat(asXml(xml)).node("root").node("id").isEqualTo("<escape me>")
    }

    def "should escape double quotes in a quoted string"() {
        given:
        String xml = """<?xml version="1.0" encoding="UTF-8" ?>
            <root>
                <text>text with &quot;quotes&quot; inside</text>
            </root>
                """
        expect:
        assertThat(asXml(xml)).node("root").node("text").isEqualTo('''text with "quotes" inside''')
    }

    def 'should match array containing an array of primitives'() {
        given:
        String xml =  '''<?xml version="1.0" encoding="UTF-8" ?>
        <root>
            <first_name>existing</first_name>
            <elements>
                <partners>
                    <role>AGENT</role>
                    <payment_methods>BANK</payment_methods>
                    <payment_methods>CASH</payment_methods>
                </partners>
            </elements>
        </root>
    '''
        expect:
        assertThat(asXml(xml)).node("root").array("elements").array("partners").contains("payment_methods").isEqualTo("BANK")
    }

    def 'should match pattern in array'() {
        given:
        String xml =  '''<?xml version="1.0" encoding="UTF-8" ?>
        <root>
            <authorities>ROLE_ADMIN</authorities>
        </root>
        '''
        expect:
        assertThat(asXml(xml)).node("root").array("authorities").matches("^[a-zA-Z0-9_\\- ]+\$")
    }

    def 'should manage to parse array with string values'() {
        given:
        String xml =  '''<?xml version="1.0" encoding="UTF-8" ?>
        <root>
            <some_list>name1</some_list>
            <some_list>name2</some_list>
        </root>'''
        expect:
        assertThat(asXml(xml)).node("root").array("some_list").isEqualTo("name1")
        assertThat(asXml(xml)).node("root").array("some_list").isEqualTo("name2")
    }

    @Issue("#2")
    def 'should allow nested calls with counting the elements size'() {
        given:
            String xml =  '''<?xml version="1.0" encoding="UTF-8" ?>
            <root>
                <some_list>name1</some_list>
                <some_list>name2</some_list>
            </root>'''

        expect:
            assertThat(asXml(xml)).node("root").array("some_list").hasSize(2).isEqualTo("name1")
    }

    @Issue("#2")
    def 'should count the elements size'() {
        given:
            String xml =  '''<?xml version="1.0" encoding="UTF-8" ?>
        <root>
            <some_list>name1</some_list>
            <some_list>name2</some_list>
        </root>'''

        expect:
            assertThat(asXml(xml)).node("root").array("some_list").hasSize(2)
    }

    @Issue("#2")
    def 'should throw exception if size is wrong'() {
        given:
            String xml =  '''<?xml version="1.0" encoding="UTF-8" ?>
            <root>
                <some_list>name1</some_list>
                <some_list>name2</some_list>
            </root>'''

        when:
            assertThat(asXml(xml)).node("root").array("some_list").hasSize(1)
        then:
            IllegalStateException e = thrown(IllegalStateException)
            e.message.contains("has size [2] and not [1] for XPath <count(/root/some_list)>")
    }

    @Issue("#2")
    def 'should return 0 if element is missing'() {
        given:
            String xml =  '''<?xml version="1.0" encoding="UTF-8" ?>
            <root>
                <some_list>name1</some_list>
                <some_list>name2</some_list>
            </root>'''

        when:
            assertThat(asXml(xml)).node("root").array("foo").hasSize(1)
        then:
            IllegalStateException e = thrown(IllegalStateException)
            e.message.contains("has size [0] and not [1] for XPath <count(/root/foo)>")
    }
}