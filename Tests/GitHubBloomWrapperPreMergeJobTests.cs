// Copyright (c) 2015 SIL International
// This software is licensed under the MIT License (http://opensource.org/licenses/MIT)
using System;
using NUnit.Framework;
using OpenQA.Selenium;

namespace Tests
{
	[TestFixtureAttribute]
	public class GitHubBloomWrapperMasterPreMergeJobTests: GitHubBloomWrapperPreMergeJobTests
	{
		protected override string BranchName
		{
			get { return "master"; }
		}
	}

	[TestFixtureAttribute]
	public class GitHubBloomWrapperVersion30PreMergeJobTests: GitHubBloomWrapperPreMergeJobTests
	{
		protected override string BranchName
		{
			get { return "Version3.0"; }
		}
	}

	public abstract class GitHubBloomWrapperPreMergeJobTests
	{
		private Jenkins _jenkins;

		protected abstract string BranchName { get; }

		private string SanitizedBranchName
		{
			get { return BranchName.Replace('.', '_').Replace("__", "_"); }
		}

		[TestFixtureSetUp]
		public void FixtureSetUp()
		{
			_jenkins = Jenkins.Connect();
			_jenkins.Login();
			_jenkins.OpenPage(string.Format("GitHub-Bloom-Wrapper-{0}-debug", BranchName));
		}

		[TestFixtureTearDown]
		public void FixtureTearDown()
		{
			_jenkins.Dispose();
		}

		[Test]
		public void ParameterizedBuild()
		{
			Assert.That(_jenkins.IsChecked("This build is parameterized"), Is.True,
				"'This build is parameterized' is not checked");
			Assert.That(_jenkins.GetValueByXPath("String Parameter",
				"./tr/td[preceding-sibling::td[text()='Name']]/input"),
				Is.EqualTo("sha1"));
			Assert.That(_jenkins.GetValueByXPath("String Parameter",
				"./tr/td[preceding-sibling::td[text()='Default Value']]/input"),
				Is.Empty);
			Assert.That(_jenkins.GetTextByXPath("String Parameter",
				"./tr/td[preceding-sibling::td[text()='Description']]//div[@style='']/pre"),
				Is.EqualTo("What pull request to build, e.g. origin/pr/9/head"));
		}

		[Test]
		public void RestrictNode()
		{
			Assert.That(_jenkins.IsChecked("Restrict where this project can be run"), Is.True,
				"'Restrict where this project can be run' is not checked");
			Assert.That(_jenkins.GetValueByXPath(
				"//tr[preceding-sibling::tr/td/label[text()='Restrict where this project can be run']]/" +
				"td[preceding-sibling::td[text()='Label Expression']]/input"),
				Is.EqualTo("linux"));
		}

		[Test]
		public void SourceCodeManagement()
		{
			Assert.That(_jenkins.GetTextByXPath("//label[input[@name='scm' and @checked='true']]").Trim(' ', '\n', '\t'),
				Is.EqualTo("Git"));
			Assert.That(_jenkins.GetValue("Repository URL"),
				Is.EqualTo("https://github.com/BloomBooks/BloomDesktop.git"));
			Assert.That(_jenkins.GetValue("Branch Specifier (blank for 'any')"),
				Is.EqualTo("${sha1}"));
			Assert.That(_jenkins.GetTextByXPath("//td[preceding-sibling::td[text()='Repository browser']]" +
				"/select/option[@selected]"), Is.EqualTo("githubweb"));
			Assert.That(_jenkins.GetValueByXPath(
				"//tr[preceding-sibling::tr/td[text()='Repository browser']]//td[preceding-sibling::td[text()='URL']]/input"),
				Is.EqualTo("https://github.com/BloomBooks/BloomDesktop/"));
		}

