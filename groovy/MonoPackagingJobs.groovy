/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */

def distros_tobuild = "precise trusty xenial"
def email = "eb1@sil.org"

static String GetPackageName(repo, branchName) {
	def ext = ''
	if (repo == 'gtk-sharp') {
		ext = '2'
	}
	if (branchName == 'develop') {
		ext = '4'
	}
	return "${repo}${ext}-sil"
}

for (repo in [ 'mono', 'gtk-sharp', 'libgdiplus', 'mono-basic']) {
	for (branchName in ['develop', 'release/mono-sil-3.4']) {
		def packageName = GetPackageName(repo, branchName)

		freeStyleJob("${repo.capitalize().replace('-', '')}_NightlyPackaging-Linux-all-${branchName.replace('/', '_').replace('-', '_')}-debug") {

			description """<p>Automatically creates the <i>$packageName</i> package from the <i>$branchName</i> branch
for testing. Can be used to manually make a package for release.</p>
<p>The job is created by the DSL plugin from <i>MonoPackagingJobs.groovy</i> script.</p>"""

			label('packager')

			logRotator(600, 25, -1, 3)

			parameters {
				stringParam("DistsToBuild", distros_tobuild,
					"The distributions to build packages for")
				stringParam("ArchesToBuild", "amd64 i386",
					"The architectures to build packages for")
				stringParam("Suite", "experimental",
					'''Area of llso to publish to. eg proposed, main, experimental. Use 'main' for
packages being tested for imminent release to pso.''')
				booleanParam("AppendNightlyToVersion", true,
					'''Create a new changelog entry with a nightly timestamp appended to version, rather than
using the version in debian/changelog. Turn off for making releasable packages.''')
				booleanParam("SimulateDput", false,
					"Don't upload packages, just build them. eg for testing.")
			}

			throttleConcurrentBuilds {
				categories(['packager-fieldworks-one-per-node'])
				maxTotal(1)
				maxPerNode(1)
			}

			multiscm {
				git {
					remote {
						url('git://gerrit.lsdev.sil.org/mono-calgary.git')
					}
					branch branchName
					extensions {
						relativeTargetDirectory('mono-calgary')
					}
				}
				git {
					remote {
						url("git://gerrit.lsdev.sil.org/${repo}.git")
					}
					branch branchName
					extensions {
						relativeTargetDirectory(repo)
					}
				}
			}

			triggers {
				scm('H H(6-10) * * *')
			}

			wrappers {
				timestamps()
				timeout {
					likelyStuck()
					abortBuild()
				}
			}

			steps {
				shell('''#!/bin/bash
# Note that although the jenkins job checks the repositories for changes, the packaging script is what updates
# its own local copy of the repository, and puts things in places it understands.

[ "$SimulateDput" = "true" ] && Simulate=--simulate-dput
[ "$AppendNightlyToVersion" = "false" ] && PreserveChangelog=--preserve-changelog

exec $HOME/FwSupportTools/packaging/build-packages --main-package-name ''' + packageName + ''' \\
    --repository-committishes "''' + "mono-calgary=$branchName,$repo=$branchName" + '''" \\
    --dists "$DistsToBuild" \\
    --arches "$ArchesToBuild" \\
    --suite-name "$Suite" \\
    $Simulate $PreserveChangelog \\
    --debkeyid 90872B06
''')
			}

			publishers {
				archiveArtifacts {
					pattern("results/*")
				}

				publishBuild {
					publishFailed(true)
					publishUnstable(true)
					discardOldBuilds(365, 20, 10, 20)
				}

				allowBrokenBuildClaiming()

				mailer(email)
			}
		}
	}
}