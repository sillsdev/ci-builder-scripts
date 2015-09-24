/*
 * DSL script for Jenkins Bloom Packaging jobs
 */
import utilities.common

// Variables
def packagename = 'Bloom';
def subdir_name = 'bloom-desktop';
def distros_tobuild = 'precise trusty';
def revision = "\$(echo \${GIT_COMMIT} | cut -b 1-6)";
def package_version = '--package-version "\${FULL_BUILD_NUMBER}" ';

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

freeStyleJob('Bloom_Packaging-Linux-all-3.0-release') {
    common.defaultPackagingJob(delegate, packagename, subdir_name, package_version, revision,
        distros_tobuild, 'Version3.0');

    description '''
<p>Nightly builds of the Version3.0 branch.</p>
<p>The job is created by the DSL plugin from <i>BloomPackagingJobs.groovy</i> script.</p>
''';

    triggers {
        githubPush();
    }

    common.gitScm(delegate, 'git://github.com/BloomBooks/BloomDesktop.git', "\$BranchOrTagToBuild",
        false, subdir_name, false, true);
}

def subdir_name_unstable = 'bloom-desktop-beta';

freeStyleJob('Bloom_Packaging-Linux-all-master-release') {
    common.defaultPackagingJob(delegate, packagename, subdir_name_unstable, package_version, revision,
        distros_tobuild, 'master');

    description '''
<p>Nightly builds of the Bloom master branch.</p>
<p>The job is created by the DSL plugin from <i>BloomPackagingJobs.groovy</i> script.</p>
''';

    triggers {
        githubPush();
    }

    common.gitScm(delegate, 'git://github.com/BloomBooks/BloomDesktop.git', "\$BranchOrTagToBuild",
        false, subdir_name_unstable, false, true);
}
