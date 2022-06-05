package com.vd.diff.split

import com.vd.diff.split.file.FileSplit
import com.vd.diff.split.file.FileSplits
import com.vd.diff.split.virtual.VirtualSplits
import spock.lang.Ignore
import spock.lang.Specification

@Ignore
class SplitsSpec extends Specification {

    def "Intersections should check splits for intersections and create pairs where closed splits should be joined"() {
        given:
            def baseSplits = VirtualSplits.builder()
                    .add("1", "10")
                    .add("10", "20")
                    .add("20", "30")
                    .add("30", "40")
                    .add("40", "50")
                    .add("50", "60")
                    .add("60", "70")
                    .add("70", "80")
                    .add("80", "90")
                    .add("90", "99")
                    .build()

            def splitsA = FileSplits.builder()
                    .add("1", "13", 1L, 2L)
                    .add("13", "25", 1L, 2L)
                    .add("25", "37", 1L, 2L)
                    .add("37", "49", 1L, 2L)
                    .add("49", "51", 1L, 2L)
                    .add("51", "55", 1L, 2L)
                    .add("55", "61", 1L, 2L)
                    .add("61", "78", 1L, 2L)
                    .add("78", "87", 1L, 2L)
                    .build()

            def splitsB = FileSplits.builder()
                    .add("45", "54", 1L, 2L)
                    .add("54", "58", 1L, 2L)
                    .add("58", "62", 1L, 2L)
                    .add("62", "67", 1L, 2L)
                    .add("67", "71", 1L, 2L)
                    .add("71", "75", 1L, 2L)
                    .add("75", "78", 1L, 2L)
                    .add("78", "91", 1L, 2L)
                    .add("91", "99", 1L, 2L)
                    .build()

        when:
            def res = baseSplits.intersections(splitsA, splitsB)

        then:
            res.size() == baseSplits.size
            res[0].tryLeft() == of("1", "13") && !res[0].tryRight()
            res[1].tryLeft() == of("1", "25") && !res[1].tryRight()
            res[2].tryLeft() == of("13", "37") && !res[2].tryRight()
            res[3].tryLeft() == of("25", "49") && !res[3].tryRight()
            res[4].tryLeft() == of("37", "51") && res[4].tryRight() == of("45", "54")
            res[5].tryLeft() == of("49", "61") && res[5].tryRight() == of("45", "62")
            res[6].tryLeft() == of("55", "78") && res[6].tryRight() == of("58", "71")
            res[7].tryLeft() == of("61", "87") && res[7].tryRight() == of("67", "91")
            res[8].tryLeft() == of("78", "87") && res[8].tryRight() == of("78", "91")
            !res[9].tryLeft() && res[9].tryRight() == of("78", "99")

            //* Next pairs will be created for virtual splits range:
            //* 1.  1-10:  A(1-13)                       : 1-10 records will be flushed as NEW. 10-13 will be skipped
            //* 2.  10-20: A(1-13) + (13-25)             : 1-10 records will be skipped. 10-20 marked as as NEW. 20-25 will be skipped
            //* 3.  20-30: A(13-25) + (25-37)            : 13-20 records will be skipped. 20-30 marked as as NEW. 30-37 will be skipped
            //* 4.  30-40: A(25-37) + (37-49)            : 25-30 records will be skipped. 30-40 marked as as NEW. 40-49 will be skipped
            //* Interesting starts here :)
            //* 5.  40-50: A(37-49) + (49-51) + B(45-54) : 37-40 records will be skipped. 40-50 will be compared. 50-51(A) and 50-54(B) will be skipped
            //* 6.  50-60: A(49-61)           + B(45-62) : 49-50(A) + 45-50(B) will be skipped. 50-60 will be compared. 60-61(A) and 60-62(B) will be skipped
            //* 7.  60-70: A(55-78)           + B(58-71) : 55-60(A) + 58-60(B) will be skipped. 60-70 will be compared. 70-78(A) and 70-71(B) will be skipped
            //* 8.  70-80: A(61-87)           + B(67-91) : 61-70(A) + 67-70(B) will be skipped. 70-80 will be compared. 80-87(A) and 70-91(B) will be skipped
            //* 9.  80-90: A(78-87)           + B(78-91) : 78-80(A) + 78-80(B) will be skipped. 80-90 will be compared. 90-91(B) will be skipped
            //* 10. 90-99:                      B(78-99) : 90-99(B) will be marked as NEW.
    }

    Optional<FileSplit> of(String startKey, String stopKey) {
        return Optional.of(new FileSplit(startKey, stopKey, 1L, 2L))
    }

}
