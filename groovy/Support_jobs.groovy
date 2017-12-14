/*
 * DSL script to create some support jobs
 */

/*
 * Definition of jobs
 */

pipelineJob('FwSupport-checkoutOnly') {
	description '''<p>This job keeps the <i>FwSupportTools</i> directory up-to-date on the packaging agents.</p>
<p>The job is created by the DSL plugin from <i>Support-jobs.groovy</i> script.<p>'''

	triggers {
		gerrit {
			project('FwSupportTools', 'develop')
			events {
				refUpdated()
			}
		}
	}

	definition {
		cpsScm {
			scm {
				github('sillsdev/FwSupportTools', 'develop')
			}
			scriptPath('Jenkinsfile')
		}
	}
}

pipelineJob('ci-builder-scripts-checkoutOnly') {
	description '''<p>This job keeps the <i>ci-builder-scripts</i> directory up-to-date on the packaging agents.</p>
<p>The job is created by the DSL plugin from <i>Support-jobs.groovy</i> script.<p>'''

	triggers {
		gerrit {
			project('ci-builder-scripts', 'master')
			events {
				refUpdated()
			}
		}
	}

	definition {
		cpsScm {
			scm {
				github('sillsdev/ci-builder-scripts', 'master')
			}
			scriptPath('Jenkinsfile')
		}
	}
}
