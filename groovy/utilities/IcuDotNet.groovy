/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */

/*
 * some common definitions for IcuDotNet related jobs
 */
package utilities
import utilities.common

class IcuDotNet {
	static void commonBuildJob(jobContext, _label) {
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
						refspec('+refs/heads/master:refs/remotes/origin/master')
					}
					branch('*/master')
				}
			}

			publishers {
				configure common.NUnitPublisher('**/TestResults.xml')
			}
		}
		common.buildPublishers(jobContext, 365, 100)
	}

	static void addGitHubPushTrigger(jobContext) {
		jobContext.with {
			triggers {
				githubPush()
			}
		}
	}

	static void commonLinuxBuildJob(jobContext) {
		commonBuildJob(jobContext, 'linux&&!packager')

		jobContext.with {
			steps {
				shell('''#!/bin/bash
ICUVER=$(icu-config --version|tr -d .|cut -c -2)

echo "Building for ICU $icu_ver"

MONO_PREFIX=/opt/mono-sil
PATH="$MONO_PREFIX/bin:$PATH"
LD_LIBRARY_PATH="$MONO_PREFIX/lib:$LD_LIBRARY_PATH"
PKG_CONFIG_PATH="$MONO_PREFIX/lib/pkgconfig:$PKG_CONFIG_PATH"
MONO_GAC_PREFIX="$MONO_PREFIX:/usr"

export LD_LIBRARY_PATH PKG_CONFIG_PATH MONO_GAC_PREFIX

xbuild /t:Test /property:BUILD_NUMBER=0.0.$BUILD_NUMBER.0 /property:icu_ver=$ICUVER /property:Configuration=ReleaseMono build/icu-dotnet.proj
''')
			}
		}
	}

	static void commonWindowsBuildJob(jobContext) {
		commonBuildJob(jobContext, 'windows && supported && timeInSync')

		jobContext.with {
			steps {
				common.addMsBuildStep(delegate, 'build\\icu-dotnet.proj', '/t:Test /property:BUILD_NUMBER=0.0.%BUILD_NUMBER%.0 /property:Configuration=Release')
			}
		}
	}
}
