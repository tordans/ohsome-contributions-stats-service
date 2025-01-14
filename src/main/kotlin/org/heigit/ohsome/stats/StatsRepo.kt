package org.heigit.ohsome.stats

import org.heigit.ohsome.stats.utils.HashtagHandler
import org.heigit.ohsome.stats.utils.getGroupbyInterval
import org.jdbi.v3.core.Jdbi.create
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.Instant.EPOCH
import java.time.Instant.now
import javax.sql.DataSource

@Component
class StatsRepo {
    //please add valuable docs here

    @Autowired
    lateinit var dataSource: DataSource

    private val logger: Logger = LoggerFactory.getLogger(StatsRepo::class.java)

    //language=SQL
    private fun getStatsFromTimeSpan(hashtagHandler: HashtagHandler) = """
        SELECT
            count(distinct changeset_id) as changesets,
            count(distinct user_id) as users,
            sum(road_length) as roads,
            count(building_area) as buildings,
            count(*) as edits,
            max(changeset_timestamp) as latest
        FROM "stats"
        WHERE
            ${if (hashtagHandler.isWildCard) "startsWith" else "equals"}(hashtag, ?) 
            and changeset_timestamp > parseDateTimeBestEffortOrNull(?) 
            and changeset_timestamp < parseDateTimeBestEffortOrNull(?);
        """.trimIndent()

    //language=SQL
    @Suppress("LongMethod")
    private fun getStatsFromTimeSpanInterval(hashtagHandler: HashtagHandler) = """
       SELECT 
            count(distinct changeset_id) as changesets,
            count(distinct user_id) as users,
            sum(road_length) as roads,
            count(building_area) as buildings,
            count(*) as edits,
            toStartOfInterval(changeset_timestamp, INTERVAL ?) as startdate,
            toStartOfInterval(changeset_timestamp, INTERVAL ?) + INTERVAL ? as enddate
        FROM "stats"    
        WHERE
            ${if (hashtagHandler.isWildCard) "startsWith" else "equals"}(hashtag, ?)  
            and changeset_timestamp > ? 
            and changeset_timestamp < ?
        GROUP BY 
            startdate
    """.trimIndent()

    //language=SQL
    private val mostUsedHashtags = """
        SELECT 
            hashtag, COUNT(DISTINCT user_id) as number_of_users
        FROM "stats"
        WHERE
            changeset_timestamp > ? and changeset_timestamp < ?
        GROUP BY
            hashtag
        ORDER BY
            number_of_users DESC
        LIMIT ?
    """.trimIndent()

    //language=SQL
    private val metadata = """
        SELECT 
            max(changeset_timestamp) as max_timestamp,
            min(changeset_timestamp) as min_timestamp
        FROM "stats"
    """.trimIndent()

    /**
     * Retrieves statistics for a specific hashtag within a time span.
     *
     * @param hashtagHandler Contains the hashtag to retrieve statistics for.
     * @param startDate The start date of the time span.
     * @param endDate The end date of the time span.
     * @return A map containing the statistics.
     */
    fun getStatsForTimeSpan(hashtagHandler: HashtagHandler, startDate: Instant?, endDate: Instant?): Map<String, Any> {
        logger.info("Getting stats for hashtag: ${hashtagHandler.hashtag}, startDate: $startDate, endDate: $endDate")

        return create(dataSource).withHandle<Map<String, Any>, RuntimeException> {
            it.select(
                getStatsFromTimeSpan(hashtagHandler),
                "#${hashtagHandler.hashtag}",
                startDate ?: EPOCH,
                endDate ?: now()
            ).mapToMap().single()
        } + ("hashtag" to hashtagHandler.hashtag)
    }

    /**
     * Retrieves statistics for a specific hashtag within a time span and interval.
     *
     * @param hashtagHandler Contains the hashtag to retrieve statistics for.
     * @param startDate The start date of the time span.
     * @param endDate The end date of the time span.
     * @param interval The interval for grouping the statistics.
     * @return A list of maps containing the statistics for each interval.
     */
    fun getStatsForTimeSpanInterval(
        hashtagHandler: HashtagHandler,
        startDate: Instant? = EPOCH,
        endDate: Instant? = now(),
        interval: String
    ): List<Map<String, Any>> {
        logger.info("Getting stats for hashtag: ${hashtagHandler.hashtag}, startDate: $startDate, endDate: $endDate, interval: $interval")

        return create(dataSource).withHandle<List<Map<String, Any>>, RuntimeException> {
            it.select(
                getStatsFromTimeSpanInterval(hashtagHandler),
                getGroupbyInterval(interval),
                getGroupbyInterval(interval),
                getGroupbyInterval(interval),
                "#${hashtagHandler.hashtag}",
                startDate,
                endDate
            ).mapToMap().list()
        }
    }

    /**
     * Retrieves the most used Hashtags in the selected Timeperiod.
     *
     * @param hashtag The hashtag to retrieve statistics for.
     * @param startDate The start date of the time span.
     * @param endDate The end date of the time span.
     * @return A list of maps containing the statistics for each interval.
     */
    fun getMostUsedHashtags(
        startDate: Instant? = EPOCH,
        endDate: Instant? = now(),
        limit: Int? = 10
    ): List<Map<String, Any>> {
        logger.info("Getting trending hashtags startDate: $startDate, endDate: $endDate, limit: $limit")
        return create(dataSource).withHandle<List<Map<String, Any>>, RuntimeException> {
            it.select(
                mostUsedHashtags,
                startDate,
                endDate,
                limit
            ).mapToMap().list()
        }
    }

    /**
     * Get min_timestamp and max_timestamp for the entire database.
     *
     * @return  A map containing the two keys.
     */
    fun getMetadata(
    ): Map<String, Any> {
        return create(dataSource).withHandle<Map<String, Any>, RuntimeException> {
            it.select(
                metadata
            ).mapToMap().single()
        }
    }


}
