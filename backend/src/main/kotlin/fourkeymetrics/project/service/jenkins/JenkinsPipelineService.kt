package fourkeymetrics.project.service.jenkins

import fourkeymetrics.common.model.Build
import fourkeymetrics.common.model.Commit
import fourkeymetrics.common.model.Stage
import fourkeymetrics.common.model.Status
import fourkeymetrics.project.repository.BuildRepository
import fourkeymetrics.project.repository.PipelineRepository
import fourkeymetrics.project.service.PipelineService
import fourkeymetrics.project.service.jenkins.dto.BuildDetailsDTO
import fourkeymetrics.project.service.jenkins.dto.BuildSummaryCollectionDTO
import fourkeymetrics.project.service.jenkins.dto.BuildSummaryDTO
import fourkeymetrics.exception.ApplicationException
import fourkeymetrics.project.model.Pipeline
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.nio.charset.Charset
import java.util.*
import kotlin.streams.toList

@Service("jenkinsPipelineService")
class JenkinsPipelineService(
    @Autowired private var restTemplate: RestTemplate,
    @Autowired private var pipelineRepository: PipelineRepository,
    @Autowired private var buildRepository: BuildRepository
) : PipelineService() {
    override fun syncBuilds(pipelineId: String): List<Build> {
        val pipeline = pipelineRepository.findById(pipelineId)

        val username = pipeline.username
        val credential = pipeline.credential
        val baseUrl = pipeline.url

        val buildsNeedToSync = getBuildSummariesFromJenkins(username!!, credential, baseUrl)
            .parallelStream()
            .filter {
                val buildInDB = buildRepository.findByBuildNumber(pipelineId, it.number)
                buildInDB == null || buildInDB.result == Status.IN_PROGRESS
            }
            .toList()

        val builds = buildsNeedToSync.parallelStream().map { buildSummary ->
            val buildDetails = getBuildDetailsFromJenkins(username, credential, baseUrl, buildSummary)

            Build(
                pipelineId,
                buildSummary.number,
                mapBuildStatus(buildSummary.result),
                buildSummary.duration,
                buildSummary.timestamp,
                buildSummary.url,
                constructBuildStages(buildDetails),
                constructBuildCommits(buildSummary).flatten()
            )
        }.toList()

        buildRepository.save(builds)

        return builds
    }

    override fun verifyPipelineConfiguration(pipeline: Pipeline) {
        val headers = setAuthHeader(pipeline.username!!, pipeline.credential)
        val entity = HttpEntity<String>("", headers)
        try {
            val response = restTemplate.exchange<String>(
                "${pipeline.url}/wfapi/", HttpMethod.GET, entity
            )
            if (!response.statusCode.is2xxSuccessful) {
                throw ApplicationException(response.statusCode, response.body!!)
            }
        } catch (ex: HttpServerErrorException) {
            throw ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, "Verify website unavailable")
        } catch (ex: HttpClientErrorException) {
            throw ApplicationException(HttpStatus.BAD_REQUEST, "Verify failed")
        }
    }

    private fun constructBuildCommits(buildSummary: BuildSummaryDTO): List<List<Commit>> {
        return buildSummary.changeSets.map { changeSetDTO ->
            changeSetDTO.items.map { commitDTO ->
                Commit(commitDTO.commitId, commitDTO.timestamp, commitDTO.date, commitDTO.msg)
            }
        }
    }

    private fun constructBuildStages(buildDetails: BuildDetailsDTO): List<Stage> {
        return buildDetails.stages.map { stageDTO ->
            val status = mapStageStatus(stageDTO.status)
            Stage(
                stageDTO.name, status, stageDTO.startTimeMillis,
                stageDTO.durationMillis, stageDTO.pauseDurationMillis
            )
        }
    }

    override fun mapStageStatus(statusInPipeline: String?): Status =
        when (statusInPipeline) {
            "SUCCESS" -> {
                Status.SUCCESS
            }
            "FAILED" -> {
                Status.FAILED
            }
            "IN_PROGRESS" -> {
                Status.IN_PROGRESS
            }
            else -> {
                Status.OTHER
            }
        }


    override fun mapBuildStatus(statusInPipeline: String?): Status =
        when (statusInPipeline) {
            null -> {
                Status.IN_PROGRESS
            }
            "SUCCESS" -> {
                Status.SUCCESS
            }
            "FAILURE" -> {
                Status.FAILED
            }
            else -> {
                Status.OTHER
            }
        }

    private fun getBuildDetailsFromJenkins(
        username: String, credential: String, baseUrl: String,
        buildSummary: BuildSummaryDTO
    ): BuildDetailsDTO {
        val headers = setAuthHeader(username, credential)
        val entity = HttpEntity<String>(headers)
        val buildDetailResponse: ResponseEntity<BuildDetailsDTO> =
            restTemplate.exchange("$baseUrl/${buildSummary.number}/wfapi/describe", HttpMethod.GET, entity)
        return buildDetailResponse.body!!
    }

    private fun getBuildSummariesFromJenkins(
        username: String, credential: String,
        baseUrl: String
    ): List<BuildSummaryDTO> {
        val headers = setAuthHeader(username, credential)
        val entity = HttpEntity<String>(headers)
        val allBuildsResponse: ResponseEntity<BuildSummaryCollectionDTO> =
            restTemplate.exchange(
                "$baseUrl/api/json?tree=allBuilds[building,number," +
                        "result,timestamp,duration,url,changeSets[items[commitId,timestamp,msg,date]]]",
                HttpMethod.GET,
                entity
            )
        return allBuildsResponse.body!!.allBuilds
    }

    private fun setAuthHeader(username: String, credential: String): HttpHeaders {
        val headers = HttpHeaders()
        val auth = "$username:$credential"
        val encodedAuth = Base64.getEncoder().encodeToString(auth.toByteArray(Charset.forName("UTF-8")))
        val authHeader = "Basic $encodedAuth"
        headers.set("Authorization", authHeader)
        return headers
    }
}
