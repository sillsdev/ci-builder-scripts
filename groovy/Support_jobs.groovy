/*
 * DSL script to create some support jobs
 */

/*
 * Definition of jobs
 */

for (job in [
	[name: 'FwSupportTools', branch: 'develop'],
	[name: 'ci-builder-scripts', branch: 'master']])
{
	pipelineJob("${job.name}-checkoutOnly") {
		description """<p>This job keeps the <i>${job.name}</i> directory up-to-date on the packaging agents.</p>
<p>The job is created by the DSL plugin from <i>Support-jobs.groovy</i> script.<p>"""

		quietPeriod(120 /*seconds*/)

		properties {
			pipelineTriggers {
				triggers {
					gerrit {
						gerritProjects {
							gerritProject {
								compareType('PLAIN')
								pattern(job.name)
								branches {
									branch {
										compareType('PLAIN')
										pattern(job.branch)
									}
								}
								disableStrictForbiddenFileVerification(false)
							}
						}
						triggerOnEvents {
							refUpdated()
						}
					}
				}
			}
		}

		definition {
			cpsScm {
				scm {
					github("sillsdev/${job.name}", job.branch)
				}
				scriptPath('Jenkinsfile')
			}
		}
	}
}
