package com.github.dsulimchuk.dynamicquery.core

import org.hamcrest.CoreMatchers.*
import org.junit.Assert.assertThat
import org.junit.Test

/**
 * @author Dmitrii Sulimchuk
 * created 22/10/16
 */
class QueryDslTest {
    @Test
    fun m() {
        val query = QueryDsl("param")
        assertThat(query.macroses.size, equalTo(0))

        query.run { m("m1") { } }
        query.run { m("m2") { } }
        assertThat(query.macroses.size, equalTo(2))
    }

    @Test
    operator fun unaryPlus() {
        val query = QueryDsl("param")
        assertThat(query.sourceQuery, equalTo(""))

        query.run { +"select" }
        assertThat(query.sourceQuery, equalTo("select"))

        query.run { +" from" }
        assertThat(query.sourceQuery, equalTo("select from"))
    }

    @Test
    fun prepare() {
        val query = query("param") {
            +"select 1 from dual where 1=1 --&m1\n--&m2"
        }
        assertThat(query.prepareText(), equalTo("select 1 from dual where 1=1 --&m1\n--&m2"))

        query.run {
            m("m1") {
                test({ true }) {
                    +"a=b"
                    +"c=d"
                }
            }
        }
        assertThat(query.prepareText(), equalTo("select 1 from dual where 1=1 and (a=b) and (c=d)\n--&m2"))

        query.run {
            m("m2") {
                test({ true }) {
                    +"x=y"
                }
            }
        }
        assertThat(query.prepareText(), equalTo("select 1 from dual where 1=1 and (a=b) and (c=d)\nand (x=y)"))
    }

    @Test
    fun prepareMacroses_for_empty_macroses() {
        val query = QueryDsl(TestParam("a", "b", null))
        val result = query.prepareMacroses()
        assertThat(result, notNullValue())
        assertThat(result.size, equalTo(0))
    }

    @Test
    fun prepareMacroses() {
        val query = QueryDsl(TestParam("a", "b", null))
        query.run {
            m("m1") {
                test({ parameter.a != null }) {
                    +"x = :a"
                }
                test({ parameter.b != null }) {
                    +"y = :b"
                }
            }
            m("m2") {
                test({ parameter.c != null }) {
                    +"z = :c"
                }
            }
        }

        val result = query.prepareMacroses()
        assertThat(result, notNullValue())
        assertThat(result.size, equalTo(2))
        assertThat(result["m1"], allOf(containsString("x = :a"), containsString("y = :b")))
        assertThat(result["m2"], containsString("1=1"))


    }

    @Test
    fun keyToCommentRegex() {
        val regex = QueryDsl("test").keyToCommentRegex("m1")


        assertThat("select 1 from dual m1".replace(regex, ""), equalTo("select 1 from dual m1"))

        assertThat("select 1 from dual --&m1".replace(regex, ""), equalTo("select 1 from dual "))
        assertThat("select 1 from dual --&m1 \n and 5=6".replace(regex, ""),
                equalTo("select 1 from dual \n and 5=6"))
        assertThat("select 1 from dual --&m1 \nand 5=6".replace(regex, ""),
                equalTo("select 1 from dual \nand 5=6"))
        assertThat("select 1 from dual where &m1 and 2=2".replace(regex, "1=2"),
                equalTo("select 1 from dual where 1=2 and 2=2"))

        assertThat("select 1 from dual /* &m1 */ and 5=6".replace(regex, ""), equalTo("select 1 from dual  and 5=6"))
    }

}

data class TestParam(val a: String?, val b: String?, val c: String?)