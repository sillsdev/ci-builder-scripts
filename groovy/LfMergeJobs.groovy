/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
/*
 * DSL script for Jenkins LfMerge jobs
 */
import utilities.common
import utilities.LfMerge

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

def distro = 'trusty xenial'
def MinDbVersion = 7000068
def MaxDbVersion = 7000070
def MonoPrefix = '/opt/mono-sil'

// *********************************************************************************************
freeStyleJob('LfMerge_InstallDependencies-Linux-any-master-release') {
	LfMerge.generalLfMergeBuildJob(delegate, '${refspec}', '${branch}', false, false)

	description '''<p>Install dependency packages for LfMerge builds.<p>
<p>The job is created by the DSL plugin from <i>LfMergeJobs.groovy</i> script.</p>'''

	parameters {
		stringParam("branch", "master",
			"What to build, e.g. master or origin/pr/9/head")
		stringParam("refspec", "refs/heads/master",
			"Refspec to build")
	}

	// will be triggered by other jobs

	steps {
		// Install packages
		shell('''#!/bin/bash
set -e
PATH=/opt/mono-sil/bin:$PATH
debian/PrepareSource
cd build
mozroots --import --sync
./install-deps''')
	}
}

// *********************************************************************************************
for (branchName in ['master', 'live', 'qa']) {
	freeStyleJob("LfMerge-Linux-any-${branchName}-release") {
		LfMerge.commonLfMergeBuildJob(delegate, "+refs/heads/${branchName}:refs/remotes/origin/${branchName}", "*/${branchName}", true, true)

		description """<p>Linux builds of LfMerge ${branchName}.<p>
<p>The job is created by the DSL plugin from <i>LfMergeJobs.groovy</i> script.</p>"""

		triggers {
			githubPush()
		}

		steps {
			downstreamParameterized {
				trigger("LfMerge_Packaging-Linux-all-${branchName}-release") {
					if (branchName == "qa") {
						parameters {
							predefinedProp("PackageBuildKind", "ReleaseCandidate")
						}
					} else if (branchName != "master") {
						parameters {
							predefinedProp("PackageBuildKind", "Release")
						}
					}
				}
			}
		}
	}

	freeStyleJob("LfMerge_Packaging-Linux-all-${branchName}-release") {
		def revision = "\$(echo \${GIT_COMMIT} | cut -b 1-6)"

		description """<p>Continuous package builds of the LfMerge ${branchName} branch.</p>
<p>The job is created by the DSL plugin from <i>LfMergeJobs.groovy</i> script.</p>
"""

		common.defaultPackagingJob(delegate, 'lfmerge', 'lfmerge', "not used", revision,
			distro, 'eb1@sil.org', 'master', 'amd64', distro, false, '.', (branchName == "master"),
			true, false, "finalresults")

		// will be triggered by other jobs

		common.gitScm(delegate, 'https://github.com/sillsdev/LfMerge.git', "\$BranchOrTagToBuild",
			false, 'lfmerge', false, true, "", "+refs/heads/*:refs/remotes/origin/* +refs/pull/*:refs/remotes/origin/pr/*",
			true)

		steps {
			shell("""#!/bin/bash -e
cd lfmerge
# We need to set MONO_PREFIX because that's set to a mono 2.10 installation on the packaging machine!
export MONO_PREFIX=${MonoPrefix}
RUNMODE="PACKAGEBUILD" BUILD=Release . environ
xbuild /t:RestorePackages build/LfMerge.proj

mono --debug packages/GitVersion.CommandLine*/tools/GitVersion.exe -output buildserver

. gitversion.properties

if ("\${GitVersion_PreReleaseLabel}" != ""); then
	PreReleaseTag="~\${GitVersion_PreReleaseLabel}-\${GitVersion_PreReleaseNumber}"
fi

echo "PackageVersion=\${GitVersion_MajorMinorPatch}\${PreReleaseTag}" >> gitversion.properties

echo "Building packages for version \${GitVersion_MajorMinorPatch}\${PreReleaseTag}"
				""")

			environmentVariables {
				propertiesFile('lfmerge/gitversion.properties')
			}

			shell("""#!/bin/bash -e
echo "Building packages for version \$PackageVersion"

if [ "\$PackageBuildKind" = "Release" ]; then
	MAKE_SOURCE_ARGS="--preserve-changelog"
	BUILD_PACKAGE_ARGS="--no-upload"
elif [ "\$PackageBuildKind" = "ReleaseCandidate" ]; then
	MAKE_SOURCE_ARGS="--preserve-changelog"
	BUILD_PACKAGE_ARGS="--suite-name proposed"
fi

mkdir -p finalresults
rm -f finalresults/*

cd lfmerge
# We need to set MONO_PREFIX because that's set to a mono 2.10 installation on the packaging machine!
export MONO_PREFIX=${MonoPrefix}
RUNMODE="PACKAGEBUILD" BUILD=Release . environ

cd -

for ((curDbVersion=${MinDbVersion}; curDbVersion<=${MaxDbVersion}; curDbVersion++)); do
	cd lfmerge
	git clean -dxf

	xbuild /t:PrepareSource build/LfMerge.proj

	debian/PrepareSource \$curDbVersion

	\$HOME/ci-builder-scripts/bash/make-source --dists "\$DistributionsToPackage" \\
		--arches "\$ArchesToPackage" --main-package-name "lfmerge" \\
		--supported-distros "${distro}" --debkeyid \$DEBSIGNKEY \\
		--source-code-subdir "lfmerge" --package-version "\$PackageVersion" \$MAKE_SOURCE_ARGS

	\$HOME/ci-builder-scripts/bash/build-package --dists "\$DistributionsToPackage" \\
		--arches "\$ArchesToPackage" --main-package-name "lfmerge" \\
		--supported-distros "${distro}" --debkeyid \$DEBSIGNKEY \$BUILD_PACKAGE_ARGS

	cd -
	mv results/* finalresults/
done
""")

			if (branchName == "master") {
				// Last step: update lfmerge package on TeamCity build agent. 2016-05 RM
				shell('''#!/bin/bash
echo Waiting 5 minutes for package to show up on LLSO
sleep 300
ssh ba-trusty64weba sudo apt update || true
ssh ba-trusty64weba sudo apt -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold" install -y -f lfmerge || true
ssh ba-trusty64weba sudo apt -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold" install -y -f || true''')
			}
		}
	}
}

