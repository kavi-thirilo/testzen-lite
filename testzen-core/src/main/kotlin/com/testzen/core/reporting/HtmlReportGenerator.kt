package com.testzen.core.reporting

import java.io.File
import java.time.format.DateTimeFormatter
import java.time.ZoneId

/**
 * Generates interactive HTML reports with drill-down capabilities.
 *
 * Features:
 * - Executive summary dashboard
 * - Module/Feature/Story/Test case hierarchy
 * - Interactive charts and graphs
 * - Step-by-step details with screenshots
 * - Failure analysis
 * - Filtering and search
 * - Responsive design
 */
class HtmlReportGenerator(
    private val config: HtmlReportConfig = HtmlReportConfig()
) {
    /**
     * Generate HTML report and save to file.
     */
    fun generate(report: TestExecutionReport, outputPath: String): File {
        val html = generateHtml(report)
        val file = File(outputPath)
        file.parentFile?.mkdirs()
        file.writeText(html)
        return file
    }

    /**
     * Generate HTML content.
     */
    fun generateHtml(report: TestExecutionReport): String {
        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"en\">")
            appendHead(report)
            appendLine("<body>")
            appendNavbar(report)
            appendLine("<div class=\"container-fluid\">")
            appendExecutiveSummary(report)
            appendModuleCards(report)
            appendDetailedResults(report)
            appendFailureAnalysis(report)
            appendLine("</div>")
            appendScripts(report)
            appendLine("</body>")
            appendLine("</html>")
        }
    }

    private fun StringBuilder.appendHead(report: TestExecutionReport) {
        appendLine("""
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${escapeHtml(report.name)} - Test Report</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
    <style>
        :root {
            --pass-color: #28a745;
            --fail-color: #dc3545;
            --skip-color: #ffc107;
            --blocked-color: #6c757d;
            --error-color: #dc3545;
        }
        body { background-color: #f8f9fa; }
        .navbar-brand { font-weight: bold; }
        .stat-card { border-left: 4px solid; transition: transform 0.2s; }
        .stat-card:hover { transform: translateY(-2px); }
        .stat-card.passed { border-left-color: var(--pass-color); }
        .stat-card.failed { border-left-color: var(--fail-color); }
        .stat-card.skipped { border-left-color: var(--skip-color); }
        .stat-card.total { border-left-color: #007bff; }
        .stat-value { font-size: 2rem; font-weight: bold; }
        .module-card { transition: all 0.3s; cursor: pointer; }
        .module-card:hover { box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
        .progress { height: 25px; }
        .progress-bar { transition: width 0.5s ease-in-out; }
        .status-badge { font-size: 0.75rem; padding: 0.25rem 0.5rem; }
        .status-passed { background-color: var(--pass-color); }
        .status-failed { background-color: var(--fail-color); }
        .status-skipped { background-color: var(--skip-color); color: #000; }
        .status-blocked { background-color: var(--blocked-color); }
        .status-error { background-color: var(--error-color); }
        .screenshot-thumbnail { max-width: 150px; cursor: pointer; border: 1px solid #dee2e6; }
        .screenshot-modal img { max-width: 100%; }
        .step-row { border-left: 3px solid transparent; }
        .step-row.passed { border-left-color: var(--pass-color); }
        .step-row.failed { border-left-color: var(--fail-color); }
        .step-row.skipped { border-left-color: var(--skip-color); }
        .test-card { margin-bottom: 1rem; }
        .test-card .card-header { cursor: pointer; }
        .test-card .card-header:hover { background-color: #e9ecef; }
        .duration-badge { font-family: monospace; }
        .error-message { font-family: monospace; font-size: 0.875rem; white-space: pre-wrap; }
        .filter-section { background-color: #fff; padding: 1rem; border-radius: 0.5rem; margin-bottom: 1rem; }
        .hierarchy-nav { font-size: 0.875rem; }
        .drill-down-section { display: none; }
        .drill-down-section.active { display: block; }
        .chart-container { height: 300px; }
        @media print {
            .no-print { display: none !important; }
            .card { break-inside: avoid; }
        }
    </style>
</head>
        """.trimIndent())
    }

    private fun StringBuilder.appendNavbar(report: TestExecutionReport) {
        val statusClass = when (report.status) {
            TestStatus.PASSED -> "bg-success"
            TestStatus.FAILED, TestStatus.ERROR -> "bg-danger"
            else -> "bg-secondary"
        }
        appendLine("""
<nav class="navbar navbar-expand-lg navbar-dark $statusClass mb-4">
    <div class="container-fluid">
        <span class="navbar-brand">
            <i class="bi bi-clipboard-check me-2"></i>${escapeHtml(report.name)}
        </span>
        <div class="navbar-text text-white">
            <span class="me-3"><i class="bi bi-calendar me-1"></i>${report.formattedStartTime}</span>
            <span class="me-3"><i class="bi bi-clock me-1"></i>${report.formattedDuration}</span>
            <span class="badge bg-light text-dark">${report.environment}</span>
        </div>
    </div>
</nav>
        """.trimIndent())
    }

    private fun StringBuilder.appendExecutiveSummary(report: TestExecutionReport) {
        appendLine("""
<section id="executive-summary" class="mb-4">
    <h4 class="mb-3"><i class="bi bi-speedometer2 me-2"></i>Executive Summary</h4>
    <div class="row">
        <!-- Total Tests -->
        <div class="col-md-3 col-sm-6 mb-3">
            <div class="card stat-card total">
                <div class="card-body text-center">
                    <div class="stat-value text-primary">${report.totalTests}</div>
                    <div class="text-muted">Total Tests</div>
                </div>
            </div>
        </div>
        <!-- Passed Tests -->
        <div class="col-md-3 col-sm-6 mb-3">
            <div class="card stat-card passed">
                <div class="card-body text-center">
                    <div class="stat-value text-success">${report.passedTests}</div>
                    <div class="text-muted">Passed</div>
                </div>
            </div>
        </div>
        <!-- Failed Tests -->
        <div class="col-md-3 col-sm-6 mb-3">
            <div class="card stat-card failed">
                <div class="card-body text-center">
                    <div class="stat-value text-danger">${report.failedTests}</div>
                    <div class="text-muted">Failed</div>
                </div>
            </div>
        </div>
        <!-- Pass Rate -->
        <div class="col-md-3 col-sm-6 mb-3">
            <div class="card stat-card ${if (report.testPassRatePercent >= 80) "passed" else "failed"}">
                <div class="card-body text-center">
                    <div class="stat-value ${if (report.testPassRatePercent >= 80) "text-success" else "text-danger"}">
                        ${"%.1f".format(report.testPassRatePercent)}%
                    </div>
                    <div class="text-muted">Pass Rate</div>
                </div>
            </div>
        </div>
    </div>

    <!-- Progress Bar -->
    <div class="card mb-4">
        <div class="card-body">
            <div class="progress">
                <div class="progress-bar bg-success" style="width: ${report.testPassRatePercent}%"
                     title="Passed: ${report.passedTests}">
                    ${report.passedTests} Passed
                </div>
                <div class="progress-bar bg-danger" style="width: ${(report.failedTests.toDouble() / report.totalTests * 100).takeIf { !it.isNaN() } ?: 0.0}%"
                     title="Failed: ${report.failedTests}">
                    ${if (report.failedTests > 0) "${report.failedTests} Failed" else ""}
                </div>
                <div class="progress-bar bg-warning" style="width: ${(report.skippedTests.toDouble() / report.totalTests * 100).takeIf { !it.isNaN() } ?: 0.0}%"
                     title="Skipped: ${report.skippedTests}">
                    ${if (report.skippedTests > 0) "${report.skippedTests} Skipped" else ""}
                </div>
            </div>
        </div>
    </div>

    <!-- Summary Stats -->
    <div class="row">
        <div class="col-md-3">
            <div class="card">
                <div class="card-body text-center">
                    <h6 class="text-muted">Modules</h6>
                    <span class="h4">${report.passedModules}/${report.totalModules}</span>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card">
                <div class="card-body text-center">
                    <h6 class="text-muted">Features</h6>
                    <span class="h4">${report.passedFeatures}/${report.totalFeatures}</span>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card">
                <div class="card-body text-center">
                    <h6 class="text-muted">Stories</h6>
                    <span class="h4">${report.passedStories}/${report.totalStories}</span>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card">
                <div class="card-body text-center">
                    <h6 class="text-muted">Steps</h6>
                    <span class="h4">${report.passedSteps}/${report.totalSteps}</span>
                </div>
            </div>
        </div>
    </div>
</section>
        """.trimIndent())
    }

    private fun StringBuilder.appendModuleCards(report: TestExecutionReport) {
        if (report.modules.isEmpty()) return

        appendLine("""
<section id="modules" class="mb-4">
    <h4 class="mb-3"><i class="bi bi-grid me-2"></i>Modules</h4>
    <div class="row">
        """.trimIndent())

        for (module in report.modules) {
            val statusClass = when (module.status) {
                TestStatus.PASSED -> "border-success"
                TestStatus.FAILED, TestStatus.ERROR -> "border-danger"
                else -> "border-secondary"
            }
            appendLine("""
        <div class="col-md-4 mb-3">
            <div class="card module-card $statusClass" onclick="showModule('${module.moduleId}')">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <strong>${escapeHtml(module.name)}</strong>
                    <span class="badge ${getStatusBadgeClass(module.status)}">${module.status}</span>
                </div>
                <div class="card-body">
                    <div class="row text-center">
                        <div class="col-4">
                            <div class="h5 text-success mb-0">${module.passedTests}</div>
                            <small class="text-muted">Passed</small>
                        </div>
                        <div class="col-4">
                            <div class="h5 text-danger mb-0">${module.failedTests}</div>
                            <small class="text-muted">Failed</small>
                        </div>
                        <div class="col-4">
                            <div class="h5 text-primary mb-0">${module.totalTests}</div>
                            <small class="text-muted">Total</small>
                        </div>
                    </div>
                    <div class="progress mt-3" style="height: 8px;">
                        <div class="progress-bar bg-success" style="width: ${module.passRatePercent}%"></div>
                    </div>
                    <div class="mt-2 text-center">
                        <small class="text-muted">
                            ${"%.1f".format(module.passRatePercent)}% pass rate
                            &bull; ${module.totalFeatures} features
                            &bull; ${formatDuration(module.durationMs)}
                        </small>
                    </div>
                </div>
            </div>
        </div>
            """.trimIndent())
        }

        appendLine("""
    </div>
</section>
        """.trimIndent())
    }

    private fun StringBuilder.appendDetailedResults(report: TestExecutionReport) {
        appendLine("""
<section id="detailed-results" class="mb-4">
    <h4 class="mb-3"><i class="bi bi-list-check me-2"></i>Detailed Results</h4>

    <!-- Filter Section -->
    <div class="filter-section no-print">
        <div class="row">
            <div class="col-md-3">
                <select class="form-select" id="moduleFilter" onchange="filterResults()">
                    <option value="">All Modules</option>
                    ${report.modules.joinToString("\n") { "<option value=\"${it.moduleId}\">${escapeHtml(it.name)}</option>" }}
                </select>
            </div>
            <div class="col-md-3">
                <select class="form-select" id="statusFilter" onchange="filterResults()">
                    <option value="">All Statuses</option>
                    <option value="PASSED">Passed</option>
                    <option value="FAILED">Failed</option>
                    <option value="SKIPPED">Skipped</option>
                </select>
            </div>
            <div class="col-md-4">
                <input type="text" class="form-control" id="searchFilter" placeholder="Search tests..." onkeyup="filterResults()">
            </div>
            <div class="col-md-2">
                <button class="btn btn-outline-secondary w-100" onclick="expandAll()">
                    <i class="bi bi-arrows-expand"></i> Expand All
                </button>
            </div>
        </div>
    </div>

    <!-- Module Details -->
    <div class="accordion" id="moduleAccordion">
        """.trimIndent())

        for ((moduleIndex, module) in report.modules.withIndex()) {
            appendModuleDetails(module, moduleIndex)
        }

        // Direct test cases (not in any module)
        if (report.directTestCases.isNotEmpty()) {
            appendLine("""
        <div class="accordion-item">
            <h2 class="accordion-header">
                <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#directTests">
                    <span class="badge ${getStatusBadgeClass(TestStatus.PASSED)} me-2">
                        ${report.directTestCases.count { it.passed }}/${report.directTestCases.size}
                    </span>
                    Uncategorized Tests
                </button>
            </h2>
            <div id="directTests" class="accordion-collapse collapse" data-bs-parent="#moduleAccordion">
                <div class="accordion-body">
            """.trimIndent())

            for (testCase in report.directTestCases) {
                appendTestCaseCard(testCase)
            }

            appendLine("""
                </div>
            </div>
        </div>
            """.trimIndent())
        }

        appendLine("""
    </div>
</section>
        """.trimIndent())
    }

    private fun StringBuilder.appendModuleDetails(module: ModuleResult, moduleIndex: Int) {
        val collapseId = "module-${module.moduleId}"
        appendLine("""
        <div class="accordion-item" data-module="${module.moduleId}">
            <h2 class="accordion-header">
                <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#$collapseId">
                    <span class="badge ${getStatusBadgeClass(module.status)} me-2">
                        ${module.passedTests}/${module.totalTests}
                    </span>
                    <strong>${escapeHtml(module.name)}</strong>
                    <span class="ms-auto me-3 duration-badge badge bg-light text-dark">
                        <i class="bi bi-clock me-1"></i>${formatDuration(module.durationMs)}
                    </span>
                </button>
            </h2>
            <div id="$collapseId" class="accordion-collapse collapse" data-bs-parent="#moduleAccordion">
                <div class="accordion-body">
        """.trimIndent())

        // Features within module
        for (feature in module.features) {
            appendFeatureDetails(feature, module.moduleId)
        }

        // Direct test cases in module
        for (testCase in module.directTestCases) {
            appendTestCaseCard(testCase)
        }

        appendLine("""
                </div>
            </div>
        </div>
        """.trimIndent())
    }

    private fun StringBuilder.appendFeatureDetails(feature: FeatureResult, moduleId: String) {
        val collapseId = "feature-${feature.featureId}"
        appendLine("""
                    <div class="card mb-3">
                        <div class="card-header" data-bs-toggle="collapse" data-bs-target="#$collapseId" style="cursor: pointer;">
                            <div class="d-flex justify-content-between align-items-center">
                                <span>
                                    <i class="bi bi-folder me-2"></i>
                                    <strong>${escapeHtml(feature.name)}</strong>
                                </span>
                                <span>
                                    <span class="badge ${getStatusBadgeClass(feature.status)}">${feature.status}</span>
                                    <span class="badge bg-light text-dark ms-2">${feature.passedTests}/${feature.totalTests}</span>
                                </span>
                            </div>
                        </div>
                        <div id="$collapseId" class="collapse">
                            <div class="card-body">
        """.trimIndent())

        // Stories within feature
        for (story in feature.stories) {
            appendStoryDetails(story, feature.featureId)
        }

        // Direct test cases in feature
        for (testCase in feature.directTestCases) {
            appendTestCaseCard(testCase)
        }

        appendLine("""
                            </div>
                        </div>
                    </div>
        """.trimIndent())
    }

    private fun StringBuilder.appendStoryDetails(story: StoryResult, featureId: String) {
        val collapseId = "story-${story.storyId}"
        appendLine("""
                                <div class="card mb-2">
                                    <div class="card-header bg-light" data-bs-toggle="collapse" data-bs-target="#$collapseId" style="cursor: pointer;">
                                        <div class="d-flex justify-content-between align-items-center">
                                            <span>
                                                <i class="bi bi-bookmark me-2"></i>
                                                ${escapeHtml(story.storyId)}: ${escapeHtml(story.name)}
                                            </span>
                                            <span>
                                                <span class="badge ${getStatusBadgeClass(story.status)}">${story.status}</span>
                                                <span class="badge bg-secondary ms-2">${story.passedTests}/${story.totalTests}</span>
                                            </span>
                                        </div>
                                    </div>
                                    <div id="$collapseId" class="collapse">
                                        <div class="card-body">
        """.trimIndent())

        for (testCase in story.testCases) {
            appendTestCaseCard(testCase)
        }

        appendLine("""
                                        </div>
                                    </div>
                                </div>
        """.trimIndent())
    }

    private fun StringBuilder.appendTestCaseCard(testCase: TestCaseResult) {
        val collapseId = "test-${testCase.id}"
        val statusIcon = when (testCase.status) {
            TestStatus.PASSED -> "<i class=\"bi bi-check-circle-fill text-success\"></i>"
            TestStatus.FAILED, TestStatus.ERROR -> "<i class=\"bi bi-x-circle-fill text-danger\"></i>"
            TestStatus.SKIPPED -> "<i class=\"bi bi-skip-forward-fill text-warning\"></i>"
            else -> "<i class=\"bi bi-circle text-secondary\"></i>"
        }

        appendLine("""
                                            <div class="test-card card" data-status="${testCase.status}" data-testid="${testCase.testId}">
                                                <div class="card-header" data-bs-toggle="collapse" data-bs-target="#$collapseId">
                                                    <div class="d-flex justify-content-between align-items-center">
                                                        <span>
                                                            $statusIcon
                                                            <strong class="ms-2">${escapeHtml(testCase.name)}</strong>
                                                            <small class="text-muted ms-2">(${testCase.testId})</small>
                                                        </span>
                                                        <span>
                                                            <span class="badge bg-light text-dark duration-badge">
                                                                ${formatDuration(testCase.durationMs)}
                                                            </span>
                                                            <span class="badge bg-secondary ms-2">
                                                                ${testCase.passedSteps}/${testCase.totalSteps} steps
                                                            </span>
                                                        </span>
                                                    </div>
                                                </div>
                                                <div id="$collapseId" class="collapse">
                                                    <div class="card-body">
        """.trimIndent())

        // Test case details
        if (testCase.description != null) {
            appendLine("<p class=\"text-muted\">${escapeHtml(testCase.description)}</p>")
        }

        // Error message for failed tests
        if (testCase.failed && testCase.errorMessage != null) {
            appendLine("""
                                                        <div class="alert alert-danger">
                                                            <strong>Error:</strong>
                                                            <pre class="error-message mb-0">${escapeHtml(testCase.errorMessage)}</pre>
                                                        </div>
            """.trimIndent())
        }

        // Steps table
        appendLine("""
                                                        <table class="table table-sm table-hover">
                                                            <thead>
                                                                <tr>
                                                                    <th width="50">#</th>
                                                                    <th>Instruction</th>
                                                                    <th width="100">Duration</th>
                                                                    <th width="100">Status</th>
                                                                    <th width="200">Screenshots</th>
                                                                </tr>
                                                            </thead>
                                                            <tbody>
        """.trimIndent())

        for (step in testCase.steps) {
            val stepStatusClass = when (step.status) {
                TestStatus.PASSED -> "passed"
                TestStatus.FAILED, TestStatus.ERROR -> "failed"
                else -> "skipped"
            }

            appendLine("""
                                                                <tr class="step-row $stepStatusClass">
                                                                    <td>${step.stepNumber}</td>
                                                                    <td>
                                                                        ${escapeHtml(step.instruction)}
                                                                        ${if (step.wasHealed) "<span class=\"badge bg-info ms-1\">Healed</span>" else ""}
                                                                        ${if (step.failed && step.errorMessage != null) "<br><small class=\"text-danger\">${escapeHtml(step.errorMessage.take(100))}</small>" else ""}
                                                                    </td>
                                                                    <td><span class="duration-badge">${step.durationMs}ms</span></td>
                                                                    <td><span class="badge ${getStatusBadgeClass(step.status)}">${step.status}</span></td>
                                                                    <td>
            """.trimIndent())

            // Screenshots
            step.screenshotBefore?.let {
                appendLine("""
                                                                        <img src="${escapeHtml(it.filePath)}" class="screenshot-thumbnail me-1"
                                                                             onclick="showScreenshot('${escapeHtml(it.filePath)}')" title="Before" alt="Before">
                """.trimIndent())
            }
            step.screenshotAfter?.let {
                appendLine("""
                                                                        <img src="${escapeHtml(it.filePath)}" class="screenshot-thumbnail"
                                                                             onclick="showScreenshot('${escapeHtml(it.filePath)}')" title="After" alt="After">
                """.trimIndent())
            }

            appendLine("""
                                                                    </td>
                                                                </tr>
            """.trimIndent())
        }

        appendLine("""
                                                            </tbody>
                                                        </table>
                                                    </div>
                                                </div>
                                            </div>
        """.trimIndent())
    }

    private fun StringBuilder.appendFailureAnalysis(report: TestExecutionReport) {
        if (report.failedTests == 0) return

        appendLine("""
<section id="failure-analysis" class="mb-4">
    <h4 class="mb-3"><i class="bi bi-exclamation-triangle me-2"></i>Failure Analysis</h4>

    <div class="row">
        <!-- Top Failure Reasons -->
        <div class="col-md-6">
            <div class="card">
                <div class="card-header">
                    <strong>Top Failure Reasons</strong>
                </div>
                <div class="card-body">
                    <table class="table table-sm">
                        <thead>
                            <tr>
                                <th>Error Message</th>
                                <th width="80">Count</th>
                            </tr>
                        </thead>
                        <tbody>
        """.trimIndent())

        for (reason in report.topFailureReasons.take(5)) {
            appendLine("""
                            <tr>
                                <td><small>${escapeHtml(reason.message.take(100))}</small></td>
                                <td><span class="badge bg-danger">${reason.count}</span></td>
                            </tr>
            """.trimIndent())
        }

        appendLine("""
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- Failures by Module -->
        <div class="col-md-6">
            <div class="card">
                <div class="card-header">
                    <strong>Failures by Module</strong>
                </div>
                <div class="card-body">
                    <table class="table table-sm">
                        <thead>
                            <tr>
                                <th>Module</th>
                                <th width="80">Failed</th>
                                <th width="80">Total</th>
                            </tr>
                        </thead>
                        <tbody>
        """.trimIndent())

        for (module in report.modules.filter { it.failedTests > 0 }) {
            appendLine("""
                            <tr>
                                <td>${escapeHtml(module.name)}</td>
                                <td><span class="badge bg-danger">${module.failedTests}</span></td>
                                <td>${module.totalTests}</td>
                            </tr>
            """.trimIndent())
        }

        appendLine("""
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</section>
        """.trimIndent())
    }

    private fun StringBuilder.appendScripts(report: TestExecutionReport) {
        appendLine("""
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script>
function filterResults() {
    const moduleFilter = document.getElementById('moduleFilter').value;
    const statusFilter = document.getElementById('statusFilter').value;
    const searchFilter = document.getElementById('searchFilter').value.toLowerCase();

    document.querySelectorAll('.test-card').forEach(card => {
        const status = card.dataset.status;
        const testId = card.dataset.testid.toLowerCase();
        const text = card.textContent.toLowerCase();

        let show = true;
        if (statusFilter && status !== statusFilter) show = false;
        if (searchFilter && !text.includes(searchFilter) && !testId.includes(searchFilter)) show = false;

        card.style.display = show ? 'block' : 'none';
    });

    if (moduleFilter) {
        document.querySelectorAll('[data-module]').forEach(item => {
            item.style.display = item.dataset.module === moduleFilter ? 'block' : 'none';
        });
    } else {
        document.querySelectorAll('[data-module]').forEach(item => {
            item.style.display = 'block';
        });
    }
}

function expandAll() {
    document.querySelectorAll('.accordion-collapse').forEach(el => {
        new bootstrap.Collapse(el, { toggle: false }).show();
    });
    document.querySelectorAll('.collapse').forEach(el => {
        new bootstrap.Collapse(el, { toggle: false }).show();
    });
}

function showModule(moduleId) {
    const element = document.querySelector('#module-' + moduleId);
    if (element) {
        new bootstrap.Collapse(element).show();
        element.scrollIntoView({ behavior: 'smooth' });
    }
}

function showScreenshot(src) {
    const modal = new bootstrap.Modal(document.getElementById('screenshotModal'));
    document.getElementById('screenshotImage').src = src;
    modal.show();
}
</script>

<!-- Screenshot Modal -->
<div class="modal fade" id="screenshotModal" tabindex="-1">
    <div class="modal-dialog modal-xl">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Screenshot</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body screenshot-modal text-center">
                <img id="screenshotImage" src="" alt="Screenshot">
            </div>
        </div>
    </div>
</div>
        """.trimIndent())
    }

    private fun getStatusBadgeClass(status: TestStatus): String {
        return when (status) {
            TestStatus.PASSED -> "status-passed text-white"
            TestStatus.FAILED -> "status-failed text-white"
            TestStatus.ERROR -> "status-error text-white"
            TestStatus.SKIPPED -> "status-skipped"
            TestStatus.BLOCKED -> "status-blocked text-white"
            else -> "bg-secondary"
        }
    }

    private fun formatDuration(ms: Long): String {
        return when {
            ms < 1000 -> "${ms}ms"
            ms < 60000 -> "${"%.1f".format(ms / 1000.0)}s"
            else -> "${ms / 60000}m ${(ms % 60000) / 1000}s"
        }
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}

/**
 * Configuration for HTML report generation.
 */
data class HtmlReportConfig(
    /** Include inline screenshots as base64 */
    val embedScreenshots: Boolean = false,

    /** Include charts */
    val includeCharts: Boolean = true,

    /** Theme (light/dark) */
    val theme: String = "light",

    /** Company logo URL */
    val logoUrl: String? = null,

    /** Custom CSS */
    val customCss: String? = null,

    /** Report title prefix */
    val titlePrefix: String = "TestZen"
)
