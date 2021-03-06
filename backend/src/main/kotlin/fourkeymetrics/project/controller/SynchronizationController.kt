package fourkeymetrics.project.controller

import fourkeymetrics.project.controller.applicationservice.SynchronizationApplicationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

data class SynchronizationRecordResponse(val synchronizationTimestamp: Long?)

@RestController
class SynchronizationController {
    @Autowired
    private lateinit var synchronizationApplicationService: SynchronizationApplicationService

    @PostMapping("/api/project/{projectId}/synchronization")
    fun updateBuilds(@PathVariable projectId: String): ResponseEntity<SynchronizationRecordResponse> {
        val updatedTimestamp = synchronizationApplicationService.synchronize(projectId)

        return if (updatedTimestamp == null) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        } else {
            ResponseEntity.ok(SynchronizationRecordResponse(updatedTimestamp))
        }
    }

    @GetMapping("/api/project/{projectId}/synchronization")
    fun getLastSynchronization(@PathVariable projectId: String): ResponseEntity<SynchronizationRecordResponse> {
        val lastSyncTimestamp = synchronizationApplicationService.getLastSyncTimestamp(projectId)

        return ResponseEntity.ok(SynchronizationRecordResponse(lastSyncTimestamp))
    }
}
