/*
 * Copyright (c) 2018-2020 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
//#include utilities/Common.groovy

// Variables
final distros_tobuild = "xenial bionic"
final email_recipients = 'eb1@sil.org marc_durdin@sil.org dglassey@gmail.com darcy_wong@sil.org'

final revision = "\$(echo \${GIT_COMMIT} | cut -b 1-6)"
final fullBuildNumber="0.0.0+\$BUILD_NUMBER"

final onboard_repo = 'git://github.com/keymanapp/onboard-keyman.git'
final branch = 'keymankb'

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

freeStyleJob("Keyman_Packaging-Linux-onboard-keyman-${branch}") {
	mainRepoDir = '.'
	packagename = "onboard"
	Common.defaultPackagingJob(delegate, "onboard-keyman", "", "", revision,
		distros_tobuild, email_recipients, branch, "amd64 i386", distros_tobuild, true, mainRepoDir,
		/* buildMasterBranch: */ false, /* addParameters */ true, /* addSteps */ false,
		/* resultsDir: */ "results", /* extraSourceArgs: */ "",
		/* extraBuildArgs: */ '', /* fullBuildNumber: */ fullBuildNumber,
		/* nodeLabel: */ 'packager && bionic')

	description """
<p>Automatic builds of the Onboard Keyboard for Linux ${branch} branch.</p>
<p>The job is created by the DSL plugin from <i>KeymanPackagingJobs.groovy</i> script.</p>
"""

	// TriggerToken needs to be set in the seed job! Note that we use the
	// `binding.variables.*` notation so that it works when we build the tests.
	authenticationToken(binding.variables.TriggerToken)

	triggers {
		githubPush()
	}

	Common.gitScm(delegate, /*url*/ onboard_repo, /*branch*/"\$BranchOrTagToBuild",
			/*createTag*/ false, /*subdir*/ "onboard-keyman", /*disableSubmodules*/ false,
			/*commitAuthorInChangelog*/ true, /*scmName*/ "", /*refspec*/ "",
			/*clean*/ false, /*credentials*/ "", /*fetchTags*/ true,
			/*onlyTriggerFileSpec*/ "",
			/*githubRepo*/ "keymanapp/onboard-keyman")

	wrappers {
		timeout {
			elastic(300, 3, 120)
			abortBuild()
			writeDescription("Build timed out after {0} minutes")
		}
	}

	steps {
		shell("""#!/bin/bash
export FULL_BUILD_NUMBER=${fullBuildNumber}

if [ "\$PackageBuildKind" = "Release" ]; then
	MAKE_SOURCE_ARGS="--preserve-changelog"
	BUILD_PACKAGE_ARGS="--suite-name main"
elif [ "\$PackageBuildKind" = "ReleaseCandidate" ]; then
	MAKE_SOURCE_ARGS="--preserve-changelog"
	BUILD_PACKAGE_ARGS="--suite-name proposed"
fi

# get orig.tar.gz
onboard_version=`dpkg-parsechangelog -l onboard-keyman/debian/changelog --show-field=Version | cut -d '-' -f 1`
wget http://deb.debian.org/debian/pool/main/o/onboard/onboard_\${onboard_version}.orig.tar.gz
cd onboard-keyman

# make source package
rm -rf onboard_*.{dsc,build,buildinfo,changes,tar.?z,log}
dgit -wg build-source
cp ../onboard_*.{dsc,build,buildinfo,changes,tar.?z,log} .
for file in `ls *.dsc`; do log "Signing source package \$file"; debsign -k\$DEBSIGNKEY \$file; done

\$HOME/ci-builder-scripts/bash/build-package --dists "\$DistributionsToPackage" \
	--arches "\$ArchesToPackage" \
	--main-package-name "onboard" \
	--supported-distros "${distros_tobuild}" \
	--debkeyid \$DEBSIGNKEY \
	--build-in-place \
	\$BUILD_PACKAGE_ARGS

buildret="\$?"

if [ "\$buildret" == "0" ]; then echo "PackageVersion=\$(dpkg-parsechangelog --show-field=Version)" > ../${packagename}-packageversion.properties; fi

exit \$buildret
""")

		environmentVariables {
			propertiesFile("${packagename}-packageversion.properties")
		}

		Common.addBuildNumber(delegate, 'PackageVersion')
	}
}

// Packaging jobs with Pipeline
multibranchPipelineJob('pipeline-keyman-packaging') {
	description """<p>Packaging builds of Keyman</p>
<p>The job is created by the DSL plugin from <i>KeymanPackagingJobs.groovy</i> script.</p>"""

	factory {
		workflowBranchProjectFactory {
			scriptPath('linux/Jenkinsfile')
		}
	}

	branchSources {
		branchSource {
			// see https://stackoverflow.com/a/56291979/487503
			source {
				github {
					id('keyman')
					repoOwner('keymanapp')
					repository('keyman')
					repositoryUrl('https://github.com/keymanapp/keyman.git')
					configuredByUrl(true)
					credentialsId('github-sillsdevgerrit')
					traits {
						gitHubBranchDiscovery {
							strategyId(1) // Exclude branches that are also filed as PRs
						}
						gitHubPullRequestDiscovery() {
							strategyId(1) // Merging the pull request with the current target branch revision
						}
						/* https://issues.jenkins-ci.org/browse/JENKINS-60874
						gitHubForkDiscovery() {
							strategyId(1) // Merging the pull request with the current target branch revision
							trust(gitHubTrustPermissions())
						}
						*/
						headWildcardFilter {
							includes('master beta PR-*')
							excludes('')
						}
						disableStatusUpdateTrait()
					}
				}
			}
		}
	}

	configure {
		def traits = it / 'sources' / 'data' / 'jenkins.branch.BranchSource' / 'source' / 'traits'

		traits << 'org.jenkinsci.plugins.github__branch__source.ForkPullRequestDiscoveryTrait' {
			strategyId(1)
			trust(class: 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait$TrustPermission')
		}
	}

	orphanedItemStrategy {
		discardOldItems {
			daysToKeep(60)
			numToKeep(10)
		}
	}

	triggers {
		// check once a day if not otherwise run
		periodicFolderTrigger {
			interval('1d')
		}
	}
}
