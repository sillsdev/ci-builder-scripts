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
                    project / 'publishers' << 'hudson.plugins.jira.JiraIssueUpdater' {
                    }
                    project / 'publishers' << 'hudson.plugins.build__publisher.BuildPublisher' {
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
                                node / 'extensions' / 'hudson.plugins.git.extensions.impl.SubmoduleOption' {
                                    disableSubmodules(disableSubmodules_);
                                    /* recursiveSubmodules(false);
                                    trackingSubmodules(false); */
                                }
                            }
                            if (scmName_ != "") {
                                node / 'extensions' / 'hudson.plugins.git.extensions.impl.ScmName' {
                                    name(scmName_);
                                }
                            }
                            if (commitAuthorInChangelog_) {
                                node / 'extensions' / 'hudson.plugins.git.extensions.impl.AuthorInChangelog';
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

    static void buildPublishers(jobContext, daysToKeepVal = 365, numToKeepVal = 20, artifactDaysToKeepVal = 10, artifactNumToKeepVal = 20) {
        jobContext.with {
            configure { project ->
                project / 'publishers' << 'hudson.plugins.build__publisher.BuildPublisher' {
                    publishUnstableBuilds(true);
                    publishFailedBuilds(true);
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

    static void addInstallKarmaBuildStep(stepContext) {
        stepContext.with {
            // NOTE: we create `node` as a symlink. Debian has renamed it to `nodejs` but karma etc
            // expects it as `node`.
            shell('''#!/bin/bash
if [ ! -f node_modules/.bin/karma ]; then
    sudo apt-get install -y npm
    mkdir -p ~/bin
    PATH="$HOME/bin:$PATH"
    if [ -f /usr/bin/nodejs ]; then
        ln -s /usr/bin/nodejs ~/bin/node
    fi
    npm install karma@~0.12 karma-cli@~0.0 karma-phantomjs-launcher phantomjs jasmine@~2.1 karma-jasmine@~0.3 karma-junit-reporter
fi
''');
        }
    }

    static void addGetDependenciesBuildStep(stepContext) {
        stepContext.with {
            shell('''
echo "Fetching dependencies"
cd build
./getDependencies-Linux.sh
''');
        }
    }

    static void addRunJsTestsBuildStep(stepContext, workDir) {
        // Remember: this is a dash, not a bash, script!
        def build_script = '''
echo "Running unit tests"
cd "@@{workDir}"
PATH="$HOME/bin:$PATH"
NODE_PATH=/usr/lib/nodejs:/usr/lib/node_modules:/usr/share/javascript
export NODE_PATH
../../node_modules/.bin/karma start karma.conf.js --browsers PhantomJS --reporters dots,junit --single-run
''';

        stepContext.with {
            // Run unit tests
            def values = [ 'workDir' : workDir ];

            shell(Helper.prepareScript(build_script, values));
        }
    }

    static void addMagicAggregationFile(stepContext) {
        stepContext.with {
            shell('''
# this is needed so that upstream aggregation of unit tests works
echo -n ${UPSTREAM_BUILD_TAG} > ${WORKSPACE}/magic.txt
''');
        }
    }
}
