// Copyright (c) 2015 SIL International
// This software is licensed under the MIT License (http://opensource.org/licenses/MIT)
using NUnit.Framework;

namespace Tests
{
	[TestFixture]
	public class GitHubBloomWin32PreMergeJobTests
	{
		private Jenkins _jenkins;

		[TestFixtureSetUp]
		public void FixtureSetUp()
		{
			_jenkins = Jenkins.Connect();
			_jenkins.Login();
			_jenkins.OpenPage("GitHub-Bloom-Win32-master-debug");
		}

		[TestFixtureTearDown]
		public void FixtureTearDown()
		{
			_jenkins.Dispose();
		}

		[Test]
		public void DiscardOldBuilds()
		{
			Assert.That(_jenkins.IsChecked("Discard Old Builds"), Is.True,
				"'Discard Old Builds' not checked");
		}

		[Test]
		public void TimeOutStrategy()
		{
			Assert.That(_jenkins.IsChecked("Abort the build if it's stuck"), Is.True,
				"'Abort the build if it's stuck' not checked");

			Assert.That(_jenkins.GetTextByXPath(
				"//td[preceding-sibling::td[text()='Time-out strategy']]/select/option[@selected]"),
				Is.EqualTo("No Activity"));

			Assert.That(_jenkins.GetValueByName("_.timeoutSecondsString"), Is.EqualTo("180"));

			Assert.That(_jenkins.GetTextByXPath(
				"//td[preceding-sibling::td[text()='Time-out actions']]//div/b"),
				Is.EqualTo("Abort the build"));
		}

		[Test]
		public void MsBuildBuilder()
		{
			Assert.That(_jenkins.GetTextByXPath(
				"//select[@name='msBuildBuilder.msBuildName']/option[@selected]"),
				Is.EqualTo(".NET 4.0"));
			Assert.That(_jenkins.GetValueByName("msBuildBuilder.msBuildFile"),
				Is.EqualTo("Bloom VS2010.sln"));
		}
	}
}

