package com.vd.diff

import com.vd.diff.content.Row
import com.vd.diff.content.parser.RowBuilder
import spock.lang.Specification
import spock.lang.Unroll

class RowBuilderSpec extends Specification {

    @Unroll
    def "ToRow function should parse raw row into the Instance"() {
        when:
            def actualRow = RowBuilder.toRow(rawRow)

        then:
            actualRow == expectedRow

        where:
            rawRow                                  | expectedRow
            "05c-8\t(2 14 null)"                    |
                    new Row(
                            "05c-8\t(2 14 null)",
                            "05c-8", ["2 14 null": new Row.Event(2 as int, 14l, null)
                    ])
            "05c-8\t(2 14 null)(2 15 app.agg.ccom)" |
                    new Row(
                            "05c-8\t(2 14 null)(2 15 app.agg.ccom)",
                            "05c-8", [
                            "2 14 null"        : new Row.Event(2 as int, 14l, null),
                            "2 15 app.agg.ccom": new Row.Event(2 as int, 15l, "app.agg.ccom")
                    ])

    }

}
