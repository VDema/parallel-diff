package com.vd.diff.compare

import com.google.common.base.Preconditions
import com.vd.diff.utils.FileUtils
import spock.lang.Ignore
import spock.lang.Specification

class DiffServiceSpec extends Specification {

    @Ignore
    def "DiffService should compare two files and return the result file"() {
        def fileA = FileUtils.getFileFromResources("/aus-28-2002", DiffServiceSpec.class)
        def fileB = FileUtils.getFileFromResources("/aws-28-2002", DiffServiceSpec.class)

        when:
            def diff = new DiffService().diffFiles(fileA, fileB, "eventsDiff.txt")
            println "The diff file path is: [${diff.getAbsolutePath()}]. Size of file is: [${diff.length()}]"

        then:
            diff
    }

    def "Always fail"() {
        when:
            println "will fail"
        then:
            false
    }
}
