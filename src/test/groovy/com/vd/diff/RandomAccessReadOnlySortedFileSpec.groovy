package com.vd.diff

import com.vd.diff.content.Row
import com.vd.diff.file.FastAccessRowFile
import com.vd.diff.utils.FileUtils
import spock.lang.Specification

class RandomAccessReadOnlySortedFileSpec extends Specification {

    def "Upon the create RandomAccessFile firstRow and Last row should be read"() {
        given:
            def fileA = FileUtils.getFileFromResources("/aus-28-2002", RandomAccessReadOnlySortedFileSpec.class)
            def fileB = FileUtils.getFileFromResources("/aws-28-2002", RandomAccessReadOnlySortedFileSpec.class)

        when:
            def rAFA = new FastAccessRowFile(fileA, 2 as int)
            def rAFB = new FastAccessRowFile(fileB, 4096 * 4 as int)

        then:
            rAFA.firstRow == new Row(
                    "00001578-ec0b-4428-a348-3c9668493b7b\t(10 1487614402000 null)",
                    "00001578-ec0b-4428-a348-3c9668493b7b", [
                    "10-1487614402000": new Row.Event("10 1487614402000 null", 10, 1487614402000L, null)
            ])

            rAFA.lastRow == new Row(
                    "ffffd8c2-5b23-4c07-8b5b-babdc1f9f968\t(304 1487560700000 null)",
                    "ffffd8c2-5b23-4c07-8b5b-babdc1f9f968", [
                    "304-1487560700000": new Row.Event("304 1487560700000 null", 304, 1487560700000, null)
            ])
            rAFB.firstRow == new Row(
                    "00001578-ec0b-4428-a348-3c9668493b7b\t(10 1487614402000 null)",
                    "00001578-ec0b-4428-a348-3c9668493b7b", [
                    "10-1487614402000": new Row.Event("10 1487614402000 null", 10, 1487614402000L, null)
            ])

            rAFB.lastRow == new Row(
                    "ffffd8c2-5b23-4c07-8b5b-babdc1f9f968\t(304 1487560700000 null)",
                    "ffffd8c2-5b23-4c07-8b5b-babdc1f9f968", [
                    "304-1487560700000": new Row.Event("304 1487560700000 null", 304, 1487560700000, null)
            ])
    }

}
