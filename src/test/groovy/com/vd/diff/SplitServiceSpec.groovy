package com.vd.diff

import com.vd.diff.split.SplitService
import spock.lang.Specification

class SplitServiceSpec extends Specification {

    def "SplitService should split range by provided number of splits"() {
        given:
            def startKey = "00001578-ec0b-4428-a348-3c9668493b7b"
            def stopKey = "00001bbb-7872-468b-91db-815f99be87c1"

        when:
            def splits = SplitService.splitRange(startKey, stopKey, 10).splits
            splits.each { println it }

            def firstSplit = splits.remove(0)
            def lastSplit = splits.removeLast()

        then:
            assert firstSplit.startKey == startKey && firstSplit.stopKey > startKey && firstSplit.stopKey < stopKey
            assert lastSplit.startKey > startKey && lastSplit.stopKey > startKey && lastSplit.stopKey == stopKey

            splits.each { split ->
                assert split.startKey > startKey && split.stopKey < stopKey
            }
    }
}