		[Test]
		public void GitHubTrigger()
		{
			Assert.That(_jenkins.IsChecked(
				"GitHub Pull Request Builder"), Is.True,
				"'GitHub Pull Request Builder' not checked");
			// It seems that no matter what we set the setting from the config.xml file is
			// ignored in the UI.
//			Assert.That(_jenkins.IsCheckedByName(
//				"_.displayBuildErrorsOnDownstreamBuilds"), Is.True,
//				"'Display build errors on downstream builds' not checked");
			Assert.That(_jenkins.IsCheckedByName(
				"_.allowMembersOfWhitelistedOrgsAsAdmin"), Is.True,
				"'Allow members of whitelisted organisations as admins' not checked");
			Assert.That(_jenkins.GetValueByName("_.branch"),
				Is.EqualTo(BranchName));
			Assert.That(_jenkins.GetTextByName("_.adminlist"),
				Is.EqualTo("ermshiperete"));
			Assert.That(_jenkins.IsCheckedByName("_.useGitHubHooks"), Is.True,
				"'Use github hooks for build triggering' not checked");
			Assert.That(_jenkins.GetTextByName("_.whitelist"),
				Is.EqualTo("StephenMcConnel hatton phillip-hopper davidmoore1 gmartin7 JohnThomson"));
			Assert.That(_jenkins.GetTextByName("_.orgslist"),
				Is.EqualTo("BloomBooks"));
			Assert.That(_jenkins.GetValueByXPath("//td[preceding-sibling::td[text()='Crontab line']]/input"),
				Is.EqualTo("H/5 * * * *"));
		}

		[Test]
		public void BuildSteps_ExecuteShell()
		{
			Assert.That(_jenkins.GetTextByXPath(
				"//tr[preceding-sibling::tr/td/div/b[text()='Execute shell']]/td/textarea"),
				Is.EqualTo("echo -n ${BUILD_TAG} > ${WORKSPACE}/magic.txt"));
		}

		[Test]
		public void BuildSteps_TriggerOtherProjects()
		{
			var triggerBuildsTables = _jenkins.Driver.FindElementsByXPath(
				"//tbody[tr/td/div/b[text()='Trigger/call builds on other projects']]");
			Assert.That(triggerBuildsTables.Count, Is.EqualTo(2));
			Assert.That(triggerBuildsTables[0].FindElement(By.Name("_.projects")).GetAttribute("value"),
				Is.EqualTo(string.Format("GitHub-Bloom-Linux-any-{0}-debug,GitHub-Bloom-Win32-{0}-debug,GitHub-Bloom-Linux-any-{0}--JSTests", BranchName)));
			Assert.That(triggerBuildsTables[0].FindElement(By.Name("_.block")).Selected, Is.True,
				"'Block until the triggered projects finish their builds' is not checked");
			Assert.That(triggerBuildsTables[0].FindElement(
				By.XPath(".//td[preceding-sibling::td[text()='Fail this build step if the triggered build is worse or equal to']]" +
				"/select/option[@selected]")).Text, Is.EqualTo("FAILURE"));
			Assert.That(triggerBuildsTables[0].FindElement(
				By.XPath(".//td[preceding-sibling::td[text()='Mark this build as failure if the triggered build is worse or equal to']]" +
				"/select/option[@selected]")).Text, Is.EqualTo("FAILURE"));
			Assert.That(triggerBuildsTables[0].FindElement(
				By.XPath(".//td[preceding-sibling::td[text()='Mark this build as unstable if the triggered build is worse or equal to']]" +
				"/select/option[@selected]")).Text, Is.EqualTo("UNSTABLE"));
			Assert.That(triggerBuildsTables[0].FindElement(By.XPath("//b[text()='Current build parameters']")),
				Is.Not.Null);

			Assert.That(triggerBuildsTables[1].FindElement(By.Name("_.projects")).GetAttribute("value"),
				Is.EqualTo(string.Format("GitHub-Bloom-Linux-any-{0}-debug-Tests, GitHub-Bloom-Win32-{0}-debug-Tests", BranchName)));
			Assert.That(triggerBuildsTables[1].FindElement(By.Name("_.block")).Selected, Is.True,
				"'Block until the triggered projects finish their builds' is not checked");
			Assert.That(triggerBuildsTables[1].FindElement(
				By.XPath(".//td[preceding-sibling::td[text()='Fail this build step if the triggered build is worse or equal to']]" +
					"/select/option[@selected]")).Text, Is.EqualTo("FAILURE"));
			Assert.That(triggerBuildsTables[1].FindElement(
				By.XPath(".//td[preceding-sibling::td[text()='Mark this build as failure if the triggered build is worse or equal to']]" +
					"/select/option[@selected]")).Text, Is.EqualTo("FAILURE"));
			Assert.That(triggerBuildsTables[1].FindElement(
				By.XPath(".//td[preceding-sibling::td[text()='Mark this build as unstable if the triggered build is worse or equal to']]" +
					"/select/option[@selected]")).Text, Is.EqualTo("UNSTABLE"));
			Assert.That(triggerBuildsTables[1].FindElement(By.XPath("//b[text()='Current build parameters']")),
				Is.Not.Null);
			Assert.That(triggerBuildsTables[1].FindElement(By.Name("_.properties")).GetAttribute("textContent"),
				Is.EqualTo(string.Format("ARTIFACTS_TAG=\"jenkins-GitHub-Bloom-Win32-{0}-debug-" +
					"${{TRIGGERED_BUILD_NUMBERS_GitHub_Bloom_Win32_PR_debug}}\"\n" +
					"UPSTREAM_BUILD_TAG=${{BUILD_TAG}}", BranchName)));
		}

