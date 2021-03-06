package fourkeymetrics.project.controller.vo.response

import fourkeymetrics.project.model.Project
import org.apache.logging.log4j.util.Strings


class ProjectResponse(
    var id: String = Strings.EMPTY,
    var name: String = Strings.EMPTY,
    var synchronizationTimestamp: Long? = null,
) {
    constructor(project: Project) : this() {
        this.id = project.id
        this.name = project.name
        this.synchronizationTimestamp = project.synchronizationTimestamp
    }

}
