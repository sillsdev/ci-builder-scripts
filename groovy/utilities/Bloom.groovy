/*
 * some common definitions for Bloom related jobs
 */
package utilities
import utilities.Helper
import utilities.common

class Bloom {
	static void generalBloomBuildJob(jobContext, useTimeout = true) {
		jobContext.with {
			properties {
				priority(100)
			}

			logRotator(365, 100)

			wrappers {
				colorizeOutput()
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

	static void addInstallKarmaBuildStep(stepContext) {
		stepContext.with {
			// NOTE: we create `node` as a symlink. Debian has renamed it to `nodejs` but karma etc
			// expects it as `node`.
			shell('''#!/bin/bash -e
if [ ! -f node_modules/.bin/karma ]; then
	sudo apt-get install -y npm
	mkdir -p ~/bin
	PATH="$HOME/bin:$PATH"
	if [ -f /usr/bin/nodejs ]; then
		ln -s /usr/bin/nodejs ~/bin/node
	fi
	cd 'src/BloomBrowserUI'
	npm install
	npm run build
fi
''')
		}
	}

	static void addRunJsTestsBuildStep(stepContext) {
		stepContext.with {
			// Run unit tests
			shell("""#!/bin/bash -e
echo "Running unit tests"
cd 'src/BloomBrowserUI'
PATH="\$HOME/bin:\$PATH"
NODE_PATH=/usr/lib/nodejs:/usr/lib/node_modules:/usr/share/javascript
export NODE_PATH
../../node_modules/.bin/karma start --reporters dots,junit --single-run --browsers Firefox --capture-timeout 15000
""")
		}
	}

}

