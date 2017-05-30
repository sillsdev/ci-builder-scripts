/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */

/*
 * some Common definitions for IcuDotNet related jobs
 */
class IcuDotNet {
	static void commonBuildJob(jobContext, _label, _refspec = '+refs/heads/master:refs/remotes/origin/master', _branch = '*/master') {
		jobContext.with {
			properties {
				priority(100)
			}

			label _label

			logRotator(365, 100)

			wrappers {
				timestamps()
				colorizeOutput()
				timeout {
					likelyStuck()
					abortBuild()
					writeDescription("Build timed out after {0} minutes")
				}
			}

			scm {
				git {
					remote {
						github("sillsdev/icu-dotnet", "https")
						refspec(_refspec)
					}
					branch(_branch)
					extensions {
						cleanAfterCheckout()
					}
				}
			}

			publishers {
				configure Common.NUnitPublisher('**/TestResults.xml')
			}
		}
		Common.buildPublishers(jobContext, 365, 100)
	}

	static void addGitHubPushTrigger(jobContext) {
		jobContext.with {
			triggers {
				githubPush()
			}
		}
	}

	static void commonLinuxBuildJob(jobContext, refspec = '+refs/heads/master:refs/remotes/origin/master', branch = '*/master') {
		// Wheezy has a too old version of ICU (48) that causes failing tests
		// GitVersion includes LibGit2Sharp, but that includes only a 64-bit version LibGit2 so
		// we can't build on 32-bit
		commonBuildJob(jobContext, 'linux64&&!packager&&!wheezy', refspec, branch)

		jobContext.with {
			steps {
				// Call Compile and TestOnly as separate targets. This works around a mono bug where the Clean
				// target deletes all files in the source tree.
				shell('''#!/bin/bash
ICUVER=$(icu-config --version|tr -d .|cut -c -2)

echo "Building for ICU $icu_ver"

MONO_PREFIX=/opt/mono4-sil
PATH="$MONO_PREFIX/bin:$PATH"
LD_LIBRARY_PATH="$MONO_PREFIX/lib:$LD_LIBRARY_PATH"
PKG_CONFIG_PATH="$MONO_PREFIX/lib/pkgconfig:$PKG_CONFIG_PATH"
MONO_GAC_PREFIX="$MONO_PREFIX:/usr"

export LD_LIBRARY_PATH PKG_CONFIG_PATH MONO_GAC_PREFIX

xbuild /t:Compile /property:Configuration=Release build/icu-dotnet.proj
xbuild /t:TestOnly/property:Configuration=Release build/icu-dotnet.proj
''')
			}
		}
	}

	static void commonWindowsBuildJob(jobContext, refspec = '+refs/heads/master:refs/remotes/origin/master', branch = '*/master', isPr = false) {
		commonBuildJob(jobContext, 'windows && supported && timeInSync', refspec, branch)

		jobContext.with {
			steps {
				Common.addMsBuildStep(delegate, 'build\\icu-dotnet.proj', '/t:Test /property:Configuration=Release')

				if (!isPr) {
					//batchFile("build\\NuGet.exe push source\\NuGetBuild\\*.nupkg -Source https://www.nuget.org/api/v2/package")
				}
			}

			if (!isPr) {
				publishers {
					archiveArtifacts {
						pattern("source\\NuGetBuild\\*.nupkg")
					}
				}
			}
		}
	}
}
