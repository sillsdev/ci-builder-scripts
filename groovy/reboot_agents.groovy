/*
 * DSL script for package builder Jenkins jobs
 */

def agents = "autopackager-1 autopackager-2 autopackager-4"

pipelineJob('reboot-agents') {
	description '''
<p>Reboot packaging agents once a day.</p>
<p>The job is created by the DSL plugin from <i>reboot_agents.groovy</i> script.</p>
'''

	logRotator(365, 100, 10, 10)

	properties {
		pipelineTriggers {
			triggers {
				cron {
					spec('@daily')
				}
			}
		}
	}

	definition {
		cps {
			script("""@Library('lsdev-pipeline-library') _
				rebootAgents(label: '${agents}')"""
			)
		}
	}
}
