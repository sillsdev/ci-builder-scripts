/*
 * some common definitions that can be imported in other scripts
 */
package utilities;
import utilities.Helper;

class common {
    static void defaultPackagingJob(jobContext, packagename, subdir_name,
        package_version = "",
        revision = "",
        distros_tobuild = "precise trusty",
        arches_tobuild = "amd64 i386",
        supported_distros = "precise trusty utopic wheezy jessie") {
        /*
         * Definition of build step scripts
         */

        // Remember: this is a dash, not a bash, script!
        def build_script = '''
export FULL_BUILD_NUMBER=0.0.$BUILD_NUMBER.@@{revision}

if [ "$PackageBuildKind" = "Release" ]; then
    RELEASE_PACKAGE="--preserve-changelog"
fi

cd "@@{subdir_name}"
$HOME/ci-builder-scripts/bash/make-source --dists "$DistributionsToPackage" \
    --arches "$ArchesToPackage" \
    --main-package-name "@@{packagename}" \
    --supported-distros "@@{supported_distros}" \
    --debkeyid $DEBSIGNKEY \
    @@{package_version} \
    $RELEASE_PACKAGE

$HOME/ci-builder-scripts/bash/build-package --dists "$DistributionsToPackage" \
    --arches "$ArchesToPackage" \
    --main-package-name "@@{packagename}" \
    --supported-distros "@@{supported_distros}" \
    --debkeyid $DEBSIGNKEY \
    --destination-repository $RepoSection
        '''

        /*
         * Definition of jobs
         */

        jobContext.with {

            label 'packager';

            logRotator(365, 100, 10, 10);

            parameters {
                stringParam("DistributionsToPackage", distros_tobuild,
                    "The distributions to build packages for");
                stringParam("ArchesToPackage", arches_tobuild,
                    "The architectures to build packages for");
                choiceParam("PackageBuildKind",
                    ["Nightly", "Release"],
                    "What kind of build is this? A nightly build will have the prefix +nightly2014... appended, a release will just have the version number.");
                choiceParam("RepoSection",
                    ["llso-experimental", "llso-main", "pso-experimental", "pso-main"],
                    "For a release build where should the binary package go?");
            }

            triggers {
                // Check once every day for new changes
                // Times are UTC
                scm("H H(4-10) * * *");
            }

            wrappers {
                timestamps();
            }

            steps {
                def values = [ 'packagename' : packagename,
                    'supported_distros' : supported_distros,
                    'subdir_name' : subdir_name,
                    'package_version' : package_version,
                    'revision' : revision ];

                shell(Helper.prepareScript(build_script, values));
            }

            publishers {
                archiveArtifacts {
                    pattern("results/*, ${packagename}*");
                    allowEmpty(true);
                }

                configure { project ->
                    project / publishers << 'hudson.plugins.jira.JiraIssueUpdater' {
                    }
                }
            }
        }
    }

    static void hgScm(jobContext, project, branch, subdir_name) {
        jobContext.with {
            scm {
                hg(project, branch) { node ->
                    node / clean('true');
                    node / subdir(subdir_name);
                }
            }
        }
    }
}
