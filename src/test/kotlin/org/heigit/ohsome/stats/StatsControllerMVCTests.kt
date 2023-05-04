package org.heigit.ohsome.stats

import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant


@WebMvcTest(StatsController::class)
class StatsControllerMVCTests {

    private val hashtag = "&uganda"


    @MockBean
    private lateinit var repo: StatsRepo


    @Autowired
    private lateinit var mockMvc: MockMvc


    //language=JSON
    private val expectedStatic = """{
          "changesets": 65009011,
          "users": 3003842,
          "roads": 45964973.0494135,
          "buildings": 844294167,
          "edits": 1095091515,
          "latest": "2023-03-20T10:55:38.000Z",
          "hashtag": "*"
      }"""



    @Test
    fun `stats can be served without date restriction`() {

        `when`(repo.getStatsForTimeSpan(hashtag, null, null))
            .thenReturn(mapOf("hashtag" to hashtag))

        this.mockMvc
            .perform(get("/stats/$hashtag"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.hashtag").value(hashtag))

    }


    @Test
    fun `stats can be served with explicit start and end dates`() {

        `when`(repo.getStatsForTimeSpan(anyString(), any(Instant::class.java), any(Instant::class.java)))
            .thenReturn(mapOf("hashtag" to hashtag))


        val GET = get("/stats/$hashtag")
            .queryParam("startdate", "2017-10-01T04:00+05:00")
            .queryParam("enddate", "2020-10-01T04:00+00:00")


        this.mockMvc
            .perform(GET)
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.hashtag").value(hashtag))
            .andExpect(jsonPath("$.startdate").value("2017-09-30T23:00:00Z"))
            .andExpect(jsonPath("$.enddate").value("2020-10-01T04:00:00Z"))
    }


    @Test
    fun `stats_static should return a static map of stats values`() {

        this.mockMvc
            .perform(get("/stats_static"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(content().json(expectedStatic, false))

    }


}
