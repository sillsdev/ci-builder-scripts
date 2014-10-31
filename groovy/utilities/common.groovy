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
    --debkeyid $DEBSIGNKEY
        '''

        /*
         * Definition of jobs
         */

        jobContext.with {

            label 'packager';

            logRotator(365, 20, 10, 10);

            parameters {
                stringParam("DistributionsToPackage", distros_tobuild,
                    "The distributions to build packages for");
                stringParam("ArchesToPackage", arches_tobuild,
                    "The architectures to build packages for");
                choiceParam("PackageBuildKind",
                    ["Nightly", "Release"],
                    "What kind of build is this? A nightly build will have the prefix +nightly2014... appended, a release will just have the version number.");
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
                    pattern("results/*, ${subdir_name}_*");
                    allowEmpty(true);
                }

                configure { project ->
                    project / publishers << 'hudson.plugins.jira.JiraIssueUpdater' {
                    }
                    project / publishers << 'hudson.plugins.build__publisher.BuildPublisher' {
                        publishUnstableBuilds(true);
                        publishFailedBuilds(true);
                        logRotator {
                            daysToKeep 365
                            numToKeep 20
                            artifactDaysToKeep 10
                            artifactNumToKeep 20
                        }
                    }
                }

                allowBrokenBuildClaiming();

                mailer("eb1@sil.org");
            }
        }
    }

    static void gitScm(jobContext, url_, branch_, createTag_ = false, subdir = "",
        disableSubmodules_ = false, commitAuthorInChangelog_ = false, scmName_ = "",
        refspec_ = "") {
        jobContext.with {
            scm {
                git {
                    remote {
                        url(url_);
                        if (refspec_ != "") {
                            refspec(refspec_);
                        }
                    }
                    branch(branch_);
                    createTag(createTag_);
                    if (subdir != "") {
                        relativeTargetDir(subdir);
                    }

                    if (disableSubmodules_ || scmName_ != "" | commitAuthorInChangelog_) {
                        configure { node ->
                            if (disableSubmodules_) {
                                node / extensions / 'hudson.plugins.git.extensions.impl.SubmoduleOption' {
                                    disableSubmodules(disableSubmodules_);
                                    /* recursiveSubmodules(false);
                                    trackingSubmodules(false); */
                                }
                            }
                            if (scmName_ != "") {
                                node / extensions / 'hudson.plugins.git.extensions.impl.ScmName' {
                                    name(scmName_);
                                }
                            }
                            if (commitAuthorInChangelog_) {
                                node / extensions / 'hudson.plugins.git.extensions.impl.AuthorInChangelog';
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
                    node / clean('true');
                    node / subdir(subdir_name);
                }
            }
        }
    }
}
