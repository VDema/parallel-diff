package com.vd.diff

import com.vd.diff.split.virtual.VirtualSplit
import spock.lang.Specification
import spock.lang.Unroll

class SplitSpec extends Specification {

    @Unroll
    def "isIntersects should check range of start/stop keys and detect if ranges intersects symmetrically"() {
        given:
            def split1 = new VirtualSplit(startKey1, stopKey1)
            def split2 = new VirtualSplit(startKey2, stopKey2)

        when:
            def isIntersects1_2 = split1.isIntersects(split2)
            def isIntersects2_1 = split2.isIntersects(split1)

        then:
            isIntersects1_2 == isIntersects2_1
            isIntersects1_2 == expectedIntersects

        where:
            startKey1 | stopKey1 | startKey2 | stopKey2 | expectedIntersects
            "3"       | "5"      | "4"       | "5"      | true
            "3"       | "7"      | "4"       | "6"      | true
            "3"       | "4"      | "5"       | "6"      | false
            "3"       | "5"      | "5"       | "6"      | false

    }
}
