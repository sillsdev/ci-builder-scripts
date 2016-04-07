/*
 * some common definitions for Bloom related jobs
 */
package utilities
import utilities.Helper
import utilities.common

class Bloom {
	static void generalBloomBuildJob(jobContext, useTimeout = true) {
		jobContext.with {
			priority(100);
			logRotator(365, 100)

			wrappers {
				timestamps()
				if (useTimeout) {
					timeout {
						noActivity 180
						abortBuild()
					}
				}
			}

			common.buildPublishers(delegate, 365, 100)
		}
	}

	static void defaultBuildJob(jobContext, descriptionVal, useTimeout = true) {
		generalBloomBuildJob(jobContext, useTimeout)

		jobContext.with {
			description '<p>' + descriptionVal + ''' This job gets triggered by Bloom-Wrapper-Trigger-debug.<p>
<p>The job is created by the DSL plugin from <i>BloomJobs.groovy</i> script.</p>'''

			scm {
				git {
					remote {
						github("BloomBooks/BloomDesktop", "git")
					}
					branch('*/master')
				}
			}

		}
	}

	static void defaultGitHubPRBuildJob(jobContext, descriptionVal, useTimeout = true) {
		generalBloomBuildJob(jobContext, useTimeout)
		jobContext.with {
			description """<p>$descriptionVal This job gets triggered by GitHub-Bloom-Wrapper-*.<p>
<p>The job is created by the DSL plugin from <i>BloomGitHubJobs.groovy</i> script.</p>"""

			parameters {
				stringParam("sha1", "",
					"What pull request to build, e.g. origin/pr/9/head")
			}

			scm {
				git {
					remote {
						github("BloomBooks/BloomDesktop", "git")
						refspec('+refs/pull/*:refs/remotes/origin/pr/*')
					}
					branch('${sha1}')
				}
			}
		}
	}
}

