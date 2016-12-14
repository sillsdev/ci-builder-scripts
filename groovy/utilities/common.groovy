/*
 * some common definitions that can be imported in other scripts
 */
package utilities

import utilities.Helper

class common {
    static void defaultPackagingJob(jobContext, packagename, subdir_name,
        package_version = "",
        revision = "",
        distros_tobuild = "precise trusty xenial",
        email = "eb1@sil.org",
        branch = "master",
        arches_tobuild = "amd64 i386",
        supported_distros = "precise trusty utopic vivid wily xenial wheezy jessie",
        blockDownstream = true,
        mainRepoDir = '.',
        buildMasterBranch = true,
        addParameters = true,
        addSteps = true,
        resultsDir = "results") {

        jobContext.with {

            label('packager')

            logRotator(365, 20, 10, 10)

            if (addParameters) {
                parameters {
                    stringParam("DistributionsToPackage", distros_tobuild,
                        "The distributions to build packages for (separated by space)")
                    stringParam("ArchesToPackage", arches_tobuild,
                        "The architectures to build packages for (separated by space)")
                    choiceParam("PackageBuildKind",
                        ["Nightly", "Release"],
                        "What kind of build is this? A nightly build will have the prefix +nightly2016... appended, a release will just have the version number.")
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
export FULL_BUILD_NUMBER=0.0.\$BUILD_NUMBER.${revision}

if [ "\$PackageBuildKind" = "Release" ]; then
    MAKE_SOURCE_ARGS="--preserve-changelog"
    BUILD_PACKAGE_ARGS="--no-upload"
fi

cd "${subdir_name}"
\$HOME/ci-builder-scripts/bash/make-source --dists "\$DistributionsToPackage" \
    --arches "\$ArchesToPackage" \
    --main-package-name "${packagename}" \
    --supported-distros "${supported_distros}" \
    --debkeyid \$DEBSIGNKEY \
    --main-repo-dir ${mainRepoDir} \
    ${package_version} \
    \$MAKE_SOURCE_ARGS

\$HOME/ci-builder-scripts/bash/build-package --dists "\$DistributionsToPackage" \
    --arches "\$ArchesToPackage" \
    --main-package-name "${packagename}" \
    --supported-distros "${supported_distros}" \
    --debkeyid \$DEBSIGNKEY \
    \$BUILD_PACKAGE_ARGS""")
                }
            }

            publishers {
                archiveArtifacts {
                    pattern("${resultsDir}/*, ${subdir_name}*")
                    allowEmpty(true)
                }

                publishBuild {
                    publishFailed(true)
                    publishUnstable(true)
                    discardOldBuilds(365, 20, 10, 20)
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

    static void gitScm(jobContext, url_, branch_, createTag_ = false, subdir = "",
        disableSubmodules_ = false, commitAuthorInChangelog_ = false, scmName_ = "",
        refspec_ = "", clean_ = false) {
        jobContext.with {
            scm {
                git {
                    remote {
                        if (scmName_ != "") {
                            name(scmName_)
                        }
                        url(url_)
                        if (refspec_ != "") {
                            refspec(refspec_)
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
                    }

                    if (commitAuthorInChangelog_) {
                        configure { node ->
                            if (commitAuthorInChangelog_) {
                                node / 'extensions' / 'hudson.plugins.git.extensions.impl.AuthorInChangelog'
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

    static void addInstallPackagesBuildStep(stepContext) {
        stepContext.with {
            shell('''#!/bin/bash
set -e
cd build
mozroots --import --sync
./install-deps''')
        }
    }

    static void addXbuildBuildStep(stepContext, projFile, cmdArgs = "") {
        stepContext.with {
            shell("""#!/bin/bash
set -e
. ./environ
xbuild $cmdArgs $projFile""")
        }
    }

    static void addGetDependenciesBuildStep(stepContext) {
        stepContext.with {
            shell('''#!/bin/bash
set -e
echo "Fetching dependencies"
cd build
./getDependencies-Linux.sh
''')
        }
    }

    static void addGetDependenciesWindowsBuildStep(stepContext, scriptName = 'build/getDependencies-windows.sh') {
        def build_script = '''SET TEMP=%HOME%\\\\tmp
SET TMP=%TEMP%
IF NOT EXIST %TEMP% mkdir %TEMP%
echo which mkdir > %TEMP%\\\\%BUILD_TAG%.txt
echo @@{scriptName} >> %TEMP%\\\\%BUILD_TAG%.txt
"c:\\\\Program Files (x86)\\\\Git\\\\bin\\\\bash.exe" --login -i < %TEMP%\\\\%BUILD_TAG%.txt
'''

        stepContext.with {
            def values = [ 'scriptName' : scriptName ]

            batchFile(Helper.prepareScript(build_script, values))
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

	static void addGitHubParamAndTrigger(jobContext, branch, os = 'linux', whitelistArgs = '') {
		jobContext.with {
			parameters {
				stringParam("sha1", "refs/heads/master",
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
							context("continuous-integration/jenkins-$os")
						}
					}
				}
			}
		}
	}
}
