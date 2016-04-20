/*
 * DSL script for Jenkins Bloom Packaging jobs
 */
import utilities.common

// Variables
def packagename = 'Bloom'
def subdir_name = 'bloom-desktop'
def subdir_name_beta = 'bloom-desktop-beta'
def subdir_name_unstable = 'bloom-desktop-unstable'
def distros_tobuild = 'precise trusty xenial'
def repo = 'git://github.com/BloomBooks/BloomDesktop.git'
def email_recipients = 'eb1@sil.org stephen_mcconnel@sil.org'

def revision = "\$(echo \${GIT_COMMIT} | cut -b 1-6)"
def package_version = '--package-version "\${FULL_BUILD_NUMBER}" '

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

/*
 * we're really building beta packages at the moment.  Someday, we may want three jobs on three different branches...
 *freeStyleJob('Bloom_Packaging-Linux-all-3.6-release') {
 *    common.defaultPackagingJob(delegate, packagename, subdir_name, package_version, revision,
 *        distros_tobuild, email_recipients, 'Version3.6');
 */
freeStyleJob('Bloom_Packaging-Linux-all-3.6-release') {
    common.defaultPackagingJob(delegate, packagename, subdir_name_beta, package_version, revision,
        distros_tobuild, email_recipients, 'Version3.6')

    description '''
<p>Nightly builds of the Version3.6 branch.</p>
<p>The job is created by the DSL plugin from <i>BloomPackagingJobs.groovy</i> script.</p>
'''

    triggers {
        githubPush()
    }

    common.gitScm(delegate, repo, "\$BranchOrTagToBuild",
        false, subdir_name, false, true)
}

freeStyleJob('Bloom_Packaging-Linux-all-master-release') {
    common.defaultPackagingJob(delegate, packagename, subdir_name_unstable, package_version, revision,
        distros_tobuild, email_recipients, 'master')

    description '''
<p>Nightly builds of the Bloom master branch.</p>
<p>The job is created by the DSL plugin from <i>BloomPackagingJobs.groovy</i> script.</p>
'''

    triggers {
        githubPush()
    }

    common.gitScm(delegate, repo, "\$BranchOrTagToBuild",
        false, subdir_name_unstable, false, true)
}
