package com.toomuchcoding.xmlassert

import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author Marcin Grzejszczak
 */
class XPathSpec extends Specification {

    @Unroll
    def "should generate [#expectedXPath] for XPath [#xPath]"() {
        expect:
            xPath == expectedXPath
        where:
            xPath                                                                                                            || expectedXPath
            XPathBuilder.builder().node("some").node("nested").node("anothervalue").isEqualTo(4).xPath()                     || '''/some/nested[anothervalue=4]'''
            XPathBuilder.builder().node("some").node("nested").array("withlist").contains("name").isEqualTo("name1").xPath() || '''/some/nested/withlist[name='name1']'''
            XPathBuilder.builder().node("some").node("nested").array("withlist").contains("name").isEqualTo("name2").xPath() || '''/some/nested/withlist[name='name2']'''
            XPathBuilder.builder().node("some").node("nested").node("json").isEqualTo("with \"val'ue").xPath()               || '''/some/nested[json=concat('with "val',"'",'ue')]'''
            XPathBuilder.builder().node("some", "nested", "json").isEqualTo("with \"val'ue").xPath()                         || '''/some/nested[json=concat('with "val',"'",'ue')]'''
    }

}
