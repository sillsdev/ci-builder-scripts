/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
/*
 * DSL script for Jenkins LfMerge jobs
 */
//#include utilities/Common.groovy
//#include utilities/LfMerge.groovy

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

def distro = 'xenial'
def buildAgent = 'ba-xenial-web-s2-138'

// *********************************************************************************************
for (prefix in [ '', 'fieldworks8-']) {
	def MinDbVersion = prefix == '' ? 7000072 : 7000068
	def MaxDbVersion = prefix == '' ? 7000072 : 7000070
	def MonoPrefix = prefix == '' ? '/opt/mono5-sil' : '/opt/mono-sil'
	def MonoPrefixForPackaging = prefix == '' ? '/opt/mono5-sil' : '/opt/mono4-sil'
	def msbuild = prefix == '' ? 'msbuild' : 'xbuild'

	freeStyleJob("LfMerge_InstallDependencies-Linux-any-${prefix}master-release") {
		LfMerge.generalLfMergeBuildJob(delegate, '${refspec}', '${branch}', false, false)

		description '''<p>Install dependency packages for LfMerge builds.<p>
<p>The job is created by the DSL plugin from <i>LfMergeJobs.groovy</i> script.</p>'''

		parameters {
			stringParam("branch", "${prefix}master",
				"What to build, e.g. master or origin/pr/9/head")
			stringParam("refspec", "refs/heads/${prefix}master",
				"Refspec to build")
		}

		// will be triggered by other jobs

		steps {
			// Install packages
			shell("""#!/bin/bash
set -e
PATH=${MonoPrefix}/bin:\$PATH
debian/PrepareSource
cd build
mozroots --import --sync
./install-deps""")
		}
	}

	// *****************************************************************************************
	def branchName = prefix + 'master'
	freeStyleJob("GitHub-LfMerge-Linux-any-${branchName}-release") {
		LfMerge.commonLfMergeBuildJob(delegate, '+refs/pull/*:refs/remotes/origin/pr/*', '${sha1}',
			/* useTimeout: */ true, /* addLanguageForge: */ true, /* isPR: */ true,
			/* branchName: */ branchName, /* prefix: */ prefix, /* msbuild: */ msbuild)

		description """<p>Pre-merge Linux builds of ${branchName} branch. Triggered by creating a PR on GitHub.<p>
<p>The job is created by the DSL plugin from <i>LfMergeGitHubJobs.groovy</i> script.</p>"""

		Common.addGitHubParamAndTrigger(delegate, branchName)
	}

	// *****************************************************************************************
	for (branchNameSuffix in ['master', 'live', 'qa']) {
		branchName = prefix + branchNameSuffix
		freeStyleJob("LfMerge-Linux-any-${branchName}-release") {
			LfMerge.commonLfMergeBuildJob(delegate,
				/* spec: */ "+refs/heads/${branchName}:refs/remotes/origin/${branchName}",
				/* sha1: */ "refs/remotes/origin/${branchName}", /* useTimeout: */ true, /* addLanguageForge: */ true,
				/* isPr: */ false, /* branchName: */ branchName, /* prefix: */ prefix,
				/* msbuild: */ msbuild)

			description """<p>Linux builds of LfMerge ${branchName}.<p>
<p>The job is created by the DSL plugin from <i>LfMergeJobs.groovy</i> script.</p>"""

			triggers {
				githubPush()
			}

			steps {
				downstreamParameterized {
					trigger("LfMerge_Packaging-Linux-all-${branchName}-release")
				}
			}
		}

		freeStyleJob("LfMerge_Packaging-Linux-all-${branchName}-release") {
			def revision = "\$(echo \${GIT_COMMIT} | cut -b 1-6)"

			if (branchNameSuffix == "live") {
				BuildPackageArgs = "--suite-name main"
				AdditionalDescription = "(Uploaded to main section of llso)"
			} else if (branchNameSuffix == "qa") {
				BuildPackageArgs = "--suite-name proposed"
				AdditionalDescription = "(Uploaded to -proposed section of llso)"
			} else {
				BuildPackageArgs = ""
				AdditionalDescription = "(Uploaded to -experimental section of llso)"
			}

			description """<p>Package builds of the LfMerge ${branchName} branch ${AdditionalDescription}.</p>
<p>The job is created by the DSL plugin from <i>LfMergeJobs.groovy</i> script.</p>
"""

			parameters {
				stringParam("DistributionsToPackage", distro,
					"The distributions to build packages for (separated by space)")
			}

			Common.defaultPackagingJob(delegate, 'lfmerge', 'lfmerge', "not used", revision,
				distro, 'eb1@sil.org', branchName, 'amd64', distro, false, '.', false,
				false, false, "finalresults")

			// will be triggered by other jobs

			Common.gitScm(delegate, 'https://github.com/sillsdev/LfMerge.git', "refs/heads/${branchName}",
				false, 'lfmerge', false, true, "", "+refs/heads/*:refs/remotes/origin/* +refs/pull/*:refs/remotes/origin/pr/*",
				true, 'github-sillsdevgerrit')

			steps {
				shell("""#!/bin/bash -e
cd lfmerge
# We need to set MONO_PREFIX because that's set to a mono 2.10 installation on the packaging machine!
export MONO_PREFIX=${MonoPrefixForPackaging}
RUNMODE="PACKAGEBUILD" BUILD=Release . environ
${msbuild} /t:RestoreBuildTasks build/LfMerge.proj
mkdir -p output/Release

if [ -f packages/GitVersion.CommandLine/tools/GitVersion.exe ]; then
	mono --debug packages/GitVersion.CommandLine/tools/GitVersion.exe -output buildserver
else
	mono --debug packages/GitVersion.CommandLine*/tools/GitVersion.exe -output buildserver
fi

. gitversion.properties

if [ "\${GitVersion_PreReleaseLabel}" != "" ]; then
	PreReleaseTag="~\${GitVersion_PreReleaseLabel}.\${GitVersion_PreReleaseNumber}"
fi

echo "PackageVersion=\${GitVersion_MajorMinorPatch}\${PreReleaseTag}.\${BUILD_NUMBER}" >> gitversion.properties

echo "Building packages for version \${GitVersion_MajorMinorPatch}\${PreReleaseTag}.\${BUILD_NUMBER}"
					""")

				environmentVariables {
					propertiesFile('lfmerge/gitversion.properties')
				}

				Common.addBuildNumber(delegate, 'PackageVersion')

				shell("""#!/bin/bash -e
echo -e "\\033[0;34mBuilding packages for version \${PackageVersion}\\033[0m"

TRACE()
{
	echo \$@
	\$@
}

mkdir -p finalresults
rm -f finalresults/*

cd lfmerge
# We need to set MONO_PREFIX because that's set to a mono 2.10 installation on the packaging machine!
export MONO_PREFIX=${MonoPrefixForPackaging}
RUNMODE="PACKAGEBUILD" BUILD=Release . environ

cd -

for ((curDbVersion=${MinDbVersion}; curDbVersion<=${MaxDbVersion}; curDbVersion++)); do
	echo -e "\\033[0;34mBuilding package for database version \${curDbVersion}\\033[0m"
	cd lfmerge
	git clean -dxf --exclude=packages/
	git reset --hard

	echo -e "\\033[0;34mPrepare source\\033[0m"
	TRACE ${msbuild} /t:PrepareSource /v:detailed build/LfMerge.proj

	TRACE debian/PrepareSource \$curDbVersion

	echo -e "\\033[0;34mBuild source package\\033[0m"
	TRACE \$HOME/ci-builder-scripts/bash/make-source --dists "\$DistributionsToPackage" \\
		--arches "amd64" --main-package-name "lfmerge" \\
		--supported-distros "${distro}" --debkeyid \$DEBSIGNKEY \\
		--source-code-subdir "lfmerge" --package-version "\$PackageVersion" --preserve-changelog

	echo -e "\\033[0;34mBuild binary package\\033[0m"
	TRACE \$HOME/ci-builder-scripts/bash/build-package --dists "\$DistributionsToPackage" \\
		--arches "amd64" --main-package-name "lfmerge" \\
		--supported-distros "${distro}" --debkeyid \$DEBSIGNKEY ${BuildPackageArgs}

	cd -
	mv results/* finalresults/
done
""")
			}

			if (branchNameSuffix == "live") {
				publishers {
					git {
						pushOnlyIfSuccess()
						tag('origin', 'v$PackageVersion') {
							message('Version $PackageVersion')
							create()
							update()
						}
					}
				}
			}
		}
	}
}