		[Test]
		public void BuildSteps_CopyArtifacts()
		{
			var copyArtifactsTables = _jenkins.Driver.FindElementsByXPath(
				"//tbody[tr/td/div/b[text()='Copy artifacts from another project']]");
			Assert.That(copyArtifactsTables.Count, Is.EqualTo(3));

			Assert.That(copyArtifactsTables[0].FindElement(By.Name("_.projectName")).GetAttribute("value"),
				Is.EqualTo(string.Format("GitHub-Bloom-Linux-any-{0}-debug-Tests", BranchName)));
			Assert.That(copyArtifactsTables[0].FindElement(By.XPath(
				"./tr/td[preceding-sibling::td[text()='Which build']]/select/option[@selected]")).GetAttribute("textContent"),
				Is.EqualTo("Specific build"));
			Assert.That(copyArtifactsTables[0].FindElement(By.Name("buildNumber")).GetAttribute("value"),
				Is.EqualTo(string.Format("${{TRIGGERED_BUILD_NUMBER_GitHub_Bloom_Linux_any_{0}_debug_Tests}}",
					SanitizedBranchName)));
			Assert.That(copyArtifactsTables[0].FindElement(By.Name("_.filter")).GetAttribute("value"),
				Is.EqualTo("output/Debug/BloomTests.dll.results.xml"));
			Assert.That(copyArtifactsTables[0].FindElement(By.Name("_.excludes")).GetAttribute("value"),
				Is.Empty);
			Assert.That(copyArtifactsTables[0].FindElement(By.Name("_.target")).GetAttribute("value"),
				Is.EqualTo(string.Format("GitHub-Bloom-Linux-any-{0}-debug-Tests-results/", BranchName)));
			Assert.That(copyArtifactsTables[0].FindElement(By.Name("_.parameters")).GetAttribute("value"),
				Is.Empty);
			Assert.That(copyArtifactsTables[0].FindElement(By.Name("_.flatten")).Selected,
				Is.True, "'Flatten directories' is not checked");
			Assert.That(copyArtifactsTables[0].FindElement(By.Name("_.optional")).Selected,
				Is.True, "'Optional' is not checked");
			Assert.That(copyArtifactsTables[0].FindElement(By.Name("_.fingerprintArtifacts")).Selected,
				Is.True, "'Fingerprint Artifacts' is not checked");

			Assert.That(copyArtifactsTables[1].FindElement(By.Name("_.projectName")).GetAttribute("value"),
				Is.EqualTo(string.Format("GitHub-Bloom-Win32-{0}-debug-Tests", BranchName)));
			Assert.That(copyArtifactsTables[1].FindElement(By.XPath(
				"./tr/td[preceding-sibling::td[text()='Which build']]/select/option[@selected]")).GetAttribute("textContent"),
				Is.EqualTo("Specific build"));
			Assert.That(copyArtifactsTables[1].FindElement(By.Name("buildNumber")).GetAttribute("value"),
				Is.EqualTo(string.Format("${{TRIGGERED_BUILD_NUMBER_GitHub_Bloom_Win32_{0}_debug_Tests}}",
					SanitizedBranchName)));
			Assert.That(copyArtifactsTables[1].FindElement(By.Name("_.filter")).GetAttribute("value"),
				Is.EqualTo("output/Debug/BloomTests.dll.results.xml"));
			Assert.That(copyArtifactsTables[1].FindElement(By.Name("_.excludes")).GetAttribute("value"),
				Is.Empty);
			Assert.That(copyArtifactsTables[1].FindElement(By.Name("_.target")).GetAttribute("value"),
				Is.EqualTo(string.Format("GitHub-Bloom-Win32-{0}-debug-Tests-results/", BranchName)));
			Assert.That(copyArtifactsTables[1].FindElement(By.Name("_.parameters")).GetAttribute("value"),
				Is.Empty);
			Assert.That(copyArtifactsTables[1].FindElement(By.Name("_.flatten")).Selected,
				Is.True, "'Flatten directories' is not checked");
			Assert.That(copyArtifactsTables[1].FindElement(By.Name("_.optional")).Selected,
				Is.True, "'Optional' is not checked");
			Assert.That(copyArtifactsTables[1].FindElement(By.Name("_.fingerprintArtifacts")).Selected,
				Is.True, "'Fingerprint Artifacts' is not checked");

			Assert.That(copyArtifactsTables[2].FindElement(By.Name("_.projectName")).GetAttribute("value"),
				Is.EqualTo(string.Format("GitHub-Bloom-Linux-any-{0}--JSTests", BranchName)));
			Assert.That(copyArtifactsTables[2].FindElement(By.XPath(
				"./tr/td[preceding-sibling::td[text()='Which build']]/select/option[@selected]")).GetAttribute("textContent"),
				Is.EqualTo("Specific build"));
			Assert.That(copyArtifactsTables[2].FindElement(By.Name("buildNumber")).GetAttribute("value"),
				Is.EqualTo(string.Format("${{TRIGGERED_BUILD_NUMBER_GitHub_Bloom_Linux_any_{0}_JSTests}}",
					SanitizedBranchName)));
			Assert.That(copyArtifactsTables[2].FindElement(By.Name("_.filter")).GetAttribute("value"),
				Is.EqualTo("src/BloomBrowserUI/test-results.xml"));
			Assert.That(copyArtifactsTables[2].FindElement(By.Name("_.excludes")).GetAttribute("value"),
				Is.Empty);
			Assert.That(copyArtifactsTables[2].FindElement(By.Name("_.target")).GetAttribute("value"),
				Is.EqualTo(string.Format("GitHub-Bloom-Linux-any-{0}--JSTests-results/", BranchName)));
			Assert.That(copyArtifactsTables[2].FindElement(By.Name("_.parameters")).GetAttribute("value"),
				Is.Empty);
			Assert.That(copyArtifactsTables[2].FindElement(By.Name("_.flatten")).Selected,
				Is.True, "'Flatten directories' is not checked");
			Assert.That(copyArtifactsTables[2].FindElement(By.Name("_.optional")).Selected,
				Is.True, "'Optional' is not checked");
			Assert.That(copyArtifactsTables[2].FindElement(By.Name("_.fingerprintArtifacts")).Selected,
				Is.True, "'Fingerprint Artifacts' is not checked");
		}

