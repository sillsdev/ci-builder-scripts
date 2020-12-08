/*
 * Copyright (c) 2017 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */

/*
 * some common definitions that can be imported in other scripts
 */
class Common {
	static void defaultPackagingJob(Map args) {
		def jobContext = args.jobContext
		def packageName = args.packageName
		def subdirName = args.subdirName
		def packageVersion = args.containsKey('packageVersion') ? args.packageVersion : ''
		def revision = args.containsKey('revision') ? args.revision : ''
		def distrosToBuild = args.containsKey('distrosToBuild') ? args.distrosToBuild : 'focal bionic xenial'
		def email = args.containsKey('email') ? args.email : 'eb1@sil.org'
		def branch = args.containsKey('branch') ? args.branch : 'master'
		def archesToBuild = args.containsKey('archesToBuild') ? args.archesToBuild : 'amd64 i386'
		def supportedDistros = args.containsKey('supportedDistros') ? args.supportedDistros : 'focal bionic xenial'
		def blockDownstream = args.containsKey('blockDownstream') ? args.blockDownstream : true
		def mainRepoDir = args.containsKey('mainRepoDir') ? args.mainRepoDir : '.'
		def buildMasterBranch = args.containsKey('buildMasterBranch') ? args.buildMasterBranch : true
		def addParameters = args.containsKey('addParameters') ? args.addParameters : true
		def addSteps = args.containsKey('addSteps') ? args.addSteps : true
		def resultsDir = args.containsKey('resultsDir') ? args.resultsDir : 'results'
		def extraSourceArgs = args.containsKey('extraSourceArgs') ? args.extraSourceArgs : ''
		def extraBuildArgs = args.containsKey('extraBuildArgs') ? args.extraBuildArgs : ''
		def fullBuildNumber = args.containsKey('fullBuildNumber') ? args.fullBuildNumber : "0.0.\$BUILD_NUMBER.${revision}"
		def nodeLabel = args.containsKey('nodeLabel') ? args.nodeLabel : 'packager'

		jobContext.with {

			label(nodeLabel)

			logRotator(365, 20, 365, 10)

			if (addParameters) {
				parameters {
					stringParam("DistributionsToPackage", distrosToBuild,
						"The distributions to build packages for (separated by space)")
					stringParam("ArchesToPackage", archesToBuild,
						"The architectures to build packages for (separated by space)")
					choiceParam("PackageBuildKind",
						["Nightly", "Release", "ReleaseCandidate"],
						"What kind of build is this? A nightly build will have a version suffix like +nightly2016... appended, a release (or a release candidate) will just have the version number.")
					stringParam("BranchOrTagToBuild", "refs/heads/$branch",
						"What branch/tag to build? (examples: refs/heads/master, refs/tags/v3.1, origin/pr/9/head)")
				}
			}

			wrappers {
				timestamps()
				colorizeOutput()
			}

			if (addSteps) {
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

rm -f ${resultsDir}/*
rm -f ${subdirName}_*

cd "${subdirName}"
\$HOME/ci-builder-scripts/bash/make-source --dists "\$DistributionsToPackage" \
	--arches "\$ArchesToPackage" \
	--main-package-name "${packageName}" \
	--supported-distros "${supportedDistros}" \
	--debkeyid \$DEBSIGNKEY \
	--main-repo-dir ${mainRepoDir} \
	${packageVersion} \
	${extraSourceArgs} \
	\$MAKE_SOURCE_ARGS

\$HOME/ci-builder-scripts/bash/build-package --dists "\$DistributionsToPackage" \
	--arches "\$ArchesToPackage" \
	--main-package-name "${packageName}" \
	--supported-distros "${supportedDistros}" \
	--debkeyid \$DEBSIGNKEY \
	${extraBuildArgs} \
	\$BUILD_PACKAGE_ARGS

RESULT=\$?
cd \$WORKSPACE
mv ${subdirName}_* ${resultsDir}

exit \$RESULT""")
				}
			}

			publishers {
				archiveArtifacts {
					pattern("${resultsDir}/*")
					allowEmpty(true)
				}

				allowBrokenBuildClaiming()

				mailer(email)

				if (buildMasterBranch) {
					flexiblePublish {
						conditionalAction {
							condition {
								not {
									stringsMatch("\$BranchOrTagToBuild", "refs/heads/$branch", false)
								}
							}
							steps {
								downstreamParameterized {
									trigger(jobContext.name) {
										if (blockDownstream) {
											block {
												buildStepFailure('FAILURE')
												failure('FAILURE')
												unstable('UNSTABLE')
											}
										}
										parameters {
											predefinedProp("BranchOrTagToBuild", "refs/heads/$branch")
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/*
	`onlyTriggerFileSpec` is a java regular expression
	(http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html)
	e.g. "testproject/testprojectTests/.*"
	*/
	static void gitScm(jobContext, url_, branch_, createTag_ = false, subdir = "",
		disableSubmodules_ = false, commitAuthorInChangelog_ = false, scmName_ = "",
		refspec_ = "", clean_ = false, credentials_ = "", fetchTags = true,
		onlyTriggerFileSpec = "", githubRepo = "") {
		jobContext.with {
			scm {
				git {
					remote {
						if (scmName_ != "") {
							name(scmName_)
						}
						if (githubRepo != "") {
							github(githubRepo, "https")
						} else {
							url(url_)
						}
						if (refspec_ != "") {
							refspec(refspec_)
						}
						if (credentials_ != "") {
							credentials(credentials_)
						}
					}
					branch(branch_)
					extensions {
						if (createTag_) {
							perBuildTag()
						}
						if (subdir != "") {
							relativeTargetDirectory(subdir)
						}
						if (clean_) {
							cleanAfterCheckout()
						}
						if (disableSubmodules_) {
							submoduleOptions {
								disable(disableSubmodules_)
								/*recursive(false)
								tracking(false) */
							}
						}
						if (fetchTags) {
							cloneOptions {
								noTags(!fetchTags)
							}
						}
						if (onlyTriggerFileSpec != "") {
							disableRemotePoll()
						}
					}

					if (commitAuthorInChangelog_ || onlyTriggerFileSpec != "") {
						configure { node ->
							if (commitAuthorInChangelog_) {
								node / 'extensions' / 'hudson.plugins.git.extensions.impl.AuthorInChangelog'
							}
							if (onlyTriggerFileSpec != "") {
								node / 'extensions' / 'hudson.plugins.git.extensions.impl.PathRestriction' {
									includedRegions onlyTriggerFileSpec
								}
							}
						}
					}
				}
			}
		}
	}

	static void hgScm(jobContext, project, branch, subdir_name) {
		jobContext.with {
			scm {
				hg(project, branch) { node ->
					node / clean('true')
					node / subdir(subdir_name)
				}
			}
		}
	}

	static void buildPublishers(jobContext, daysToKeepVal = 365, numToKeepVal = 20, artifactDaysToKeepVal = 10, artifactNumToKeepVal = 20) {
		jobContext.with {
			configure { project ->
				project / 'publishers' << 'hudson.plugins.build__publisher.BuildPublisher' {
					publishUnstableBuilds(true)
					publishFailedBuilds(true)
					logRotator {
						daysToKeep daysToKeepVal
						numToKeep numToKeepVal
						artifactDaysToKeep artifactDaysToKeepVal
						artifactNumToKeep artifactNumToKeepVal
					}
				}
			}
		}
	}

	static void addInstallPackagesBuildStep(stepContext, scriptName = 'build/install-deps') {
		stepContext.with {
			shell("""#!/bin/bash -e
$scriptName""")
		}
	}

	static void addXbuildBuildStep(stepContext, projFile, cmdArgs = "") {
		stepContext.with {
			shell("""#!/bin/bash -e
. ./environ
xbuild $cmdArgs $projFile""")
		}
	}

	static void addGetDependenciesBuildStep(stepContext, scriptName = 'build/getDependencies-Linux.sh') {
		stepContext.with {
			shell("""#!/bin/bash -e
echo "Fetching dependencies"
$scriptName
""")
		}
	}

	static void addGetDependenciesWindowsBuildStep(stepContext, scriptName = 'build/getDependencies-windows.sh') {
		stepContext.with {
			batchFile("""SET TEMP=%HOME%\\\\tmp
SET TMP=%TEMP%
IF NOT EXIST %TEMP% mkdir %TEMP%
echo which mkdir > %TEMP%\\\\%BUILD_TAG%.txt
echo $scriptName >> %TEMP%\\\\%BUILD_TAG%.txt
"c:\\\\Program Files (x86)\\\\Git\\\\bin\\\\bash.exe" --login -i < %TEMP%\\\\%BUILD_TAG%.txt
""")
		}
	}

	static void addRunUnitTestsLinuxBuildStep(stepContext, testDll) {
		stepContext.with {
			shell('''#!/bin/bash
set -e
. ./environ
dbus-launch --exit-with-session
cd output/Debug
mono --debug ../../packages/NUnit.Runners.Net4.2.6.4/tools/nunit-console.exe -apartment=STA -nothread \
-labels -xml=''' + testDll + '.results.xml ' + testDll + ''' || true
exit 0
			''')
		}
	}

	static void addRunUnitTestsWindowsBuildStep(stepContext, testDll) {
		stepContext.with {
			batchFile('packages\\NUnit.Runners.Net4.2.6.4\\tools\\nunit-console-x86.exe -exclude=RequiresUI -xml=output\\Debug\\' +
				testDll + '.results.xml output\\Debug\\' + testDll + '''
exit 0
			''')
		}
	}

	static void addCopyArtifactsWindowsBuildStep(stepContext) {
		stepContext.with {
			batchFile('xcopy /q /e /s /r /h /y %HOME%\\archive\\%ARTIFACTS_TAG%\\*.* .')
		}
	}

	static void addMagicAggregationFile(stepContext) {
		stepContext.with {
			shell('''
# this is needed so that upstream aggregation of unit tests works
echo -n ${UPSTREAM_BUILD_TAG} > ${WORKSPACE}/magic.txt
''')
		}
	}

	static void addMagicAggregationFileWindows(stepContext) {
		stepContext.with {
			batchFile('''
REM This is needed so that upstream aggregation of test results works
echo %UPSTREAM_BUILD_TAG% > %WORKSPACE%\\magic.txt
''')
		}
	}

	static void addTriggerDownstreamBuildStep(stepContext, projects, predefProps = null) {
		stepContext.with {
			downstreamParameterized {
				trigger(projects) {
					block {
						buildStepFailure('FAILURE')
						failure('FAILURE')
						unstable('UNSTABLE')
					}
					parameters {
						currentBuild()
						if (predefProps != null)
							predefinedProps(predefProps)
					}
				}
			}
		}
	}

	static void addMsBuildStep(stepContext, projFile, cmdArgs = "", installation = '.NET 4.0') {
		stepContext.with {
			msBuild {
				msBuildInstallation(installation)
				buildFile(projFile)
				args(cmdArgs)
				passBuildVariables(false)
				continueOnBuildFailure(false)
				unstableIfWarnings(false)
			}
		}
	}

	static void addXvfbBuildWrapper(wrapperContext) {
		wrapperContext.with {
			xvfb('default') {
				screen('1024x768x24')
				displayNameOffset(1)
			}
		}
	}

	static Closure NUnitPublisher(results) {
		return { project ->
			project / 'publishers' << 'hudson.plugins.nunit.NUnitPublisher' {
				testResultsPattern(results)
			}
		}
	}

	static void addNUnitPublisher(publisherContext, results, failIfNotPresent = true, failedThresholdsClosure = null, skippedThresholdsClosure = null, useNUnit3 = false) {
		publisherContext.with {
			archiveXUnit {
				if (useNUnit3) {
					nUnit3 {
						pattern(results)
						skipNoTestFiles(!failIfNotPresent)
					}
				} else {
					nUnit {
						pattern(results)
						skipNoTestFiles(!failIfNotPresent)
					}
				}
				if (failedThresholdsClosure) {
					failedThresholdsClosure()
				} else {
					failedThresholds {
						failure(999999)
						failureNew(999999)
						unstable(0)
						unstableNew(0)
					}
				}
				if (skippedThresholdsClosure) {
					skippedThresholdsClosure()
				} else {
					skippedThresholds {
						failure(999999)
						failureNew(999999)
						unstable(999999)
						unstableNew(999999)
					}
				}
			}
		}
	}

	static void addGitHubParamAndTrigger(jobContext, branch, os = 'linux', whitelistArgs = '') {
		jobContext.with {
			parameters {
				stringParam("sha1", "refs/heads/${branch}",
					"What pull request to build, e.g. pr/9/merge")
			}

			triggers {
				githubPullRequest {
					admin('ermshiperete')
					useGitHubHooks(true)
					orgWhitelist('sillsdev')
					if (whitelistArgs != '') {
						userWhitelist(whitelistArgs)
					}
					cron('H/5 * * * *')
					allowMembersOfWhitelistedOrgsAsAdmin()
					displayBuildErrorsOnDownstreamBuilds(true)
					whiteListTargetBranches([ branch ])
					extensions {
						commitStatus {
							context("continuous-integration/jenkins-${os}")
						}
					}
				}
			}
		}
	}

	static void addBuildNumber(stepContext, envVariableName) {
		stepContext.with {
			systemGroovyCommand("""
def build = Thread.currentThread().executable
assert build
def buildVersion = build.getEnvironment().get("${envVariableName}")
try {
	if (buildVersion)
		build.displayName = buildVersion
	println "Build display name is set to \${buildVersion}"
} catch (MissingPropertyException e) {}
			""")
		}
	}
}