// ***********************************************************************************************
freeStyleJob('LfMergeFDO_Packaging-Linux-all-lfmerge-release') {
	def revision = "\$(echo \${GIT_COMMIT} | cut -b 1-6)"
	def fwBranch = 'feature/lfmerge'
	def debianBranch = 'feature/lfmerge'
	def libcomBranch = 'develop'

	description '''
<p>Package builds of the <b>lfmerge-fdo</b> package.</p>
<p>The job is created by the DSL plugin from <i>LfMergeJobs.groovy</i> script.</p>
'''

	multiscm {
		git {
			remote {
				url('git://gerrit.lsdev.sil.org/libcom')
				refspec("+refs/heads/${libcomBranch}:refs/remotes/origin/${libcomBranch}")
			}
			branch libcomBranch
			extensions {
				relativeTargetDirectory('lfmerge-fdo/libcom')
				submoduleOptions {
					recursive(true)
				}
				cloneOptions {
					shallow(true)
					timeout(30)
					noTags(false)
				}
				cleanAfterCheckout()
			}
		}
		git {
			remote {
				url('git://gerrit.lsdev.sil.org/FwDebian')
				refspec("+refs/heads/${debianBranch}:refs/remotes/origin/${debianBranch}")
			}
			branch debianBranch
			extensions {
				relativeTargetDirectory('lfmerge-fdo/debian')
				submoduleOptions {
					recursive(true)
				}
				cloneOptions {
					shallow(true)
					timeout(30)
					noTags(false)
				}
				cleanAfterCheckout()
			}
		}
		git {
			remote {
				name('fw')
				url('ssh://jenkins@gerrit.lsdev.sil.org:59418/FieldWorks')
				refspec("+refs/heads/*:refs/remotes/fw/*")
				credentials('f130e507-4ea0-49a6-8d3a-274485e4ee1a')
			}
			branch '$BranchOrTagToBuild'
			extensions {
				relativeTargetDirectory('lfmerge-fdo/fw')
				submoduleOptions {
					recursive(true)
				}
				cloneOptions {
					shallow(true)
					timeout(30)
					noTags(false)
				}
				cleanAfterCheckout()
			}
		}
	}

	triggers {
		gerrit {
			events {
				refUpdated()
			}
			project('FieldWorks', "ant:*${fwBranch}")
			project('FwDebian', "ant:*${debianBranch}")
			project('libcom', "ant:*${libcomBranch}")
		}
	}

	steps {
		shell('''#!/bin/bash -e
cd lfmerge-fdo/fw
. Src/MasterVersionInfo.txt 2>/dev/null || true

echo "PackageVersion=$FWMAJOR.$FWMINOR.$FWREVISION.$BUILD_NUMBER" > ../../lfmerge-fdo-version.properties
echo "Setting version to $FWMAJOR.$FWMINOR.$FWREVISION.$BUILD_NUMBER"
''')

		environmentVariables {
			propertiesFile('lfmerge-fdo-version.properties')
		}

		Common.addBuildNumber(delegate, 'PackageVersion')

		shell('''#!/bin/bash -e
# Remove old packages
find . -maxdepth 1 -type f -print0 | xargs -0 rm

cd lfmerge-fdo
mkdir -p cmakebuild
cd cmakebuild
cmake -DADD_PACKAGE_LINK:BOOL=ON ../debian/
cd ..
rm -rf cmakebuild
rm -rf fw/Lib/src/Enchant/fieldworks-enchant-1.6.1
''')

		shell("""#!/bin/bash -e
export FULL_BUILD_NUMBER=\$PackageVersion

if [ "\$PackageBuildKind" = "Release" ]; then
	BUILD_PACKAGE_ARGS="--no-upload"
elif [ "\$PackageBuildKind" = "ReleaseCandidate" ]; then
	BUILD_PACKAGE_ARGS="--suite-name proposed"
fi

cd "lfmerge-fdo"
\$HOME/ci-builder-scripts/bash/make-source --dists "\$DistributionsToPackage" \
	--arches "\$ArchesToPackage" \
	--main-package-name "lfmerge-fdo" \
	--supported-distros "${distro}" \
	--debkeyid \$DEBSIGNKEY \
	--main-repo-dir "fw" \
	--package-version "\$PackageVersion" \
	--preserve-changelog

\$HOME/ci-builder-scripts/bash/build-package --dists "\$DistributionsToPackage" \
	--arches "\$ArchesToPackage" \
	--main-package-name "lfmerge-fdo" \
	--supported-distros "${distro}" \
	--debkeyid \$DEBSIGNKEY \
	\$BUILD_PACKAGE_ARGS

echo "Successfully build package" """)
	}

	Common.defaultPackagingJob(delegate, 'lfmerge-fdo', 'lfmerge-fdo', 'unused', revision,
		distro, 'eb1@sil.org', fwBranch, 'amd64', distro, false, 'fw', false, true, false)

	publishers {
		flexiblePublish {
			conditionalAction {
				condition {
					stringsMatch('$PackageBuildKind', 'Release', true)
				}
				publishers {
					buildDescription("Successfully build package", "<span style=\"background-color:yellow\">lfmerge-fdo \$PackageVersion</span>")
				}
			}
		}
	}
}

// *********************************************************************************************
multibranchPipelineJob('lfmerge') {
	description '''<p>Builds of LfMerge</p>
<p>The job is created by the DSL plugin from <i>LfMergeJobs.groovy</i> script.</p>'''

	branchSources {
		git {
			remote('https://github.com/sillsdev/LfMerge')
			credentialsId('github-sillsdev')
			excludes('tags/*')
		}

		orphanedItemStrategy {
			discardOldItems {
				numToKeep(10)
			}
		}

		triggers {
			// check once a day if not otherwise run
			periodic(1440)
		}
	}
}
