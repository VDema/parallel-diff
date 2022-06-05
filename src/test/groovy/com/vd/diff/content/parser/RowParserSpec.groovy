package com.vd.diff.content.parser

import com.vd.diff.content.Row
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

@Ignore
class RowParserSpec extends Specification {

    @Unroll
    def "ToRow function should parse raw row into the Instance"() {
        when:
            def actualRow = RowParser.toRow(rawRow)

        then:
            actualRow == expectedRow

        where:
            rawRow                                  | expectedRow
            "05c-8\t(2 14 null)"                    |
                    new Row(
                            "05c-8\t(2 14 null)",
                            "05c-8", ["2-14": new Row.Event("2 14 null", 2 as int, 14l, null)
                    ])
            "05c-8\t(2 14 null)(2 15 app.agg.ccom)" |
                    new Row(
                            "05c-8\t(2 14 null)(2 15 app.agg.ccom)",
                            "05c-8", [
                            "2-14": new Row.Event("2 14 null", 2 as int, 14l, null),
                            "2-15": new Row.Event("2 15 app.agg.ccom", 2 as int, 15l, "app.agg.ccom")
                    ])

    }

}
