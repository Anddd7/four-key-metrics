package fourkeymetrics.metrics.controller.vo

import fourkeymetrics.metrics.model.Metrics

data class MetricsInfo(val summary: Metrics, val details: List<Metrics>)

data class FourKeyMetricsResponse(
    val deploymentFrequency: MetricsInfo,
    val leadTimeForChange: MetricsInfo,
    val meanTimeToRestore: MetricsInfo,
    val changeFailureRate: MetricsInfo
)