		[Test]
		public void PublishNUnitResults()
		{
			Assert.That(_jenkins.GetValueByXPath("Publish NUnit test result report",
				"./tr/td[preceding-sibling::td[text()='Test report XMLs']]/input"),
				Is.EqualTo("**/BloomTests.dll.results.xml"));
		}

		[Test]
		public void RecordFingerprint()
		{
			Assert.That(_jenkins.GetValueByXPath(
				"//td[preceding-sibling::td[text()='Files to fingerprint']]/input"),
				Is.EqualTo("magic.txt"));
		}

		[Test]
		public void PublishJUnitResults()
		{
			Assert.That(_jenkins.GetValueByXPath("Publish JUnit test result report",
				"./tr/td[preceding-sibling::td[text()='Test report XMLs']]/input"),
				Is.EqualTo(string.Format("GitHub-Bloom-Linux-any-{0}--JSTests-results/test-results.xml",
					BranchName)));
		}

		[Test]
		public void PublishBuild()
		{
			Assert.That(_jenkins.IsCheckedByName("bp.publishUnstableBuilds"), Is.True,
				"'Publish unstable builds' not checked");
			Assert.That(_jenkins.IsCheckedByName("bp.publishFailedBuilds"), Is.True,
				"'Publish failed builds' not checked");
		}
	}
}

