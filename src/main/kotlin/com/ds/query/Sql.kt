package com.ds.query

import com.ds.query.core.QueryDsl
import mu.KLogging
import org.apache.commons.beanutils.BeanUtils
import org.apache.commons.beanutils.PropertyUtils
import javax.persistence.EntityManager

/**
 * @author Dmitrii Sulimchuk
 * created 23/10/16
 */
class Sql<T : Any>(val entityManager: EntityManager,
                   val initQueryDsl: QueryDsl<T>.() -> Unit)
: AbstractDialect() {
    companion object : KLogging()

    fun prepare(parameter: T): javax.persistence.Query {
        val query = QueryDsl(parameter)
        query.initQueryDsl()

        val queryText = query.prepareText()
        val result = entityManager.createNativeQuery(queryText)

        val allParameters = findAllQueryParameters(queryText)

        if (allParameters.isNotEmpty()) {
            if (allParameters.size == 1 && isBaseType(query.parameter)) {
                logger.debug { "set parameter ${allParameters[0]} to ${query.parameter}" }
                result.setParameter(allParameters[0], query.parameter)
            } else {
                allParameters
                        .map { it to PropertyUtils.getProperty(query.parameter, it) }
                        .forEach {
                            logger.debug { "set parameter ${it.first} to ${it.second}" }
                            result.setParameter(it.first, it.second)
                        }
            }
        }

        return result
    }



}