// *********************************************************************************************
freeStyleJob('LfMergeFDO_Packaging-Linux-all-lfmerge-release') {
	def revision = "\$(echo \${GIT_COMMIT} | cut -b 1-6)"
	def package_version = '--package-version "0.0.0.\${BUILD_NUMBER}" '
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
				}
				cleanAfterCheckout()
			}
		}
		git {
			remote {
				url('git://gerrit.lsdev.sil.org/FieldWorks')
				refspec("+refs/heads/*:refs/remotes/origin/*")
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
cd lfmerge-fdo
mkdir cmakebuild
cd cmakebuild
cmake -DADD_PACKAGE_LINK:BOOL=ON ../debian/
''')

		shell("""#!/bin/bash -e
export FULL_BUILD_NUMBER=0.0.\$BUILD_NUMBER.${revision}

if [ "\$PackageBuildKind" = "Release" ]; then
	MAKE_SOURCE_ARGS="--preserve-changelog"
	BUILD_PACKAGE_ARGS="--no-upload"
elif [ "\$PackageBuildKind" = "ReleaseCandidate" ]; then
	MAKE_SOURCE_ARGS="--preserve-changelog"
	BUILD_PACKAGE_ARGS="--suite-name proposed"
fi

cd "lfmerge-fdo"
\$HOME/ci-builder-scripts/bash/make-source --dists "\$DistributionsToPackage" \
	--arches "\$ArchesToPackage" \
	--main-package-name "lfmerge-fdo" \
	--supported-distros "${distro}" \
	--debkeyid \$DEBSIGNKEY \
	--main-repo-dir "fw" \
	${package_version} \
	\$MAKE_SOURCE_ARGS

\$HOME/ci-builder-scripts/bash/build-package --dists "\$DistributionsToPackage" \
	--arches "\$ArchesToPackage" \
	--main-package-name "lfmerge-fdo" \
	--supported-distros "${distro}" \
	--debkeyid \$DEBSIGNKEY \
	\$BUILD_PACKAGE_ARGS""")

	}

	common.defaultPackagingJob(delegate, 'lfmerge-fdo', 'lfmerge-fdo', package_version, revision,
		distro, 'eb1@sil.org', fwBranch, 'amd64', distro, false, 'fw', false, true, false)
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

